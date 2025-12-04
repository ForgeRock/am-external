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
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getSchema;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDENTITY_RESOURCE;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that evaluates if all attributes required to create an object exist within context state.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = RequiredAttributesDecisionNode.Config.class,
        tags = {"identity management"})
public class RequiredAttributesDecisionNode extends AbstractDecisionNode {
    private final Logger logger = LoggerFactory.getLogger(RequiredAttributesDecisionNode.class);
    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The identity resource for schema lookup.
         *
         * @return the identity resource
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default String identityResource() {
            return DEFAULT_IDENTITY_RESOURCE;
        }
    }

    /**
     * Constructs a new RequiredAttributesDecisionNode instance.
     *
     * @param config node configuration
     * @param realm the realm context
     * @param idmIntegrationService service stub for the IDM integration service
     */
    @Inject
    public RequiredAttributesDecisionNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("RequiredAttributesDecisionNode started");

        if (!context.identityResource.equals(config.identityResource())) {
            throw new NodeProcessException("Configured object type for the node (" + config.identityResource()
                    + ") does not match the configured object type for the tree ("
                    + context.identityResource + ")");
        }

        logger.debug("Retrieving {} schema", context.identityResource);
        final JsonValue schema = getSchema(idmIntegrationService, realm, context.request.locales,
                context.identityResource);

        logger.debug("Searching for missing required attributes");
        final boolean missingRequiredAttributes = schema.get("required").stream()
                .noneMatch(attributeName -> {
                    Optional<JsonValue> attribute = getAttributeFromContext(idmIntegrationService, context,
                            attributeName.asString());
                    if (!attribute.isPresent() || attribute.get().isNull()) {
                        logger.debug("Missing {}", attributeName.asString());
                        return true;
                    } else {
                        return false;
                    }
                });

        return goTo(missingRequiredAttributes)
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy())
                .build();
    }

    @Override
    public InputState[] getInputs() {
        try {
            logger.debug("Retrieving {} schema", config.identityResource());
            return getSchema(idmIntegrationService, realm, null, config.identityResource()).get("required").stream()
                    .map(attr -> new InputState(attr.asString()))
                    .toArray(InputState[]::new);
        } catch (NodeProcessException e) {
            logger.warn("Failed to retrieve schema", e);
            return new InputState[]{};
        }
    }
}
