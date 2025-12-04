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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getSchema;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getValidationRequirements;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.validateInput;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.PROPERTIES;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.ValidatedUsernameCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that prompts the user to create a username and validates that username against IDM policies.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ValidatedUsernameNode.Config.class,
        tags = {"identity management"})
public class ValidatedUsernameNode extends SingleOutcomeNode {

    private static final String BUNDLE = ValidatedUsernameNode.class.getName();

    private final Logger logger = LoggerFactory.getLogger(ValidatedUsernameNode.class);
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;

    private Callback usernameCallback;

    /**
     * Node configuration.
     */
    public interface Config {

        /**
         * Optionally validate input data.
         *
         * @return true iff input data should be validated
         */
        @Attribute(order = 100)
        default Boolean validateInput() {
            return false;
        }

        /**
         * The attribute in which this username will be stored.
         *
         * @return the attribute
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String usernameAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Constructor.
     *
     * @param config the config for the node
     * @param realm the realm for the tree
     * @param idmIntegrationService the IdmIntegrationService object
     */
    @Inject
    public ValidatedUsernameNode(@Assisted ValidatedUsernameNode.Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ValidatedCreateUsernameNode started");
        usernameCallback = initialiseCallbacks(context);

        String username;
        if (config.validateInput()) {
            Optional<ValidatedUsernameCallback> validatedContextCallback =
                    context.getCallback(ValidatedUsernameCallback.class);
            ValidatedUsernameCallback validatedUsernameCallback = (ValidatedUsernameCallback) usernameCallback;
            if (validatedContextCallback.isEmpty()) {
                logger.debug("Collecting username");
                return send(validatedUsernameCallback).build();
            }

            username = validatedContextCallback.get().getUsername();
            validatedUsernameCallback.validateOnly(validatedContextCallback.get().validateOnly());
            if (!checkUsername(context, username)) {
                logger.debug("Re-collecting invalid username");
                validatedUsernameCallback.setUsername(username);
                return send(validatedUsernameCallback).build();
            } else if (validatedContextCallback.get().validateOnly()) {
                logger.debug("Validation passed but validateOnly is true; Returning callbacks");
                return send(validatedContextCallback.get()).build();
            }
        } else {
            Optional<NameCallback> contextCallback = context.getCallback(NameCallback.class);
            NameCallback nameCallback = (NameCallback) usernameCallback;
            if (contextCallback.isEmpty()) {
                logger.debug("Collecting username");
                return send(nameCallback).build();
            }

            username = contextCallback.get().getName();
        }

        JsonValue sharedState = context.sharedState.copy();
        if (!username.isEmpty()) {
            idmIntegrationService.storeAttributeInState(sharedState, config.usernameAttribute(), username);
            // Also store the username in "the usual place" for interoperability with older nodes
            sharedState.put(USERNAME, username);
        }

        return goToNext()
                .replaceSharedState(sharedState)
                .replaceTransientState(context.transientState)
                .build();
    }

    private Callback initialiseCallbacks(TreeContext context) throws NodeProcessException {
        if (config.validateInput()) {
            String objectType = context.identityResource;
            JsonValue policies = json(object());
            logger.debug("Retrieving policy validation requirements for {} username", objectType);
            JsonValue requirements = getValidationRequirements(idmIntegrationService, realm, context.request.locales,
                    objectType);
            policies = requirements.get(PROPERTIES).stream()
                    .filter(requirement -> requirement.get("name").asString().equals(config.usernameAttribute()))
                    .findFirst()
                    .orElse(json(object()));
            logger.debug("Retrieving {} schema", objectType);
            JsonValue schema = getSchema(idmIntegrationService, realm, context.request.locales, objectType);
            JsonValue attribute = schema.get("properties").get(config.usernameAttribute());
            return new ValidatedUsernameCallback(attribute.isDefined("title")
                    ? attribute.get("title").asString()
                    : attribute.get("description").asString(),
                    policies, false);
        } else {
            ResourceBundle bundle =
                    context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
            return new NameCallback(bundle.getString("callback.username"));
        }
    }

    /**
     * Validates username input using IDM's policies.
     *
     * Example failed policy check:
     * <pre>
     *  {
     *      "result": false,
     *      "failedPolicyRequirements": [
     *          {
     *              "policyRequirements": [
     *                  {
     *                      "params": {
     *                          "minLength": 8
     *                      },
     *                      "policyRequirement": "MIN_LENGTH"
     *                  }
     *              ],
     *              "property": "username"
     *          }
     *      ]
     *  }
     * </pre>
     */
    private boolean checkUsername(TreeContext context, String username) throws NodeProcessException {
        logger.debug("Validating new username");
        JsonValue result = validateInput(idmIntegrationService, realm, context.request.locales,
                context.identityResource,
                IdmIntegrationService.mapContextToObject(context).put(config.usernameAttribute(), username),
                new HashSet<>(Collections.singletonList(config.usernameAttribute())));
        if (!result.isDefined("result")) {
            throw new NodeProcessException("Communication failure");
        }
        if (!result.get("result").asBoolean()) {
            ValidatedUsernameCallback callback = (ValidatedUsernameCallback) usernameCallback;
            result.get("failedPolicyRequirements").stream()
                    .filter(failure -> failure.get("property").asString().equals(config.usernameAttribute()))
                    .forEach(failure -> failure.get("policyRequirements")
                            .forEach(requirement -> {
                                logger.debug("Username failed policy: {}", requirement.toString());
                                callback.addFailedPolicy(requirement.toString());
                            }));
            return callback.failedPolicies().isEmpty();
        }
        return true;
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(config.usernameAttribute(), Collections.singletonMap("*", false)),
            new OutputState(USERNAME, Collections.singletonMap("*", false))
        };
    }
}
