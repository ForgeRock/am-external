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
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.retrieveLoginCount;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.IDM_LOGIN_COUNT_ATTRIBUTE;

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
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Checks if the login count equals the 'at' or 'every' specified count.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = LoginCountDecisionNode.Config.class,
        tags = {"behavioral"})
public class LoginCountDecisionNode extends AbstractDecisionNode {

    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final Logger logger = LoggerFactory.getLogger(LoginCountDecisionNode.class);

    /**
     * The interval types allowed for checking login count.
     */
    public enum LoginCountIntervalType {
        /**
         * Represents that login count will be checked at the `every` interval.
         */
        EVERY,
        /**
         * Represents that the login count will be checked at the `at` interval.
         */
        AT
    }

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The interval to check the login count at.
         *
         * @return the interval type
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default LoginCountIntervalType interval() {
            return LoginCountIntervalType.AT;
        }

        /**
         * The amount of login counts to check.
         *
         * @return the login count amount
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class, GreaterThanZeroValidator.class})
        default int amount() {
            return 25;
        }

        /**
         * The attribute to query the IDM object by.
         *
         * @return the identity attribute of the IDM object
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Builds the node.
     *
     * @param config the node configuration
     * @param realm the realm context
     * @param idmIntegrationService Service stub for the IDM integration service
     */
    @Inject
    public LoginCountDecisionNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("LoginCountDecisionNode started");
        String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));
        logger.debug("Retrieving ID of {} {}", context.identityResource,
                identity);
        JsonValue existingObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), Optional.of(identity))
                .orElseThrow(() -> new NodeProcessException("Failed to retrieve existing object"));

        logger.debug("Retrieving {} of {}", IDM_LOGIN_COUNT_ATTRIBUTE, existingObject.get(FIELD_CONTENT_ID));
        JsonValue loginCount = retrieveLoginCount(idmIntegrationService, realm, context.request.locales,
                context.identityResource, existingObject.get(FIELD_CONTENT_ID).asString());

        boolean result = Optional.ofNullable(loginCount.get((ptr(IDM_LOGIN_COUNT_ATTRIBUTE))))
                .filter(JsonValue::isNotNull)
                .map(loginCountAttr -> {
                    int count = loginCountAttr.asInteger();
                    switch (config.interval()) {
                    case AT:
                        return count == config.amount();
                    case EVERY:
                        return count % config.amount() == 0;
                    default:
                        return false;
                    }
                })
                .orElse(false);

        return goTo(result).replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
