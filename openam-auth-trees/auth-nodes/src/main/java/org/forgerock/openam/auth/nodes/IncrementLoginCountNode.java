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

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.incrementLoginCount;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that will increment an object's login count in IDM.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = IncrementLoginCountNode.Config.class,
        tags = {"behavioral"})
public class IncrementLoginCountNode extends SingleOutcomeNode {
    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final Logger logger = LoggerFactory.getLogger(IncrementLoginCountNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The attribute to query the IDM object by.
         *
         * @return the identity attribute of the IDM object
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Constructs a new {@link IncrementLoginCountNode} instance.
     *
     * @param config Node configuration.
     * @param realm The realm context.
     * @param idmIntegrationService Service stub for the IDM integration service.
     */
    @Inject
    public IncrementLoginCountNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("IncrementLoginCountNode started");
        try {
            Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                    config.identityAttribute()))
                    .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)));
            identity.ifPresent(id -> logger.debug("Retrieving {} {}", context.identityResource, id));

            JsonValue object = getObject(idmIntegrationService, realm, context.request.locales,
                    context.identityResource, config.identityAttribute(), identity)
                   .orElseThrow(() -> new NodeProcessException("No object to increment"));
            logger.debug("Incrementing login count");
            incrementLoginCount(idmIntegrationService, realm, context.request.locales, context.identityResource,
                    object.get(FIELD_CONTENT_ID).asString());
        } catch (NodeProcessException e) {
            logger.warn("Unable to increment login count", e);
        }
        return goToNext().build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
