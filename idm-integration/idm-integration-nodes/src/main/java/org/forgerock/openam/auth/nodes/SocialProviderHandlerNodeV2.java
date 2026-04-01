/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2023-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNodeV2.SocialAuthOutcomeV2.SOCIAL_AUTH_INTERRUPTED;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNodeV2.SocialAuthOutcomeV2.TIMEOUT_OUTCOME;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import jakarta.inject.Inject;

import org.apache.http.ConnectionClosedException;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.SocialIdPProfileTransformationScriptContext;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.openam.social.idp.SocialProviderHandlerNodeScriptContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;

/**
 * Redirects user to a social identity provider, handles post-auth, fetches and normalizes social userInfo and
 * determines whether this user has an existing AM account.
 */
@Node.Metadata(outcomeProvider = SocialProviderHandlerNodeV2.SocialAuthOutcomeProviderV2.class,
        configClass = SocialProviderHandlerNodeV2.Config.class, tags = {"social", "federation", "platform"})
public class SocialProviderHandlerNodeV2 extends SocialProviderHandlerNode {

    private static final String EXPECT_PROFILE_INFORMATION = "expectProfileInformation";
    private static final String BUNDLE = SocialProviderHandlerNodeV2.class.getName();
    private static final String TIMEOUT_OUTCOME_ID = "TIMEOUT_OUTCOME";
    private static final String DETECT_CONNECTION_TIMEOUT = "detectConnectionTimeOut";
    private final Logger logger = LoggerFactory.getLogger(SocialProviderHandlerNodeV2.class);
    private final SocialProviderHandlerNodeV2.Config config;

    /**
     * Constructor.
     *
     * @param config                 node configuration instance
     * @param authModuleHelper       helper for oauth2
     * @param providerConfigStore    service containing social provider configurations
     * @param identityService        an instance of the IdentityService
     * @param realm                  the realm context
     * @param scriptEvaluatorFactory factory for ScriptEvaluators
     * @param idmIntegrationService  service that provides connectivity to IDM
     * @param authScriptUtils        utilities for scripted nodes
     * @param transformationContext  the profile transformation script context
     * @param nodeContext            the node script context
     */
    @Inject
    public SocialProviderHandlerNodeV2(@Assisted Config config, SocialOAuth2Helper authModuleHelper,
            SocialIdentityProviders providerConfigStore, LegacyIdentityService identityService, @Assisted Realm realm,
            ScriptEvaluatorFactory scriptEvaluatorFactory, IdmIntegrationService idmIntegrationService,
            AuthScriptUtilities authScriptUtils, SocialIdPProfileTransformationScriptContext transformationContext,
            SocialProviderHandlerNodeScriptContext nodeContext) {
        super(config, authModuleHelper, providerConfigStore, identityService, realm, scriptEvaluatorFactory,
                idmIntegrationService, authScriptUtils, transformationContext, nodeContext);
        this.config = config;
    }

    @Override
    protected Action getAction(TreeContext context, String selectedIdp,
            OAuthClientConfig idpConfig, OAuthClient client, DataStore dataStore) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);
        Action action;
        try {
            action = handleCallback(context, selectedIdp, idpConfig, client, dataStore);
        } catch (Exception e) {
            if (isConnectionClosed(e)) {
                logger.warn("Connection closed during social login callback", e);
                if (config.detectConnectionTimeOut()) {
                    return goTo(TIMEOUT_OUTCOME.name()).build();
                } else {
                    throw e;
                }
            }
            throw new NodeProcessException("Unexpected error during social login callback", e);
        }
        if (action != null) {
            nodeState.remove(EXPECT_PROFILE_INFORMATION);
            return action;
        } else if (shouldExpectSocialProviderProfileInformation(nodeState)) {
            // If this block has been entered it means the user has navigated back to the SocialProviderHandler node
            // without having performed a social authentication (back button or refreshed URL). OPENAM-20924
            nodeState.remove(EXPECT_PROFILE_INFORMATION);
            return goTo(SOCIAL_AUTH_INTERRUPTED.name()).build();
        }
        return null;
    }

    private boolean isConnectionClosed(Throwable t) {
        while (t != null) {
            if (t instanceof ConnectionClosedException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private boolean shouldExpectSocialProviderProfileInformation(NodeState nodeState) {
        boolean expectProfileInformation;
        if (nodeState.isDefined(EXPECT_PROFILE_INFORMATION)) {
            expectProfileInformation = nodeState.get(EXPECT_PROFILE_INFORMATION).asBoolean();
        } else {
            nodeState.putShared(EXPECT_PROFILE_INFORMATION, true);
            expectProfileInformation = false;
        }
        return expectProfileInformation;
    }

    @Override
    public InputState[] getInputs() {
        InputState[] currentInputs = super.getInputs();
        InputState[] inputsWithNewState = Arrays.copyOf(currentInputs, currentInputs.length + 1);
        inputsWithNewState[currentInputs.length] = new InputState(EXPECT_PROFILE_INFORMATION, false);
        return inputsWithNewState;
    }

    /**
     * Defines the additional possible outcome from this node.
     */
    public enum SocialAuthOutcomeV2 {
        /**
         * Returned to this node without the necessary social auth parameters to continue.
         */
        SOCIAL_AUTH_INTERRUPTED,
        /**
         * Returned to this node after an identity provider client timeout.
         */
        TIMEOUT_OUTCOME
    }

    /**
     * Defines the possible outcomes from this node.
     */
    public static class SocialAuthOutcomeProviderV2 extends SocialAuthOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            return getAllOutcomes(locales)
                    .stream()
                    .filter(outcome -> {
                        if (TIMEOUT_OUTCOME_ID.equals(outcome.id)) {
                            return nodeAttributes.get(DETECT_CONNECTION_TIMEOUT).defaultTo(false).asBoolean();
                        }
                        return true;
                    }).toList();
        }

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) throws NodeProcessException {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnAuthenticationNode.OutcomeProvider.class.getClassLoader());
            return new ImmutableList.Builder<Outcome>()
                    .addAll(super.getAllOutcomes(locales))
                    .add(new Outcome(SOCIAL_AUTH_INTERRUPTED.name(), bundle.getString("socialAuthInterrupted")))
                    .add(new Outcome(TIMEOUT_OUTCOME.name(), bundle.getString("timeoutOutcome")))
                    .build();
        }
    }

    /**
     * Configuration holder for the node.
     */
    public interface Config extends SocialProviderHandlerNode.Config {

        /**
         * Whether to detect connection timeouts during the social authentication process.
         *
         * @return true if connection timeouts should be detected, false otherwise
         */
        @Attribute(order = 600)
        default boolean detectConnectionTimeOut() {
            return false;
        }
    }

}
