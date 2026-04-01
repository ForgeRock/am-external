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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.security.auth.callback.NameCallback;

import jakarta.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
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
        configClass = ValidatedUsernameNodeV2.Config.class,
        tags = {"identity management", "basic authentication", "basic authn"})
@Node.VersionMetadata(name = "ValidatedUsernameNode", version = 2)
public class ValidatedUsernameNodeV2 extends ValidatedUsernameNode {

    private final Logger logger = LoggerFactory.getLogger(ValidatedUsernameNodeV2.class);
    private final Config config;

    /**
     * Node configuration.
     */
    public interface Config extends ValidatedUsernameNode.Config {

        @Override
        @Attribute(order = 100)
        default Boolean validateInput() {
            return ValidatedUsernameNode.Config.super.validateInput();
        }

        @Override
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String usernameAttribute() {
            return ValidatedUsernameNode.Config.super.usernameAttribute();
        }

        /**
         * If true, will prepopulate the username field with a value from shared state.
         *
         * @return Whether to prepopulate the username input field with a value from shared state
         */
        @Attribute(order = 300)
        default boolean prepopulate() {
            return false;
        }

        @Override
        @Attribute(order = 400)
        default List<String> autocompleteValues() {
            return ValidatedUsernameNode.Config.super.autocompleteValues();
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
    public ValidatedUsernameNodeV2(@Assisted ValidatedUsernameNodeV2.Config config, @Assisted Realm realm,
                                   IdmIntegrationService idmIntegrationService) {
        super(config, realm, idmIntegrationService);
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ValidatedCreateUsernameNodeV2 started");
        usernameCallback = initialiseCallbacks(context);
        String username;
        var nodeState = context.getStateFor(this);
        if (config.validateInput()) {
            Optional<ValidatedUsernameCallback> validatedContextCallbackOptional =
                    context.getCallback(ValidatedUsernameCallback.class);
            ValidatedUsernameCallback validatedUsernameCallback = (ValidatedUsernameCallback) usernameCallback;
            if (validatedContextCallbackOptional.isEmpty()) {
                logger.debug("Collecting validated username");
                prepopulateIfApplicable(nodeState, validatedUsernameCallback::setUsername);
                return send(validatedUsernameCallback).build();
            }
            var validatedContextCallback = validatedContextCallbackOptional.get();
            username = validatedContextCallback.getUsername();
            Action callBackAction = getValidationCallbackAction(context, validatedContextCallback,
                    validatedUsernameCallback, username);
            if (callBackAction != null) {
                return callBackAction;
            }
        } else {
            Optional<NameCallback> contextCallback = context.getCallback(NameCallback.class);
            NameCallback nameCallback = (NameCallback) usernameCallback;
            if (contextCallback.isEmpty()) {
                logger.debug("Collecting username");
                prepopulateIfApplicable(nodeState, nameCallback::setName);
                return send(nameCallback).build();
            }

            username = contextCallback.get().getName();
        }

        if (!username.isEmpty()) {
            idmIntegrationService.storeAttributeInSharedState(nodeState, config.usernameAttribute(), username);
            // Also store the username in "the usual place" for interoperability with older nodes
            nodeState.putShared(USERNAME, username);
        }

        return goToNext().build();
    }

    private void prepopulateIfApplicable(NodeState nodeState, Consumer<String> setFunction) {
        if (config.prepopulate() && nodeState.isDefined(USERNAME)) {
            setFunction.accept(Objects.requireNonNull(nodeState.get(USERNAME)).asString());
        }
    }

    @Override
    protected String getBundle() {
        return ValidatedUsernameNodeV2.class.getName();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
                new InputState(USERNAME)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(config.usernameAttribute(), Collections.singletonMap("*", false)),
            new OutputState(USERNAME, Collections.singletonMap("*", false))
        };
    }
}
