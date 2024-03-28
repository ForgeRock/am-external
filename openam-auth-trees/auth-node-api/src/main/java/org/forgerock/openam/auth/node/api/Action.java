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
 * Copyright 2017-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Arrays.asList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.fieldIfNotNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.util.Reject;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;

/**
 * Immutable container for the result of processing a node.
 *
 */
@SupportedAll
public final class Action {
    @Inject
    @Named("SystemSessionProperties")
    static List<String> blacklistedSessionProperties;

    /** Key for the header return property. */
    public static final String HEADER = "header";
    /** Key for the description return property. */
    public static final String DESCRIPTION = "description";
    /** Key for the stage return property. */
    public static final String STAGE = "stage";

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
     * Properties to return to the client.
     */
    public final Map<String, Object> returnProperties;
    /**
     * The error message to present to the caller when the FAILURE node is reached.
     */
    public final String errorMessage;
    /**
     * The error message to present to the caller when the user is locked out.
     */
    public final String lockoutMessage;
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
     * The {@link SuspensionHandler} to call when the authentication process is suspended by the authentication
     * framework.
     */
    public final SuspensionHandler suspensionHandler;
    /**
     * The universal id of the identity object.
     * @deprecated use {@link #identifiedIdentity} instead.
     */
    @Deprecated
    public final Optional<String> universalId;
    /**
     * Optionally the identity confirmed to exist as part of this action.
     */
    public final Optional<IdentifiedIdentity> identifiedIdentity;

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

    /**
     * Suspend the current authentication request, and allow the end-user to resume it later by clicking on a link for
     * example.
     *
     * @param suspensionHandler The {@link SuspensionHandler} to use for sending the suspension ID to the end-users.
     * @return An action builder that suspends the current authentication flow.
     */
    public static ActionBuilder suspend(SuspensionHandler suspensionHandler) {
        return new ActionBuilder(suspensionHandler);
    }

