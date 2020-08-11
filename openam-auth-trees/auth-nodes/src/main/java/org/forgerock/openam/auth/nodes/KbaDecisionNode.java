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

import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getKbaConfig;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
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
import org.forgerock.openam.integration.idm.KbaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Checks whether the number of KBA questions defined for the object is equal to or greater than the required amount.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = KbaDecisionNode.Config.class,
        tags = {"identity management"})
public class KbaDecisionNode extends AbstractDecisionNode {
    private final Logger logger = LoggerFactory.getLogger(KbaDecisionNode.class);
    private final Realm realm;
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The attribute to query in the IDM object.
         *
         * @return the identity attribute of the IDM object
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Create the node.
     *
     * @param realm The realm context.
     * @param config the node configuration
     * @param idmIntegrationService Service stub for the IDM integration service.
     */
    @Inject
    public KbaDecisionNode(@Assisted Realm realm, @Assisted  Config config,
            IdmIntegrationService idmIntegrationService) {
        this.realm = realm;
        this.config = config;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("KbaDecisionNode started");

        logger.debug("Retrieving KBA configuration");
        KbaConfig kbaConfig = getKbaConfig(idmIntegrationService, realm, context.request.locales);

        if (kbaConfig.getMinimumAnswersToDefine() < 0 || kbaConfig.getKbaPropertyName() == null) {
            throw new NodeProcessException("Failed to retrieve configuration values");
        }

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()));
        identity.ifPresent(id -> logger.debug("Retrieving {} of {} {}", kbaConfig.getKbaPropertyName(),
                    context.identityResource, id));

        JsonValue existingObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity)
                .orElseThrow(() -> new NodeProcessException("Unable to read object"));
        JsonValue objectKba = existingObject.get(ptr(kbaConfig.getKbaPropertyName()));
        boolean result = objectKba != null
                && objectKba.isNotNull()
                && objectKba.size() >= kbaConfig.getMinimumAnswersToDefine();

        return goTo(result)
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
