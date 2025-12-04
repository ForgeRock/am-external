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

import static java.util.stream.Collectors.toList;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getSchema;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.listSchemaProperties;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.updateTermsAccepted;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDENTITY_RESOURCE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.mapContextToObject;

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that creates an object in IDM from the OBJECT_ATTRIBUTES field within the shared state.
 *
 */
@Node.Metadata(outcomeProvider = CreateObjectNode.CreateObjectOutcomeProvider.class,
        configClass = CreateObjectNode.Config.class,
        tags = {"identity management"})
public class CreateObjectNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The identity resource for which is being created.
         *
         * @return the identity resource for creation
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String identityResource() {
            return DEFAULT_IDENTITY_RESOURCE;
        }
    }

    private static final String BUNDLE = CreateObjectNode.class.getName();

    private final CreateObjectNode.Config config;
    private final Logger logger = LoggerFactory.getLogger(CreateObjectNode.class);
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;


    /**
     * Constructs a new CreateObjectNode instance.
     * @param config Node configuration.
     * @param realm The realm context.
     * @param idmIntegrationService Service stub for the IDM integration service.
     */
    @Inject
    public CreateObjectNode(@Assisted CreateObjectNode.Config config,
            @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("CreateObjectNode started");

        if (!context.identityResource
                .equals(config.identityResource())) {
            throw new NodeProcessException("Configured identity resource for the node (" + config.identityResource()
                    + ") does not match the configured identity resource for the tree ("
                    + context.identityResource + ")");
        }

        JsonValue newObject = mapContextToObject(context);

        if (objectHasAllRequiredFields(context, newObject)) {
            try {
                String objectType = context.identityResource;
                logger.debug("Creating {}", objectType);
                JsonValue createdObject = idmIntegrationService.createObject(realm, context.request.locales,
                        objectType, newObject);

                if (context.sharedState.isDefined(TERMS_ACCEPTED)) {
                    logger.debug("Setting terms accepted version to {}",
                            context.sharedState.get(TERMS_ACCEPTED).get(TERMS_VERSION));
                    updateTermsAccepted(idmIntegrationService, realm, context.request.locales, objectType,
                            createdObject.get(FIELD_CONTENT_ID).asString(), context.sharedState.get(TERMS_ACCEPTED));
                }

                return goTo(CreateObjectOutcome.CREATED.name()).build();
            } catch (NodeProcessException e) {
                logger.warn("Failed to create object", e);
                return goTo(CreateObjectOutcome.FAILURE.name()).build();
            } catch (ResourceException e) {
                logger.warn("Failed to create object", e);
                return goTo(CreateObjectOutcome.FAILURE.name()).withErrorMessage(e.getMessage()).build();
            }
        }
        logger.debug("Creation object does not have all required fields.");
        return goTo(CreateObjectOutcome.FAILURE.name()).build();
    }

    private boolean objectHasAllRequiredFields(TreeContext context, JsonValue newObject) throws NodeProcessException {
        logger.debug("Retrieving {} schema", context.identityResource);
        JsonValue schema = getSchema(idmIntegrationService, realm, context.request.locales,
                context.identityResource);

        return newObject.keys().containsAll(schema.get("required").asList(String.class));
    }

    /**
     * The possible outcomes for the CreateObjectNode.
     */
    public enum CreateObjectOutcome {
        /**
         * Successful object creation.
         */
        CREATED,
        /**
         * Failure to create object.
         */
        FAILURE
    }

    /**
     * Defines the possible outcomes from this Create node.
     */
    public static class CreateObjectOutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(CreateObjectNode.BUNDLE,
                    CreateObjectNode.CreateObjectOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(CreateObjectOutcome.CREATED.name(), bundle.getString("createdOutcome")),
                    new Outcome(CreateObjectOutcome.FAILURE.name(), bundle.getString("failureOutcome")));
        }
    }

    @Override
    public InputState[] getInputs() {
        try {
            logger.debug("retrieving object's schema");
            JsonValue schema = getSchema(idmIntegrationService, realm, null, config.identityResource());
            List<InputState> inputs = listSchemaProperties(schema).stream()
                    .map(property -> new InputState(property, schema.get("required").asList().contains(property)))
                    .collect(toList());
            inputs.add(new InputState(TERMS_ACCEPTED, false));
            return inputs.toArray(new InputState[]{});
        } catch (NodeProcessException e) {
            logger.warn("Failed to retrieve object's schema", e);
            return new InputState[]{};
        }
    }

}
