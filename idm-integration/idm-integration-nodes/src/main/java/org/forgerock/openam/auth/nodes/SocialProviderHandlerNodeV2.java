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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNodeV2.SocialAuthOutcomeV2.SOCIAL_AUTH_INTERRUPTED;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Provider;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.service.SessionService;

/**
 * Redirects user to a social identity provider, handles post-auth, fetches and normalizes social userInfo and
 * determines whether this user has an existing AM account.
 */
@Node.Metadata(outcomeProvider = SocialProviderHandlerNodeV2.SocialAuthOutcomeProviderV2.class,
        configClass = SocialProviderHandlerNodeV2.Config.class, tags = {"social", "federation", "platform"})
public class SocialProviderHandlerNodeV2 extends SocialProviderHandlerNode {

    private static final String EXPECT_PROFILE_INFORMATION = "expectProfileInformation";
    private static final String BUNDLE = SocialProviderHandlerNodeV2.class.getName();

    /**
     * Constructor.
     *
     * @param config                 node configuration instance
     * @param authModuleHelper       helper for oauth2
     * @param providerConfigStore    service containing social provider configurations
     * @param identityService        an instance of the IdentityService
     * @param realm                  the realm context
     * @param scriptEvaluatorFactory factory for ScriptEvaluators
     * @param sessionServiceProvider provider of the session service
     * @param idmIntegrationService  service that provides connectivity to IDM
     */
    @Inject
    public SocialProviderHandlerNodeV2(@Assisted Config config, SocialOAuth2Helper authModuleHelper,
            SocialIdentityProviders providerConfigStore, LegacyIdentityService identityService, @Assisted Realm realm,
            ScriptEvaluatorFactory scriptEvaluatorFactory, Provider<SessionService> sessionServiceProvider,
            IdmIntegrationService idmIntegrationService) {
        super(config, authModuleHelper, providerConfigStore, identityService, realm, scriptEvaluatorFactory,
                sessionServiceProvider, idmIntegrationService);
    }

    @Override
    protected Action getAction(TreeContext context, String selectedIdp,
            OAuthClientConfig idpConfig, OAuthClient client, DataStore dataStore) throws NodeProcessException {
        var nodeState = context.getStateFor(this);
        Action action = handleCallback(context, selectedIdp, idpConfig, client, dataStore);
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
        SOCIAL_AUTH_INTERRUPTED
    }

    /**
     * Defines the possible outcomes from this node.
     */
    public static class SocialAuthOutcomeProviderV2 extends SocialAuthOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    SocialAuthOutcomeProviderV2.class.getClassLoader());
            return new ImmutableList.Builder<Outcome>()
                    .addAll(super.getOutcomes(locales))
                    .add(new Outcome(SOCIAL_AUTH_INTERRUPTED.name(),
                            bundle.getString("socialAuthInterrupted"))).build();
        }
    }

    /**
     * Configuration holder for the node.
     */
    public interface Config extends SocialProviderHandlerNode.Config {
    }

}
