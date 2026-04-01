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
 * Copyright 2017-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.node.api;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;

/**
 * A representation of the context of the current tree authentication process.
 */
@SupportedAll
public final class TreeContext {

    /**
     * The tree config key for the storage of the IDM identity resource.
     */
    public static final String IDM_IDENTITY_RESOURCE = "identityResource";

    /**
     * The default IDM identity resource.
     */
    public static final String DEFAULT_IDM_IDENTITY_RESOURCE = "managed/user";

    /**
     * The shared state that has accumulated so far by traversing the tree.
     * @deprecated Use {@link #getStateFor(Node)} instead as this method does not leak implementation detail
     * of the specific type of state.
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public final JsonValue sharedState;
    /**
     * The HTTP request associated with the current authentication request.
     */
    public final ExternalRequestContext request;
    /**
     * The state that is transient and is encrypted and stored across requests only if downstream nodes
     * declare they use it.
     * @deprecated Use {@link #getStateFor(Node)} instead as this method does not leak implementation detail
     * of the specific type of state.
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public final JsonValue transientState;
    /**
     * The identity objects universal id.
     */
    public final Optional<String> universalId;
    /**
     * The state that contains entries promoted from transientState that have been retained across callbacks.
     */
    private final JsonValue secureState;
    /**
     * The IDM identity resource container for all IDM object interactions in this tree.
     */
    public final String identityResource;
    /**
     * The identified identity if present.
     */
    public final Optional<IdentifiedIdentity> identifiedIdentity;

    private final List<? extends Callback> callbacks;

    private final boolean resumedFromSuspend;
    private final Set<String> treeStateContainers;


    /**
     * Construct a tree context for the current state.
     *
     * @param identityResource The IDM identity resource.
     * @param sharedState The shared state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(String identityResource, JsonValue sharedState, ExternalRequestContext request,
            List<? extends Callback> callbacks) {
        this(identityResource, sharedState, json(object()), json(object()), request, callbacks, Optional.empty());
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param sharedState The shared state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(JsonValue sharedState, ExternalRequestContext request,
            List<? extends Callback> callbacks, Optional<String> universalId) {
        this(DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, json(object()),
             json(object()), request, callbacks, universalId);
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param identityResource The IDM identity resource.
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(String identityResource, JsonValue sharedState, JsonValue transientState,
            ExternalRequestContext request, List<? extends Callback> callbacks, Optional<String> universalId) {
        this(identityResource, sharedState, transientState, json(object()), request, callbacks, universalId);
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(JsonValue sharedState, JsonValue transientState,
            ExternalRequestContext request, List<? extends Callback> callbacks, Optional<String> universalId) {
        this(DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
             transientState, json(object()), request, callbacks, universalId);
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param identityResource The IDM identity resource.
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param secureState The secure state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(String identityResource, JsonValue sharedState, JsonValue transientState,
            JsonValue secureState, ExternalRequestContext request,
            List<? extends Callback> callbacks, Optional<String> universalId) {
        this(identityResource, sharedState, transientState, secureState, request, callbacks, false, universalId);
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param secureState The secure state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(JsonValue sharedState, JsonValue transientState, JsonValue secureState,
            ExternalRequestContext request, List<? extends Callback> callbacks, Optional<String> universalId) {
        this(DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, transientState,
             secureState, request, callbacks, false, universalId, Set.of());
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param identityResource The IDM identity resource.
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param secureState The secure state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param resumedFromSuspend whether the tree has been resumed from having been suspended.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public TreeContext(String identityResource, JsonValue sharedState, JsonValue transientState,
            JsonValue secureState, ExternalRequestContext request, List<? extends Callback> callbacks,
            boolean resumedFromSuspend, Optional<String> universalId) {
        this(identityResource, sharedState, transientState, secureState, request, callbacks, resumedFromSuspend,
             universalId, Set.of());
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param identityResource The IDM identity resource.
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param secureState The secure state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param resumedFromSuspend whether the tree has been resumed from having been suspended.
     * @param universalId The universal id of the identity object.
     * @param treeStateContainers The set of containers that are valid for the tree.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.1.0")
    public TreeContext(String identityResource, JsonValue sharedState, JsonValue transientState,
            JsonValue secureState, ExternalRequestContext request, List<? extends Callback> callbacks,
            boolean resumedFromSuspend, Optional<String> universalId, Set<String> treeStateContainers) {
        this(identityResource, sharedState, transientState, secureState, request, callbacks, resumedFromSuspend,
                universalId, Optional.empty(), treeStateContainers);
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param secureState The secure state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param resumedFromSuspend whether the tree has been resumed from having been suspended.
     * @param universalId The universal id of the identity object.
     * @deprecated use {@link #builder()}
     */
    @Deprecated(since = "8.1.0")
    public TreeContext(JsonValue sharedState, JsonValue transientState, JsonValue secureState,
                       ExternalRequestContext request, List<? extends Callback> callbacks,
                       boolean resumedFromSuspend, Optional<String> universalId) {
        this(DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, transientState, secureState, request, callbacks,
                resumedFromSuspend, universalId);
    }

