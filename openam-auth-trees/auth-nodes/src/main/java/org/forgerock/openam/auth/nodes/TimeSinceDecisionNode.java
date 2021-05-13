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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.retrieveCreateDate;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationConfig.CREATE_DATE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.util.Strings.isNullOrEmpty;
import static org.forgerock.util.time.Duration.duration;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
import org.forgerock.openam.utils.Time;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.validators.DurationValidator;

/**
 * Checks if the specified time has passed.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = TimeSinceDecisionNode.Config.class,
        tags = {"identity management"})
public class TimeSinceDecisionNode extends AbstractDecisionNode {
    private final Logger logger = LoggerFactory.getLogger(TimeSinceDecisionNode.class);
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The elapsed time to compare.
         *
         * @return the elapsed time specified
         */
        @Attribute(order = 100, validators = {DurationValidator.class})
        String elapsedTime();

        /**
         * The attribute to query in the IDM object.
         *
         * @return the identity attribute of the IDM object
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Create the node.
     *
     * @param realm The realm context
     * @param config The node configuration
     * @param idmIntegrationService Service stub for the IDM integration service
     */
    @Inject
    public TimeSinceDecisionNode(@Assisted Realm realm, @Assisted Config config,
            IdmIntegrationService idmIntegrationService) {
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("TimeSinceDecisionNode started");
        JsonValue existingObject = getExistingObject(context);
        logger.debug("Retrieve {} of {}", CREATE_DATE, existingObject.get(FIELD_CONTENT_ID));
        JsonValue createDate = retrieveCreateDate(idmIntegrationService, realm, context.request.locales,
                context.identityResource, existingObject.get(FIELD_CONTENT_ID).asString());
        // return false if property being compared does not exist in the object
        if (!createDate.isDefined(CREATE_DATE) || isNullOrEmpty(createDate.get(CREATE_DATE).asString())) {
            logger.warn("Create date not defined");
            return goTo(false).replaceSharedState(context.sharedState.copy())
                    .replaceTransientState(context.transientState.copy()).build();
        }

        ZonedDateTime originalTime = ZonedDateTime.parse(createDate.get(CREATE_DATE).asString());
        ZonedDateTime currentTime = Time.zonedDateTime(ZoneOffset.UTC);
        Duration elapsed = duration(config.elapsedTime());
        return goTo(originalTime.plus(elapsed.convertTo(MILLISECONDS).getValue(), ChronoUnit.MILLIS)
                .compareTo(currentTime) <= 0)
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()).build();
    }

    private JsonValue getExistingObject(TreeContext context) throws NodeProcessException {
        String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));
        logger.debug("Retrieving {} {}", context.identityResource, identity);
        return getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), Optional.of(identity))
                .orElseThrow(() -> new NodeProcessException("Failed to retrieve existing object"));
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }

}