    private Action(JsonValue sharedState, JsonValue transientState, String outcome,
            Map<String, Object> returnProperties, String errorMessage, String lockoutMessage,
            List<? extends Callback> callbacks, Map<String, String> sessionProperties, List<JsonValue> sessionHooks,
            List<String> webhooks, SuspensionHandler suspensionHandler, Optional<String> universalId,
            Optional<IdentifiedIdentity> identifiedIdentity) {
        this.sharedState = sharedState;
        this.transientState = transientState;
        this.outcome = outcome;
        this.returnProperties = returnProperties;
        this.errorMessage = errorMessage;
        this.lockoutMessage = lockoutMessage;
        this.callbacks = Collections.unmodifiableList(callbacks);
        this.sessionProperties = Collections.unmodifiableMap(sessionProperties);
        this.sessionHooks = sessionHooks;
        this.webhooks = webhooks;
        this.suspensionHandler = suspensionHandler;
        this.universalId = universalId;
        this.identifiedIdentity = identifiedIdentity;
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
        private Map<String, Object> returnProperties;
        private String errorMessage;
        private String lockoutMessage;
        private String universalId;
        private IdentifiedIdentity identifiedIdentity;
        private final String outcome;
        private final List<? extends Callback> callbacks;
        private final Map<String, String> sessionProperties = new HashMap<>();
        private final List<JsonValue> sessionHooks = new ArrayList<>();
        private final List<String> webhooks = new ArrayList<>();
        private final SuspensionHandler suspensionHandler;

        private ActionBuilder(String outcome) {
            this.outcome = outcome;
            this.callbacks = Collections.emptyList();
            this.suspensionHandler = null;
        }

        private ActionBuilder(List<? extends Callback> callbacks) {
            this.outcome = null;
            this.callbacks = callbacks;
            this.suspensionHandler = null;
        }

        private ActionBuilder(SuspensionHandler suspensionHandler) {
            this.outcome = null;
            this.callbacks = Collections.emptyList();
            this.suspensionHandler = suspensionHandler;
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
         * Set a header for this action.
         *
         * @param header the header for the action.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder withHeader(String header) {
            if (header != null && !header.isEmpty()) {
                return withReturnProperty(HEADER, header);
            }
            return this;
        }

        /**
         * Set a description for this action.
         *
         * @param description the description for this action.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder withDescription(String description) {
            if (description != null && !description.isEmpty()) {
                return withReturnProperty(DESCRIPTION, description);
            }
            return this;
        }

        /**
         * Set a stage for this action.
         *
         * @param stage the stage for this action.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder withStage(String stage) {
            if (stage != null && !stage.isEmpty()) {
                return withReturnProperty(STAGE, stage);
            }
            return this;
        }

        /**
         * Set a property to be returned to the client.
         *
         * @param key The name of the property.
         * @param value The value of the property.
         * @return the same instance of the ActionBuilder.
         */
        private ActionBuilder withReturnProperty(String key, Object value) {
            if (returnProperties == null) {
                returnProperties = new HashMap<>();
            }
            returnProperties.put(key, value);
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
         * Sets the error message to present to the caller when the user is locked out.
         *
         * <p>It is up to the caller to apply localisation.</p>
         *
         * @param lockoutMessage The lockout message.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder withLockoutMessage(String lockoutMessage) {
            this.lockoutMessage = lockoutMessage;
            return this;
        }

        /**
         * Sets the universal id of the user object.
         *
         * @param universalId The universal id of the user object.
         * @return the same instance of the ActionBuilder.
         * @deprecated use {@link #withIdentifiedIdentity} instead.
         */
        @Deprecated
        public ActionBuilder withUniversalId(String universalId) {
            if (isNotEmpty(universalId)) {
                this.universalId = universalId;
            }
            return this;
        }

        /**
         * Sets the universal id of the user object.
         *
         * <p>If {@literal universalId} is empty no action will take place.</p>
         *
         * @param universalId The optional universal id of the user object.
         * @return the same instance of the ActionBuilder.
         * @deprecated use {@link #withIdentifiedIdentity} instead.
         */
        @Deprecated
        public ActionBuilder withUniversalId(Optional<String> universalId) {
            universalId.ifPresent(this::withUniversalId);
            return this;
        }

        /**
         * Add a new session property.
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
            return addSessionHook(sessionHook, nodeId, nodeType, null);
        }

        /**
         * Sets a class that will be run after successful login.
         * The class has to implement {@link TreeHook}.
         *
         * @param sessionHook The class to be run.
         * @param nodeId The ID of the node.
         * @param nodeType The type of the node.
         * @param data The additional data for session hook.
         * @return the same instance of ActionBuilder
         */
        public ActionBuilder addSessionHook(Class<? extends TreeHook> sessionHook, UUID nodeId, String nodeType,
                JsonValue data) {
            Reject.ifNull(sessionHook);
            sessionHooks.add(
                    json(object(
                            field(TreeHook.SESSION_HOOK_CLASS_KEY, sessionHook.getName()),
                            field(TreeHook.NODE_ID_KEY, nodeId.toString()),
                            field(TreeHook.NODE_TYPE_KEY, nodeType),
                            fieldIfNotNull(TreeHook.HOOK_DATA, data))));
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
         * Set the identified identity that has been verified to exist in an identity store.
         * <p>The identity may or may not have been authenticated as part of the tree execution.
         *
         * @param username the username of the identified identity.
         * @param identityType the identity type of the identified identity. Must be one of the identity types
         *                     defined in com.sun.identity.idm.IdType.
         * @return this action builder.
         */
        public ActionBuilder withIdentifiedIdentity(String username, IdType identityType) {
            identifiedIdentity = new IdentifiedIdentity(username, identityType);
            return this;
        }

        /**
         * Set the identified identity that has been verified to exist in an identity store.
         * <p>The identity may or may not have been authenticated as part of the tree execution.
         *
         * @param identity the identified identity.
         * @return this action builder.
         */
        public ActionBuilder withIdentifiedIdentity(AMIdentity identity) {
            return withIdentifiedIdentity(identity.getName(), identity.getType());
        }

        /**
         * Build the Action.
         *
         * @return an Action.
         * @throws IllegalStateException if the builder is called before building mandatory fields.
         */
        public Action build() {
            return new Action(this.sharedState, this.transientState, this.outcome, this.returnProperties,
                    this.errorMessage, this.lockoutMessage, this.callbacks, sessionProperties, sessionHooks,
                    webhooks, suspensionHandler, Optional.ofNullable(this.universalId),
                    Optional.ofNullable(identifiedIdentity));
        }
    }
}