    /**
     * Construct a tree context for the current state.
     *
     * @param identityResource The IDM identity resource.
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param secureState The secure state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     * @param resumedFromSuspend whether the tree has been resumed from having been suspended.
     * @param universalId The universal id of the identity object.
     * @param identifiedIdentity The identified identity if present.
     * @param treeStateContainers The set of containers that are valid for the tree.
     */
    private TreeContext(String identityResource,
                       JsonValue sharedState,
                       JsonValue transientState,
                       JsonValue secureState,
                       ExternalRequestContext request,
                       List<? extends Callback> callbacks,
                       boolean resumedFromSuspend,
                       Optional<String> universalId,
                       Optional<IdentifiedIdentity> identifiedIdentity,
                       Set<String> treeStateContainers) {
        this.sharedState = requireNonNull(sharedState);
        this.secureState = requireNonNull(secureState);
        this.transientState = requireNonNull(transientState);
        this.request = requireNonNull(request);
        this.callbacks = unmodifiableList(requireNonNull(callbacks));
        this.resumedFromSuspend = resumedFromSuspend;
        this.identityResource = requireNonNull(identityResource);
        this.universalId = requireNonNull(universalId);
        this.identifiedIdentity = requireNonNull(identifiedIdentity);
        this.treeStateContainers = unmodifiableSet(requireNonNull(treeStateContainers));
    }

    /**
     * Get the first callback of a particular type from the callbacks in the context.
     *
     * @param callbackType The type of callback.
     * @param <T> The generic type of the callback.
     * @return An optional of the callback or empty if no callback of that type existed.
     */
    public <T extends Callback> Optional<T> getCallback(Class<T> callbackType) {
        return callbacks.stream()
                .filter(c -> callbackType.isAssignableFrom(c.getClass()))
                .map(callbackType::cast)
                .findFirst();
    }

