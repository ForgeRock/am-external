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
 * Copyright 2019-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.stream.Collectors.toList;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getSchema;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.listSchemaProperties;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.updateTermsAccepted;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDENTITY_RESOURCE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.mapContextToObject;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
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
 * A node that patches an object in IDM from the OBJECT_ATTRIBUTES field within the shared state.
 *
 */
@Node.Metadata(outcomeProvider = PatchObjectNode.PatchObjectOutcomeProvider.class,
        configClass = PatchObjectNode.Config.class,
        tags = {"identity management"})
public class PatchObjectNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * Whether to patch as the object. False represents defaulting to the oauth client.
         *
         * @return boolean to decide who to patch as
         */
        @Attribute(order = 100)
        default boolean patchAsObject() {
            return false;
        };

        /**
         * Fields to ignore in sharedState when performing patch. If empty, all fields will be attempted as
         * part of the patch.
         *
         * @return list of fields to ignore
         */
        @Attribute(order = 200)
        default Set<String> ignoredFields() {
            return new HashSet<>();
        };

        /**
         * The identityResource for which is being patched.
         *
         * @return the identityResource to patch.
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String identityResource() {
            return DEFAULT_IDENTITY_RESOURCE;
        }

        /**
         * The IDM attribute used to identify the target object in a query filter.
         *
         * @return the IDM identity attribute
         */
        @Attribute(order = 400, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }


    private static final String BUNDLE = PatchObjectNode.class.getName();

    private final PatchObjectNode.Config config;
    private final Logger logger = LoggerFactory.getLogger(PatchObjectNode.class);
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;


    /**
     * Constructs a new PatchObjectNode instance.
     * @param config Node configuration.
     * @param realm The realm context.
     * @param idmIntegrationService Service stub for the IDM integration service.
     */
    @Inject
    public PatchObjectNode(@Assisted PatchObjectNode.Config config,
            @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("PatchObjectNode started");

        if (!context.identityResource.equals(config.identityResource())) {
            throw new NodeProcessException("Configured identity resource for the node (" + config.identityResource()
                    + ") does not match the configured identity resource for the tree ("
                    + context.identityResource + ")");
        }

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)));

        if (identity.isEmpty()) {
            return goTo(PatchObjectOutcome.FAILURE.name()).build();
        }

        String objectType = context.identityResource;
        logger.debug("Retrieving {} {}", objectType, identity);
        JsonValue existingObject = getObject(idmIntegrationService, realm, context.request.locales, objectType,
                config.identityAttribute(), identity)
                .orElseThrow(() -> new NodeProcessException("Failed to retrieve existing object"));
        String objectId = existingObject.get(FIELD_CONTENT_ID).asString();

        try {
            logger.debug("Patching {}", objectId);
            JsonValue fieldsToPatch = mapContextToObject(context);
            for (String field : config.ignoredFields()) {
                JsonPointer fieldPointer = ptr(field);
                if (fieldsToPatch.get(fieldPointer) != null) {
                    fieldsToPatch.remove(fieldPointer);
                }
            }

            if (fieldsToPatch.size() == 0) {
                logger.debug("No fields to patch");
                // no need to patch - continue to next node
                return goTo(PatchObjectOutcome.PATCHED.name()).build();
            }

            if (config.patchAsObject()) {
                idmIntegrationService.patchObject(realm, context.request.locales, objectType, objectId,
                        fieldsToPatch.asMap(), objectId);
            } else {
                idmIntegrationService.patchObject(realm, context.request.locales, objectType, objectId,
                        fieldsToPatch.asMap(), null);
            }

            if (context.sharedState.isDefined(TERMS_ACCEPTED)) {
                logger.debug("Updating {} accepted terms version to {}", objectId,
                        context.sharedState.get(TERMS_ACCEPTED).get(TERMS_VERSION));
                updateTermsAccepted(idmIntegrationService, realm, context.request.locales, objectType,
                        objectId, context.sharedState.get(TERMS_ACCEPTED));
            }

            return goTo(PatchObjectOutcome.PATCHED.name()).build();
        } catch (NodeProcessException e) {
            logger.warn("Failed to patch object", e);
            return goTo(PatchObjectNode.PatchObjectOutcome.FAILURE.name()).build();
        } catch (ResourceException e) {
            logger.warn("Failed to create object", e);
            logger.warn("Failed to patch object", e);
            return goTo(PatchObjectNode.PatchObjectOutcome.FAILURE.name()).withErrorMessage(e.getMessage()).build();
        }
    }

    /**
     * The possible outcomes for the PatchObjectNode.
     */
    public enum PatchObjectOutcome {
        /**
         * Successful object patch.
         */
        PATCHED,
        /**
         * Failure to patch object.
         */
        FAILURE
    }

    /**
     * Defines the possible outcomes from this PatchObjectNode.
     */
    public static class PatchObjectOutcomeProvider implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(PatchObjectNode.BUNDLE,
                    PatchObjectNode.PatchObjectOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(PatchObjectNode.PatchObjectOutcome.PATCHED.name(), bundle.getString("patchedOutcome")),
                    new Outcome(PatchObjectNode.PatchObjectOutcome.FAILURE.name(), bundle.getString("failureOutcome")));
        }
    }

    @Override
    public InputState[] getInputs() {
        try {
            logger.debug("retrieving object's schema");
            List<InputState> inputs = listSchemaProperties(
                        getSchema(idmIntegrationService, realm, null,
                                config.identityResource())).stream()
                    .map(property -> new InputState(property, property.equals(config.identityAttribute())))
                    .collect(toList());
            inputs.add(new InputState(TERMS_ACCEPTED, false));
            return inputs.toArray(new InputState[]{});
        } catch (NodeProcessException e) {
            logger.warn("Failed to retrieve object's schema", e);
            return new InputState[]{};
        }
    }
}
