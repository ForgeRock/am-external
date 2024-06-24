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

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getConsentMappings;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.ConsentMappingCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node to collect consent to sharing profile data with resources defined by mappings in IDM.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ConsentNode.Config.class,
        tags = {"identity management"})
public class ConsentNode extends SingleOutcomeNode {
    private static final String CONSENTED_MAPPINGS = "consentedMappings";

    private final Logger logger = LoggerFactory.getLogger(ConsentNode.class);
    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final LocaleSelector localeSelector;

    /**
     * Node configuration.
     */
    public interface Config {

        /**
         * Whether all callbacks require consent.
         *
         * @return true iff all callbacks require consent from the end-user
         */
        @Attribute(order = 100)
        default Boolean allRequired() {
            return false;
        }

        /**
         * The message for the privacy and consent notice.
         *
         * @return the message
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default Map<Locale, String> message() {
            return Collections.emptyMap();
        }
    }

    /**
     * Guice constructor.
     *
     * @param config The node configuration.
     * @param realm The realm context.
     * @param idmIntegrationService Service stub for the IDM integration service.
     * @param localeSelector a LocaleSelector for choosing the correct message to display.
     */
    @Inject
    public ConsentNode(@Assisted Config config,
            @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService, LocaleSelector localeSelector) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.localeSelector = localeSelector;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ConsentNode started");
        // Start by returning all attribute callbacks
        if (context.getAllCallbacks().isEmpty()) {
            logger.debug("Collecting consent");
            List<ConsentMappingCallback> callbacks = generateCallbacks(context);
            if (callbacks.isEmpty()) {
                return goToNext().build();
            }
            return send(callbacks).build();
        }

        List<ConsentMappingCallback> callbacks = context.getCallbacks(ConsentMappingCallback.class);

        List<ConsentMappingCallback> expectedCallbacks = generateCallbacks(context);

        // Send callbacks if consent is required and any mappings came back unconsented or if any mappings are missing
        if ((config.allRequired() && callbacks.stream().anyMatch(callback -> !callback.getValue()))
                || callbacks.size() != expectedCallbacks.size()
                || callbacks.stream().anyMatch(callback -> expectedCallbacks.stream()
                            .noneMatch(expectedCallback -> callback.getName().equals(expectedCallback.getName())))) {
            logger.debug("Collecting missing consent");
            return send(expectedCallbacks).build();
        }

        // Update user consent data in shared state
        JsonValue sharedState = context.sharedState.copy();

        List<Map<String, Object>> consentedMappings = callbacks.stream()
                .filter(ConsentMappingCallback::getValue)
                .map(callback -> object(
                        field("consentDate", Time.zonedDateTime(ZoneOffset.UTC).toString()),
                        field("mapping", callback.getName())))
                .collect(toList());

        if (!consentedMappings.isEmpty()) {
            idmIntegrationService.storeAttributeInState(sharedState, CONSENTED_MAPPINGS, consentedMappings);
        }

        return goToNext()
                .replaceSharedState(sharedState)
                .build();
    }

    private List<ConsentMappingCallback> generateCallbacks(TreeContext context) throws NodeProcessException {
        // Fetch the list of configured mappings for external resources

        logger.debug("Retrieving consent mappings");
        JsonValue mappings = getConsentMappings(idmIntegrationService, realm, context.request.locales,
                context.identityResource);
        if (mappings == null || mappings.isNull()) {
            throw new NodeProcessException("Unable to fetch resources that require consent");
        }

        return mappings.stream()
                .map(mapping -> new ConsentMappingCallback(mapping, getLocalisedMessage(context, localeSelector,
                        this.getClass(), config.message(), "message.default"),
                        config.allRequired()))
                .collect(toList());
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(CONSENTED_MAPPINGS, singletonMap("*", false))
        };
    }
}