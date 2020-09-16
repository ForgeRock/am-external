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
 * Copyright 2017-2019 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Arrays.asList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Immutable container for the result of processing a node.
 *
 * @supported.all.api
 * @since AM 5.5.0
 */
public final class Action {
    @Inject
    @Named("SystemSessionProperties")
    static List<String> blacklistedSessionProperties;

    /**
     * The output state of the node.
     */
    public final JsonValue sharedState;
    /**
     * The transient state of the node.
     */
    public final JsonValue transientState;
    /**
     * Result of the node. May be null.
     **/
    public final String outcome;
    /**
     * The error message to present to the caller when the FAILURE node is reached.
     */
    public final String errorMessage;
    /**
     * Callbacks requested by the node when the outcome is null. May be null.
     */
    public final List<Callback> callbacks;
    /**
     * Properties that will be included in the user's session if/when it is created.
     */
    public final Map<String, String> sessionProperties;
    /**
     * List of classes implementing {@link TreeHook} that run after successful login.
     */
    public final List<JsonValue> sessionHooks;
    /**
     * List of webhooks that run after logout.
     */
    public final List<String> webhooks;

    /**
     * Move on to the next node in the tree that is connected to the given outcome.
     *
     * @param outcome the outcome.
     * @return an action builder to provide additional details.
     */
    public static ActionBuilder goTo(String outcome) {
        Reject.ifNull(outcome);
        return new ActionBuilder(outcome);
    }

    /**
     * Send the given callbacks to the user for them to interact with.
     *
     * @param callbacks a non-empty list of callbacks.
     * @return an action builder to provide additional details.
     */
    public static ActionBuilder send(List<? extends Callback> callbacks) {
        Reject.ifNull(callbacks);
        Reject.ifTrue(callbacks.isEmpty(), "Callbacks must not be empty");
        return new ActionBuilder(callbacks);
    }

    /**
     * Send the given callbacks to the user for them to interact with.
     *
     * @param callbacks a non-empty list of callbacks.
     * @return an action builder to provide additional details.
     */
    public static ActionBuilder send(Callback... callbacks) {
        return send(asList(callbacks));
    }

    private Action(JsonValue sharedState, JsonValue transientState, String outcome, String errorMessage,
            List<? extends Callback> callbacks, Map<String, String> sessionProperties, List<JsonValue> sessionHooks,
            List<String> webhooks) {
        this.sharedState = sharedState;
        this.transientState = transientState;
        this.outcome = outcome;
        this.errorMessage = errorMessage;
        this.callbacks = Collections.unmodifiableList(callbacks);
        this.sessionProperties = Collections.unmodifiableMap(sessionProperties);
        this.sessionHooks = sessionHooks;
        this.webhooks = webhooks;
    }

    /**
     * Returns true if the action is a request for input.
     *
     * @return true if the action need to request for input, false otherwise.
     */
    public boolean sendingCallbacks() {
        return !callbacks.isEmpty();
    }

    /**
     * Builder for the {@link Action}.
     */
    public static final class ActionBuilder {
        private JsonValue sharedState;
        private JsonValue transientState;
        private String errorMessage;
        private final String outcome;
        private final List<? extends Callback> callbacks;
        private final Map<String, String> sessionProperties = new HashMap<>();
        private final List<JsonValue> sessionHooks = new ArrayList<>();
        private final List<String> webhooks = new ArrayList<>();

        private ActionBuilder(String outcome) {
            this.outcome = outcome;
            this.callbacks = Collections.emptyList();
        }

        private ActionBuilder(List<? extends Callback> callbacks) {
            this.outcome = null;
            this.callbacks = callbacks;
        }

