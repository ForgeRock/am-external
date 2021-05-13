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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAcceptedTerms;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getActiveTerms;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.TermsAndConditionsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that evaluates if the currently active version of Terms and Conditions have been accepted.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = TermsAndConditionsDecisionNode.Config.class,
        tags = {"identity management"})
public class TermsAndConditionsDecisionNode extends AbstractDecisionNode {
    private static final String TERMS_ACCEPTED = "termsAccepted";
    private static final String TERMS_VERSION = "termsVersion";

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The attribute to query the IDM object by.
         *
         * @return IDM object identity attribute
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(TermsAndConditionsDecisionNode.class);
    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Constructs a new TermsAndConditionsDecisionNode instance.
     *
     * @param config node configuration
     * @param realm the realm context
     * @param idmIntegrationService service stub for the IDM integration service
     */
    @Inject
    public TermsAndConditionsDecisionNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("TermsAndConditionsDecisionNode started");
        logger.debug("Retrieving active terms configuration");
        final Optional<TermsAndConditionsConfig> activeTerms = getActiveTerms(idmIntegrationService, realm,
                context.request.locales);
        final String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));
        logger.debug("Retrieving accepted terms of {} {}", context.identityResource, identity);
        final JsonValue acceptedTerms = getAcceptedTerms(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity);

        return goTo(activeTerms
                    .map(active -> active.getVersion().equals(acceptedTerms.get(TERMS_ACCEPTED).get(TERMS_VERSION)
                            .asString()))
                    .orElse(true))
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy())
                .build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
