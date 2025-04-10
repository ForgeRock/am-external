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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.passthroughAuth;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_PASSWORD_ATTRIBUTE;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node that uses IDM's ICF authenticateOp to validate a username and password against a 3rd party system.
 */
@Node.Metadata(outcomeProvider = PassthroughAuthenticationNode.NodeOutcomeProvider.class,
        configClass = PassthroughAuthenticationNode.Config.class,
        tags = {"identity management"})
public class PassthroughAuthenticationNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The name of the system endpoint (connector) that should be used for passthrough authN.
         *
         * @return name of the system endpoint (connector)
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        String systemEndpoint();

        /**
         * The ICF object type of the object for which authentication will be tested.
         *
         * @return ICF object type of the target object
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String objectType() {
            return "account";
        }

        /**
         * The attribute used as the username for passthrough authN.
         *
         * @return the identity attribute
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }

        /**
         * The attribute used as the password for passthrough authN.
         *
         * @return the password attribute
         */
        @Attribute(order = 400, validators = {RequiredValueValidator.class})
        default String passwordAttribute() {
            return DEFAULT_PASSWORD_ATTRIBUTE;
        }
    }

    private static final String BUNDLE = PassthroughAuthenticationNode.class.getName();

    private final PassthroughAuthenticationNode.Config config;
    private final Logger logger = LoggerFactory.getLogger(PassthroughAuthenticationNode.class);
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Constructs a new PassthroughAuthenticationNode instance.
     *
     * @param config                Node configuration.
     * @param realm                 The realm context.
     * @param idmIntegrationService Service stub for the IDM integration service.
     */
    @Inject
    public PassthroughAuthenticationNode(@Assisted PassthroughAuthenticationNode.Config config,
            @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("PassthroughAuthenticationNode started");

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)));
        Optional<String> password = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.passwordAttribute()))
                .or(() -> stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                        DEFAULT_PASSWORD_ATTRIBUTE)));

        if (identity.isEmpty() || password.isEmpty() || password.get().isEmpty()) {
            return goTo(NodeOutcome.MISSING.name()).build();
        }

        boolean result = passthroughAuth(idmIntegrationService, realm, context.request.locales,
                config.systemEndpoint(), config.objectType(), identity.get(),
                password.get());

        return goTo(result
                ? NodeOutcome.AUTHENTICATED.name()
                : NodeOutcome.FAILED.name())
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()).build();
    }

    /**
     * The possible outcomes.
     */
    public enum NodeOutcome {
        /**
         * Successful authentication.
         */
        AUTHENTICATED,
        /**
         * Username or password missing from state.
         */
        MISSING,
        /**
         * Authntication failed.
         */
        FAILED
    }

    /**
     * Defines the possible outcomes from this PatchObjectNode.
     */
    public static class NodeOutcomeProvider implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(PassthroughAuthenticationNode.BUNDLE,
                    PatchObjectNode.PatchObjectOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(NodeOutcome.AUTHENTICATED.name(), bundle.getString("authenticatedOutcome")),
                    new Outcome(NodeOutcome.MISSING.name(), bundle.getString("missingOutcome")),
                    new Outcome(NodeOutcome.FAILED.name(), bundle.getString("failedOutcome")));
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute()),
            new InputState(config.passwordAttribute())
        };
    }
}
