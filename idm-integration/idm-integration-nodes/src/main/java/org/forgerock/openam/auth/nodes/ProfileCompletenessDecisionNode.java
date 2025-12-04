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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getProfileCompleteness;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.PercentageValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that evaluates a managed object's percentage of user-viewable and user-editable fields that contain values.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = ProfileCompletenessDecisionNode.Config.class,
        tags = {"identity management"})
public class ProfileCompletenessDecisionNode extends AbstractDecisionNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The percentage of non-empty, viewable, and editable fields required to return a true outcome.
         *
         * @return profile completeness threshold
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, PercentageValidator.class})
        Integer threshold();

        /**
         * The attribute to query the managed object by.
         *
         * @return managed object identity attribute
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(ProfileCompletenessDecisionNode.class);
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;

    /**
     * Constructs a new ProfileCompletenessDecisionNode instance.
     *
     * @param config node configuration
     * @param realm the realm context
     * @param idmIntegrationService service stub for the IDM integration service
     */
    @Inject
    public ProfileCompletenessDecisionNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ProfileCompletenessDecisionNode started");
        String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute())
                .or(() -> getUsernameFromContext(idmIntegrationService, context)))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));
        logger.debug("retrieving profile completeness score of {} {}", context.identityResource, identity);
        final float completeness = getProfileCompleteness(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity);

        return goTo(completeness > config.threshold())
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
