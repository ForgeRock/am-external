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

import static java.util.stream.Collectors.toList;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getSchema;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getValidationRequirements;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.validateInput;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.PROPERTIES;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.mapContextToObject;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.AbstractValidatedCallback;
import org.forgerock.openam.authentication.callbacks.AttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.BooleanAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.NumberAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.StringAttributeInputCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node to collect attribute values that will be saved in the shared state for later consumption. Uses IDM's
 * object schema to determine the data type of each attribute to be collected.  Optionally, if IDM is not
 * reachable/available the data type will default to String.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AttributeCollectorNode.Config.class,
        tags = {"identity management"})
public class AttributeCollectorNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger(AttributeCollectorNode.class);
    private static final Set<String> COLLECTIBLE_TYPES = asSet("string", "boolean", "number");

    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Node configuration.
     */
    public interface Config {
        /**
         * The attributes to be collected.
         *
         * @return a set of attributes to be collected
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        List<String> attributesToCollect();

        /**
         * Flag to determine if the attributes to collect should all be non-null.
         *
         * @return if the attributes must all have non-null values
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default Boolean required() {
            return false;
        }

        /**
         * Optionally validate input data.
         *
         * @return true iff attribute input data should be validated
         */
        @Attribute(order = 300)
        default Boolean validateInputs() {
            return false;
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

    /**
     * Guice constructor.
     *
     * @param config The node configuration.
     * @param realm The realm context.
     * @param idmIntegrationService Service stub for the IDM integration service.
     */
    @Inject
    public AttributeCollectorNode(@Assisted Config config,
            @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AttributeCollectorNode started");
        List<AttributeInputCallback> callbacks = generateCallbacks(context);

        // start by returning all attribute callbacks
        if (context.getAllCallbacks().isEmpty()) {
            logger.debug("Collecting attributes");
            return send(callbacks).build();
        }

        // try again if callbacks missing required data
        List<AttributeInputCallback> userCallbacks = context.getCallbacks(AttributeInputCallback.class);
        if (!checkRequiredAttributes(userCallbacks)) {
            logger.debug("Re-collecting missing required attributes");
            return send(callbacks).build();
        }

        // optionally validate inputs
        if (config.validateInputs()) {
            if (!validateInputs(context, callbacks, userCallbacks)) {
                logger.debug("Re-collecting invalid attributes");
                return send(callbacks).build();
            } else if (callbacks.stream().anyMatch(AbstractValidatedCallback::validateOnly)) {
                logger.debug("Validation succeeded but validateOnly is true; Returning callbacks");
                return send(callbacks).build();
            }
        }

        // store attribute data in shared state
        JsonValue sharedState = context.sharedState.copy();
        for (AttributeInputCallback callback : userCallbacks) {
            idmIntegrationService.storeAttributeInState(sharedState, callback.getName(), callback.getValue());
        }

        // clean up and exit
        return goToNext()
                .replaceTransientState(context.transientState.copy())
                .replaceSharedState(sharedState)
                .build();
    }

    private List<AttributeInputCallback> generateCallbacks(TreeContext context) throws NodeProcessException {
        // Fetch the idm schema for the configured object type
        logger.debug("Retrieving {} schema", context.identityResource);
        JsonValue schema = getSchema(idmIntegrationService, realm, context.request.locales, context.identityResource);
        if (schema == null || schema.isNull()) {
            throw new NodeProcessException("Configured object type has no schema");
        }

        JsonValue policies = json(object());
        if (config.validateInputs()) {
            logger.debug("Retrieving policy validation requirements for {} username", context.identityResource);
            policies = getValidationRequirements(idmIntegrationService, realm, context.request.locales,
                    context.identityResource);
        }

        // identify all user-editable attributes from the schema
        List<AttributeBean> validAttributes = findValidAttributes(schema);

        // throw error if inputs are to be validated and config contains an attribute not in the user-editable list
        if (config.validateInputs() && config.attributesToCollect().stream()
                .anyMatch(attr -> validAttributes.stream()
                        .noneMatch(fields -> fields.getAttribute().equals(attr)))) {
            throw new NodeProcessException("Configuration contains an invalid attribute");
        }

        // build and return callbacks for all editable attributes listed in the config -- prepopulate with existing
        // values from fetched user, if any
        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()));
        identity.ifPresent(id -> logger.debug("Retrieving existing attributes for {}", id));

        JsonValue userObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity)
                .orElse(json(object()));

        List<AttributeInputCallback> userCallbacks = context.getCallbacks(AttributeInputCallback.class);
        List<AttributeInputCallback> callbacks = new LinkedList<>();
        for (String attr : config.attributesToCollect()) {
            Optional<AttributeBean> attributeInfo = validAttributes.stream()
                    .filter(fields -> fields.getAttribute().equals(attr)).findFirst();
            if (!attributeInfo.get().getType().isEmpty() && attributeInfo.get().getType() != null) {
                switch (attributeInfo.get().getType()) {
                case "string":
                    callbacks.add(new StringAttributeInputCallback.StringAttributeInputCallbackBuilder()
                            .withName(attr)
                            .withPrompt(attributeInfo.get().getTitle())
                            .withValue((String) findValue(context, attr,
                                    json(userObject.get(ptr(attr))).defaultTo(null).asString(), userCallbacks))
                            .withRequired(config.required() && !attributeInfo.get().isNullable())
                            .withValidateOnly(findValidateOnly(attr, userCallbacks))
                            .withPolicies(findPoliciesForAttribute(policies, attr))
                            .build());
                    break;
                case "number":
                    callbacks.add(new NumberAttributeInputCallback.NumberAttributeInputCallbackBuilder()
                            .withName(attr)
                            .withPrompt(attributeInfo.get().getTitle())
                            .withValue((Double) findValue(context, attr,
                                    json(userObject.get(ptr(attr))).defaultTo(null).asDouble(), userCallbacks))
                            .withRequired(config.required() && !attributeInfo.get().isNullable())
                            .withValidateOnly(findValidateOnly(attr, userCallbacks))
                            .withPolicies(findPoliciesForAttribute(policies, attr))
                            .build());
                    break;
                case "boolean":
                    callbacks.add(new BooleanAttributeInputCallback.BooleanAttributeInputCallbackBuilder()
                            .withName(attr)
                            .withPrompt(attributeInfo.get().getTitle())
                            .withValue((Boolean) findValue(context, attr,
                                    json(userObject.get(ptr(attr))).defaultTo(null).asBoolean(), userCallbacks))
                            .withRequired(config.required() && !attributeInfo.get().isNullable())
                            .withValidateOnly(findValidateOnly(attr, userCallbacks))
                            .withPolicies(findPoliciesForAttribute(policies, attr))
                            .build());
                    break;
                default:
                    throw new NodeProcessException("Unhandleable data type: " + attributeInfo.get().getType());
                }
            }
        }
        return callbacks;
    }

    /**
     * Build a list of the policies enforced on a given attribute only if input validation is enabled.
     *
     * @param policies the set of policy data related to this object type
     * @param attribute the attribute for which policy enforcement information is requested
     * @return the list of enforced policies for the given attribute
     */
    private JsonValue findPoliciesForAttribute(JsonValue policies, String attribute) {
        return policies.get(PROPERTIES).defaultTo(json(array())).stream()
                .filter(requirement -> requirement.get("name").asString().equals(attribute))
                .findFirst()
                .orElse(json(object()));
    }

    /**
     * Select an attribute value to prepopulate in a returned callback.
     *
     * @param context the TreeContext
     * @param name the name of the attribute
     * @param objectValue the value for the attribute stored in IDM
     * @param userCallbacks the user callbacks which may contain a value for the attribute
     * @return the selected value
     */
    private Object findValue(TreeContext context, String name, Object objectValue,
            List<AttributeInputCallback> userCallbacks) {
        // Give priority to new value provided by user as found in the user callback, then to the sharedState, then
        // the existing user object
        Optional<AttributeInputCallback> userValue = userCallbacks.stream()
                .filter(callback -> callback.getName().equals(name) && callback.getValue() != null)
                .findFirst();
        if (userValue.isPresent()) {
            return userValue.get().getValue();
        }
        Optional<JsonValue> contextValue = idmIntegrationService.getAttributeFromContext(context, name);
        if (contextValue.isPresent()) {
            return contextValue.get().getObject();
        }
        return objectValue;
    }

    /**
     * Select the state of validateOnly to prepopulate in a returned callback.
     *
     * @param name the name of the attribute
     * @param userCallbacks the user callbacks which may contain a value for validateOnly
     * @return the selected validateOnly state or false if none was found
     */
    private boolean findValidateOnly(String name, List<AttributeInputCallback> userCallbacks) {
        Optional<AttributeInputCallback> foundCallback = userCallbacks.stream()
                .filter(callback -> callback.getName().equals(name))
                .findFirst();
        return foundCallback.isPresent()
                ? foundCallback.get().validateOnly()
                : false;
    }

    /**
     * Build a list of attributes valid for collection from the user.  A valid attribute is one marked as both viewable
     * and user-editable in the IDM schema.  If validation is disabled then all viewable attributes will be returned
     * without regard to the user-editable flag as they won't be edited.
     *
     * @param schema the IDM schema used to identify valida attributes
     * @return the list of valid attributes
     */
    private List<AttributeBean> findValidAttributes(JsonValue schema) {
        return schema.get("properties").stream()
                .filter(property -> property.get("viewable").asBoolean()
                        && (!config.validateInputs() || property.get("userEditable").asBoolean()))
                .map(this::findValues)
                .flatMap(List::stream)
                .collect(toList());
    }

    /**
     * Finds the associated information of a valid schema property.
     *
     * @param property the schema property
     * @return the List of AttributeBeans
     */
    private List<AttributeBean> findValues(JsonValue property) {
        List<AttributeBean> beanList = new ArrayList<>();
        JsonValue propertyTypeValue = property.get("type").defaultTo("none");
        String propertyType = propertyTypeValue.isList()
                ? propertyTypeValue.asList(String.class).stream()
                        .filter(type -> !type.equals("null"))
                        .findFirst()
                        .orElse("none")
                : propertyTypeValue.asString();

        if (COLLECTIBLE_TYPES.contains(propertyType)) {

            String propertyName = property.getPointer().leaf();
            if (property.getPointer().size() > 2) {
                propertyName = IdmIntegrationService.normalizeAttributeName(property.getPointer());
            }

            // if title not available, should take description field, otherwise default to field name
            String title = property.get("title").defaultTo(null).asString();
            String description = property.get("description").defaultTo(null).asString();
            String prompt = title != null ? title : description != null ? description : propertyName;
            beanList.add(new AttributeBean(propertyName, prompt, propertyType, propertyTypeValue.isList()
                    && propertyTypeValue.asList(String.class).contains("null")));
        }

        if (propertyType.equals("object")) {
            for (JsonValue prop : property.get("properties").defaultTo(json(object()))) {
                beanList.addAll(findValues(prop));
            }
        }

        return beanList;
    }

    /**
     * Test whether all attributes are required and, if so, whether all attributes collected are non-null.
     *
     * @param callbacks the callbacks to test
     * @return true if not required or all attributes have values
     */
    private boolean checkRequiredAttributes(List<AttributeInputCallback> callbacks) {
        return !config.required() || callbacks.stream().noneMatch(callback -> {
            if (callback.getValue() == null) {
                logger.debug("Required attribute {} is missing", callback.getName());
                return true;
            } else {
                return false;
            }
        });
    }

    /**
     * Validate the inputs as a group and update the returnable callbacks with the user's values and any failed
     * policy tests.
     *
     * @param callbacks the returnable callbacks
     * @param userCallbacks the user's input callbacks
     * @return true iff all inputs pass validation
     */
    private boolean validateInputs(TreeContext context, List<AttributeInputCallback> callbacks,
            List<AttributeInputCallback> userCallbacks) throws NodeProcessException {
        if (!config.validateInputs()) {
            return true;
        }

        // copy input values into returned callbacks
        callbacks.forEach(callback -> userCallbacks.stream()
                .filter(userCallback -> userCallback.getName().equals(callback.getName()))
                .findFirst()
                .map(userCallback -> setCallbackValue(callback, userCallback.getValue())));

        // validate the inputs
        JsonValue inputValues = json(object());
        userCallbacks.forEach(callback -> inputValues.put(callback.getName(), callback.getValue()));
        JsonValue attributes = mapContextToObject(inputValues, json(object()));
        logger.debug("Validating attributes");
        JsonValue result = validateInput(idmIntegrationService, realm, context.request.locales,
                context.identityResource, attributes, attributes.keys());
        if (!result.isDefined("result")) {
            throw new NodeProcessException("Communication failure");
        }

        // add failure data to returned callbacks
        if (!result.get("result").asBoolean()) {
            List<JsonValue> unmatchedFailures = result.get("failedPolicyRequirements").stream()
                    .filter(failure -> callbacks.stream()
                            .filter(callback -> callback.getName().equals(failure.get("property").asString()))
                            .collect(Collectors.toList())
                            .isEmpty())
                    .collect(Collectors.toList());
            if (!unmatchedFailures.isEmpty()) {
                throw new NodeProcessException("No callback match for failed policy! Policy failed on "
                        + json(unmatchedFailures).toString());
            }

            for (Object failure : result.get("failedPolicyRequirements").asList()) {
                for (AttributeInputCallback callback : callbacks) {
                    if (callback.getName().equals(json(failure).get("property").asString())) {
                        for (Object requirement : json(failure).get("policyRequirements").asList()) {
                            String failedPolicy = json(requirement).toString();
                            logger.debug("Attribute {} failed validation: {}", callback.getName(), failedPolicy);
                            callback.addFailedPolicy(failedPolicy);
                        }
                    }
                    userCallbacks.forEach(userCallback -> {
                        if (callback.getName().equals(userCallback.getName())) {
                            callback.validateOnly(userCallback.validateOnly());
                        }
                    });
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Set a returnable callback's value.
     *
     * @param callback the returnable callback
     * @param value the value to set in the callback
     * @return the callback
     */
    private AttributeInputCallback setCallbackValue(AttributeInputCallback callback, Object value) {
        if (callback instanceof BooleanAttributeInputCallback) {
            ((BooleanAttributeInputCallback) callback).setValue((Boolean) value);
        } else if (callback instanceof StringAttributeInputCallback) {
            ((StringAttributeInputCallback) callback).setValue((String) value);
        } else if (callback instanceof NumberAttributeInputCallback) {
            ((NumberAttributeInputCallback) callback).setValue((Double) value);
        }
        return callback;
    }

    private static class AttributeBean {
        private final String attribute;
        private final String title;
        private final String type;
        private final boolean nullable;

        AttributeBean(String attribute, String title, String type, boolean nullable) {
            this.attribute = attribute;
            this.title = title;
            this.type = type;
            this.nullable = nullable;
        }

        public String getAttribute() {
            return attribute;
        }

        public String getTitle() {
            return title;
        }

        public String getType() {
            return type;
        }

        public boolean isNullable() {
            return nullable;
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute(), false)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return config.attributesToCollect().stream()
                .map(attributeName -> new OutputState(attributeName, Collections.singletonMap("*", config.required())))
                .toArray(OutputState[]::new);
    }
}