        /**
         * Replace the shared state.
         *
         * @param sharedState the new state.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder replaceSharedState(JsonValue sharedState) {
            Reject.ifNull(sharedState);
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Replace the transient state.
         *
         * @param transientState the new transient state.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder replaceTransientState(JsonValue transientState) {
            Reject.ifNull(transientState);
            this.transientState = transientState;
            return this;
        }

        /**
         * Sets the error message to present to the caller when the FAILURE node is reached.
         *
         * <p>It is up to the caller to apply localisation.</p>
         *
         * @param errorMessage The error message.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Add a new property.
         *
         * @param key The key for the property.
         * @param value The value for the property.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder putSessionProperty(String key, String value) {
            Reject.ifNull(key, value);
            Reject.ifTrue(blacklistedSessionProperties != null && blacklistedSessionProperties.contains(key),
                    "Cannot set system session property " + key);
            sessionProperties.put(key, value);
            return this;
        }

        /**
         * Remove a previously set session property.
         *
         * @param key The key for the property.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder removeSessionProperty(String key) {
            Reject.ifNull(key);
            Reject.ifTrue(blacklistedSessionProperties != null && blacklistedSessionProperties.contains(key),
                    "Cannot remove system session property " + key);
            sessionProperties.put(key, null);
            return this;
        }

        /**
         * Sets a class that will be run after successful login.
         * The class has to implement {@link TreeHook}.
         *
         * @param sessionHook The class to be run.
         * @param nodeId The ID of the node.
         * @param nodeType The type of the node.
         * @return the same instance of ActionBuilder
         */
        public ActionBuilder addSessionHook(Class<? extends TreeHook> sessionHook, UUID nodeId, String nodeType) {
            Reject.ifNull(sessionHook);
            sessionHooks.add(
                    json(object(
                            field(TreeHook.SESSION_HOOK_CLASS_KEY, sessionHook.getName()),
                            field(TreeHook.NODE_ID_KEY, nodeId.toString()),
                            field(TreeHook.NODE_TYPE_KEY, nodeType))));
            return this;
        }

        /**
         * Adds session hooks to the list.
         *
         * @param sessionHooks List of the hooks to add.
         * @return the same instance of ActionBuilder.
         */
        public ActionBuilder addSessionHooks(List<JsonValue> sessionHooks) {
            Reject.ifNull(sessionHooks);
            this.sessionHooks.addAll(sessionHooks);
            return this;
        }

        /**
         * Adds a webhook to the list.
         *
         * @param webhookName The webhook name created on the webhook service.
         * @return the same instance of ActionBuilder.
         */
        public ActionBuilder addWebhook(String webhookName) {
            Reject.ifNull(webhookName);
            webhooks.add(webhookName);
            return this;
        }

        /**
         * Adds webhooks to the list.
         *
         * @param webhooks List of webhooks that run after logout.
         * @return the same instance of ActionBuilder.
         */
        public ActionBuilder addWebhooks(List<String> webhooks) {
            Reject.ifNull(webhooks);
            this.webhooks.addAll(webhooks);
            return this;
        }

        /**
         * Update the NodeType session property, and the NodeType sharedState value.
         * This method will copy the supplied TreeContext's shared state into this builder
         * if the builder does not already have a sharedState set.
         *
         * @param context The tree context.
         * @param authName Name of the auth module just passed
         * @return the same instance of ActionBuilder.
         */
        public ActionBuilder addNodeType(TreeContext context, String authName) {

            String authType = null;

            if (sharedState == null) {
                replaceSharedState(context.sharedState.copy());
            }

            if (sharedState.contains(NODE_TYPE)) {
                authType = sharedState.get(NODE_TYPE).asString();
            }

            StringBuilder newAuthType = new StringBuilder();

            if (authType != null) {
                newAuthType.append(authType);
            }

            newAuthType.append(authName);
            newAuthType.append("|");

            replaceSharedState(sharedState.put(NODE_TYPE, newAuthType.toString()));
            putSessionProperty(NODE_TYPE, newAuthType.toString());

            return this;
        }

        /**
         * Build the Action.
         *
         * @return an Action.
         * @throws IllegalStateException if the builder is called before building mandatory fields.
         */
        public Action build() {
            return new Action(this.sharedState, this.transientState, this.outcome, this.errorMessage, this.callbacks,
                    sessionProperties, sessionHooks, webhooks);
        }

    }
}
