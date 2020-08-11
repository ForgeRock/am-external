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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getSchema;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getValidationRequirements;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.validateInput;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_PASSWORD_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.PROPERTIES;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.mapContextToObject;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.ValidatedPasswordCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that prompt the user to create a password.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ValidatedPasswordNode.Config.class,
        tags = {"identity management"})
public class ValidatedPasswordNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger(ValidatedPasswordNode.class);
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;

    private List<ValidatedPasswordCallback> passwordCallbacks;

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
         * The attribute in which this password will be stored.
         *
         * @return the attribute
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String passwordAttribute() {
            return DEFAULT_IDM_PASSWORD_ATTRIBUTE;
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
    public ValidatedPasswordNode(@Assisted ValidatedPasswordNode.Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ValidatedCreatePasswordNode started");
        passwordCallbacks = initialiseCallbacks(context);

        List<ValidatedPasswordCallback> callbacks = context.getCallbacks(ValidatedPasswordCallback.class);
        if (isEmpty(callbacks) || callbacks.size() != 1) {
            logger.debug("Collecting password");
            return send(passwordCallbacks).build();
        }

        String password = new String(callbacks.get(0).getPassword());
        if (config.validateInput()) {
            passwordCallbacks.get(0).validateOnly(callbacks.get(0).validateOnly());
            if (!checkPassword(context, password)) {
                logger.debug("Re-collecting invalid password");
                return send(passwordCallbacks).build();
            } else if (callbacks.get(0).validateOnly()) {
                logger.debug("Validation passed but validateOnly is true; Returning callbacks");
                return send(callbacks).build();
            }
        }

        JsonValue transientState = context.transientState.copy();
        idmIntegrationService.storeAttributeInState(transientState, config.passwordAttribute(), password);
        // Also store the password in "the usual place" for interoperability with older nodes
        transientState.put(PASSWORD, password);
        return goToNext()
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(transientState)
                .build();
    }

    private List<ValidatedPasswordCallback> initialiseCallbacks(TreeContext context) throws NodeProcessException {
        List<ValidatedPasswordCallback> callbacks = new ArrayList<>();

        String objectType = context.identityResource;
        JsonValue policies = json(object());
        if (config.validateInput()) {
            logger.debug("Retrieving policy validation requirements for {} password", objectType);
            JsonValue requirements = getValidationRequirements(idmIntegrationService, realm, context.request.locales,
                    objectType);
            policies = requirements.get(PROPERTIES).stream()
                    .filter(requirement -> requirement.get("name").asString().equals(config.passwordAttribute()))
                    .findFirst()
                    .orElse(json(object()));
        }

        logger.debug("Retrieving {} schema", objectType);
        JsonValue schema = getSchema(idmIntegrationService, realm, context.request.locales, objectType);
        JsonValue attribute = schema.get("properties").get(config.passwordAttribute());
        callbacks.add(new ValidatedPasswordCallback(attribute.isDefined("title")
                ? attribute.get("title").asString()
                : attribute.get("description").asString(),
                false, policies, false));
        return callbacks;
    }

    /**
     * Validates password input using IDM's policies.
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
     *              "property": "password"
     *          }
     *      ]
     *  }
     * </pre>
     */
    private boolean checkPassword(TreeContext context, String password) throws NodeProcessException {
        logger.debug("Validating new password");
        JsonValue result = validateInput(idmIntegrationService, realm, context.request.locales,
                context.identityResource, mapContextToObject(context).put(config.passwordAttribute(), password),
                new HashSet<>(Collections.singletonList(config.passwordAttribute())));
        if (!result.isDefined("result")) {
            throw new NodeProcessException("Communication failure");
        }
        if (!result.get("result").asBoolean()) {
            ValidatedPasswordCallback callback = passwordCallbacks.get(0);
            result.get("failedPolicyRequirements").stream()
                    .filter(failure -> failure.get("property").asString().equals(config.passwordAttribute()))
                    .forEach(failure -> failure.get("policyRequirements")
                        .forEach(requirement -> {
                            logger.debug("Password failed policy: {}", requirement.toString());
                            callback.addFailedPolicy(requirement.toString());
                        }));
            return callback.failedPolicies().isEmpty();
        }
        return true;
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(config.passwordAttribute()),
            new OutputState(PASSWORD)
        };
    }
}