    /**
     * Get the callbacks of a particular type from the callbacks in the context.
     *
     * @param callbackType The type of callback.
     * @param <T> The generic type of the callback.
     * @return An list of callbacks or empty if no callback of that type existed.
     */
    public <T extends Callback> List<T> getCallbacks(Class<T> callbackType) {
        return callbacks.stream()
                .filter(c -> callbackType.isAssignableFrom(c.getClass()))
                .map(callbackType::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets all the callbacks sent in the request.
     *
     * <p>
     * Use {@link #getCallback(Class)} in preference to this method.
     * </p>
     *
     * @return An unmodifiable list of callbacks.
     */
    public List<? extends Callback> getAllCallbacks() {
        return callbacks;
    }

    /**
     * Check if any callbacks have been submitted in this authenticate request.
     *
     * @return {@code true} if there are callbacks available.
     */
    public boolean hasCallbacks() {
        return !callbacks.isEmpty();
    }

    /**
     * Returns a new instance of {@link NodeState} encapsulating all types of state and allowing access
     * to state in the following order.
     * <ol>
     *     <li>transient</li>
     *     <li>secure</li>
     *     <li>shared</li>
     * </ol>
     * @param node The node whose state is being requested.
     * @return The unified {@link NodeState}.
     */
    public NodeState getStateFor(Node node) {
        return new NodeState(Arrays.stream(node.getInputs())
                .map(inputState -> inputState.name)
                .collect(Collectors.toList()), transientState, secureState, sharedState, treeStateContainers);
    }

    /**
     * Retrieves a field from one of the three supported state locations, or null if the key is not found in any of the
     * state locations.
     *
     * @param stateKey The key to look for.
     * @return The first occurrence of the key from the states, in order: Transient, Secure, Shared.
     * @deprecated Use {@link #getStateFor(Node)} instead as this method performs the same function
     * but supports filtering of the available state based on the nodes declared inputs.
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public JsonValue getState(String stateKey) {
        if (transientState.isDefined(stateKey)) {
            return transientState.get(stateKey);
        } else if (secureState.isDefined(stateKey)) {
            return secureState.get(stateKey);
        } else if (sharedState.isDefined(stateKey)) {
            return sharedState.get(stateKey);
        }
        return null;
    }

    /**
     * Retrieves a field from one the secure state, or null if the key is not found.
     *
     * @param stateKey The key to look for.
     * @return The key value from secure state if present.
     * @deprecated Use {@link #getStateFor(Node)} instead as this method does not leak implementation detail
     * of the specific type of state.
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public JsonValue getSecureState(String stateKey) {
        return secureState.get(stateKey).defaultTo(null);
    }

    /**
     * Retrieves a field from either transient or secured state, or null if the key is not found in any of the
     * state locations.
     *
     * @param stateKey The key to look for.
     * @return The first occurrence of the key from the states, in order: Secure, Transient.
     * @deprecated Use {@link #getStateFor(Node)} instead as this method does not leak implementation detail
     * of the specific type of state.
     */
    @Deprecated(since = "8.0.0", forRemoval = true)
    public JsonValue getTransientState(String stateKey) {
        if (secureState.isDefined(stateKey)) {
            return secureState.get(stateKey);
        } else if (transientState.isDefined(stateKey)) {
            return transientState.get(stateKey);
        }
        return null;
    }

    /**
     * Whether the tree has been resumed from having been suspended.
     *
     * @return whether the tree has been resumed from having been suspended
     */
    public boolean hasResumedFromSuspend() {
        return resumedFromSuspend;
    }

    /**
     * Copies this TreeContext instance, replacing the callbacks in the context with the provided callbacks.
     *
     * @param callbacks The new callbacks.
     * @return A new TreeContext instance.
     */
    public TreeContext copyWithCallbacks(List<? extends Callback> callbacks) {
        return new TreeContext(identityResource, sharedState, transientState, secureState, request, callbacks,
                resumedFromSuspend, universalId, treeStateContainers);
    }

    /**
     * Copies this TreeContext instance, replacing the callbacks in the context with the provided callbacks and
     * with the provided new state if input state parameter is not null.
     *
     * @param sharedState The new sharedState, if null use previous value.
     * @param transientState The new transientState, if null use previous value.
     * @param secureState The new secureState, if null use previous value.
     * @param callbacks The new callbacks.
     * @return A new TreeContext instance.
     */
    public TreeContext copyWithCallbacksAndState(JsonValue sharedState, JsonValue transientState,
                                                 JsonValue secureState, List<? extends Callback> callbacks) {

        return new TreeContext(identityResource, (sharedState != null) ? sharedState : this.sharedState,
                (transientState != null) ? transientState : this.transientState,
                (secureState != null) ? secureState : this.secureState,
                request, callbacks, resumedFromSuspend, universalId, treeStateContainers);
    }

    /**
     * Get the tree state containers available in this context.
     * @return The tree state containers.
     */
    public Set<String> treeStateContainers() {
        return treeStateContainers;
    }

    /**
     * Creates a TreeContext builder.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for TreeContext.
     */
    public static class Builder {
        private String identityResource = DEFAULT_IDM_IDENTITY_RESOURCE;
        private JsonValue sharedState = json(object());
        private JsonValue transientState = json(object());
        private JsonValue secureState = json(object());
        private ExternalRequestContext requestContext = new ExternalRequestContext.Builder().build();
        private List<? extends Callback> callbacks = List.of();
        private boolean resumedFromSuspend = false;
        private Optional<String> universalId = Optional.empty();
        private Optional<IdentifiedIdentity> identifiedIdentity = Optional.empty();
        private Set<String> treeStateContainers = Set.of();

        /**
         * Constructor, for required parameters.
         */
        public Builder() {
        }

        /**
         * Set the identity resource.
         * @param identityResource The identity resource
         * @return this builder
         */
        public Builder identityResource(String identityResource) {
            this.identityResource = identityResource;
            return this;
        }

        /**
         * Set the shared state.
         * @param sharedState The shared state
         * @return this builder
         */
        public Builder sharedState(JsonValue sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Set the transient state.
         * @param transientState The transient state
         * @return this builder
         */
        public Builder transientState(JsonValue transientState) {
            this.transientState = transientState;
            return this;
        }

        /**
         * Set the secure state.
         * @param secureState The secure state
         * @return this builder
         */
        public Builder secureState(JsonValue secureState) {
            this.secureState = secureState;
            return this;
        }

        /**
         * Set the request context.
         * @param requestContext The request context
         * @return this builder
         */
        public Builder requestContext(ExternalRequestContext requestContext) {
            this.requestContext = requestContext;
            return this;
        }

        /**
         * Set the callbacks.
         * @param callbacks The callbacks
         * @return this builder
         */
        public Builder callbacks(List<? extends Callback> callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        /**
         * Set resumedFromSuspend.
         * @param resumedFromSuspend whether the tree has been resumed from having been suspended
         * @return this builder
         */
        public Builder resumedFromSuspend(boolean resumedFromSuspend) {
            this.resumedFromSuspend = resumedFromSuspend;
            return this;
        }

        /**
         * Set the universal id.
         * @param universalId The universal id
         * @return this builder
         */
        public Builder universalId(String universalId) {
            this.universalId = Optional.of(universalId);
            return this;
        }

        /**
         * Set the universal id.
         * @param universalId The universal id
         * @return this builder
         */
        public Builder universalId(Optional<String> universalId) {
            this.universalId = universalId;
            return this;
        }

        /**
         * Set the identified identity.
         * @param identifiedIdentity The identified identity
         * @return this builder
         */
        public Builder identifiedIdentity(IdentifiedIdentity identifiedIdentity) {
            this.identifiedIdentity = Optional.of(identifiedIdentity);
            return this;
        }

        /**
         * Set the identified identity.
         * @param identifiedIdentity The identified identity
         * @return this builder
         */
        public Builder identifiedIdentity(Optional<IdentifiedIdentity> identifiedIdentity) {
            this.identifiedIdentity = identifiedIdentity;
            return this;
        }

        /**
         * Set the tree state containers.
         * @param treeStateContainers The tree state containers
         * @return this builder
         */
        public Builder treeStateContainers(Set<String> treeStateContainers) {
            this.treeStateContainers = treeStateContainers;
            return this;
        }

        /**
         * Build the TreeContext.
         * @return the TreeContext
         */
        public TreeContext build() {
            return new TreeContext(identityResource, sharedState, transientState, secureState, requestContext,
                    callbacks, resumedFromSuspend, universalId, identifiedIdentity, treeStateContainers);
        }
    }
}
