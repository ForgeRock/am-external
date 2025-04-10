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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
    @Deprecated
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
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     * @deprecated use {@link #TreeContext(String, JsonValue, JsonValue, JsonValue, ExternalRequestContext, List,
     * boolean, Optional, Set)}
     */
    @Deprecated
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
     */
    public TreeContext(String identityResource, JsonValue sharedState, JsonValue transientState,
            JsonValue secureState, ExternalRequestContext request, List<? extends Callback> callbacks,
            boolean resumedFromSuspend, Optional<String> universalId, Set<String> treeStateContainers) {
        this.sharedState = requireNonNull(sharedState);
        this.secureState = requireNonNull(secureState);
        this.transientState = requireNonNull(transientState);
        this.request = requireNonNull(request);
        this.callbacks = unmodifiableList(requireNonNull(callbacks));
        this.resumedFromSuspend = resumedFromSuspend;
        this.identityResource = requireNonNull(identityResource);
        this.universalId = requireNonNull(universalId);
        this.treeStateContainers = unmodifiableSet(requireNonNull(treeStateContainers));
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
     */
    public TreeContext(JsonValue sharedState, JsonValue transientState, JsonValue secureState,
            ExternalRequestContext request, List<? extends Callback> callbacks,
            boolean resumedFromSuspend, Optional<String> universalId) {
        this(DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, transientState, secureState, request, callbacks,
                resumedFromSuspend, universalId);
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
                resumedFromSuspend, universalId);
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
                request, callbacks, resumedFromSuspend, universalId);
    }

    /**
     * Get the tree state containers available in this context.
     * @return The tree state containers.
     */
    public Set<String> treeStateContainers() {
        return treeStateContainers;
    }
}
