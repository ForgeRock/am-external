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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.util.Reject;

/**
 * Encapsulates all state that is provided by each node and passed between nodes on tree execution.
 *
 * <p>
 *     There are three types of state: transient, secure and shared.
 *     Shared state is non-sensitive state, secure state is decrypted transient state and
 *     transient state is sensitive state that will be encrypted on round trips to the client.
 *
 * <p>This class encapsulates all three types of state to abstract from where a node's implementation
 * decides to store its state based on each state's properties. Callers to this class should not
 * know or care what type of state a particular piece of state is stored, only that it can be
 * retrieved. This class also exposes the ability for callers to add state to either the shared state
 * (non-sensitive) or transient state (sensitive).
 */
@SupportedAll
public final class NodeState {

    /** Wildcard state filter that allows access to all state. */
    public static final String STATE_FILTER_WILDCARD = "*";

    private final List<String> stateFilter;
    private final JsonValue transientState;
    private final JsonValue secureState;
    private final JsonValue sharedState;
    private final Set<String> validContainers;

    /**
     * Create an instance of the {@link NodeState} with the provided state {@link JsonValue}.
     *
     * <p>This constructor allows the definition of a {@literal stateFilter} which limits which values can
     * be requested from the {@link NodeState}. If this filter is set to a single list element containing a
     * {@link String} value of {@literal *} then all keys in the node state can be accessed.
     *
     * @param stateFilter A non-null, possibly empty list of the allowed keys that can be requested from this
     *                    {@link NodeState}.
     * @param transientState A non-null {@link JsonValue} which represents the transient state.
     * @param secureState A non-null {@link JsonValue} which represents the secure state.
     * @param sharedState A non-null {@link JsonValue} which represents the shared state.
     */
    NodeState(List<String> stateFilter, JsonValue transientState, JsonValue secureState, JsonValue sharedState,
            Set<String> validContainers) {
        if (stateFilter.contains(STATE_FILTER_WILDCARD)) {
            this.stateFilter = Collections.emptyList();
        } else {
            this.stateFilter = stateFilter;
        }
        this.transientState = transientState;
        this.secureState = secureState;
        this.sharedState = sharedState;
        this.validContainers = validContainers;
    }

    /**
     * Create an instance of the {@link NodeState} with the provided state {@link JsonValue}.
     *
     * <p>This constructor variant will allow access to all keys in the node state. If the caller intends
     * to limit which keys can be accessed then {@link NodeState#NodeState(List, JsonValue, JsonValue, JsonValue, Set)}
     * should be used instead.

     * @param transientState A non-null {@link JsonValue} which represents the transient state.
     * @param secureState A non-null {@link JsonValue} which represents the secure state.
     * @param sharedState A non-null {@link JsonValue} which represents the shared state.
     */
    NodeState(JsonValue transientState, JsonValue secureState, JsonValue sharedState, Set<String> validContainers) {
        this(singletonList(STATE_FILTER_WILDCARD), transientState, secureState, sharedState, validContainers);
    }

    /**
     * Gets the value for the given {@literal key} from the state.
     *
     * <p>The order of state types checked is the following:
     * <ol>
     *     <li>transient</li>
     *     <li>secure</li>
     *     <li>shared</li>
     * </ol>
     *
     * @param key The key.
     * @return The value or {@code null} if the key is not defined.
     */
    public JsonValue get(String key) {
        if (isDefined(key)) {
            if (transientState.isDefined(key)) {
                return transientState.get(key);
            } else if (secureState.isDefined(key)) {
                return secureState.get(key);
            } else {
                return sharedState.get(key);
            }
        } else {
            return null;
        }
    }

    /**
     * Get the value for the given key from the state. Combines state from transient, secure and shared when values are
     * maps into an immutable object.
     * @param key The key.
     * @return The value or null if the key is not defined.
     */
    public JsonValue getObject(String key) {
        JsonValue value = get(key);
        if (value != null && value.isMap()) {
            JsonValue sharedStateValue = json(sharedState.isDefined(key) && sharedState.get(key).isMap()
                    ? sharedState.get(key) : object());
            JsonValue secureStateValue = json(secureState.isDefined(key) && secureState.get(key).isMap()
                    ? secureState.get(key) : object());
            JsonValue transientStateValue = json(transientState.isDefined(key) && transientState.get(key).isMap()
                    ? transientState.get(key) : object());

            return json(Collections.unmodifiableMap(
                    sharedStateValue.merge(secureStateValue).merge(transientStateValue).asMap()));
        } else {
            return value;
        }
    }

    /**
     * Checks if the given {@literal key} is defined in any of the types of state.
     *
     * @param key The key.
     * @return {@code true} if the key is defined, otherwise {@code false}.
     */
    public boolean isDefined(String key) {
        return (stateFilter.isEmpty() || stateFilter.contains(key))
                && (transientState.isDefined(key) || secureState.isDefined(key) || sharedState.isDefined(key));
    }

    /**
     * Gets the distinct keys from across all types of state.
     *
     * @return The set of all keys.
     */
    public Set<String> keys() {
        return Stream.of(transientState.keys(), secureState.keys(), sharedState.keys())
                .flatMap(Collection::stream)
                .filter(key -> stateFilter.isEmpty() || stateFilter.contains(key))
                .collect(toSet());
    }

    /**
     * Puts the given key/value pair in the shared state.
     *
     * <p>The shared state should only be used for non-sensitive information that will be signed
     * on round trips to the client.
     *
     * @param key The key.
     * @param value The value.
     * @return This modified {@code NodeState} instance.
     */
    public NodeState putShared(String key, Object value) {
        sharedState.put(key, value);
        return this;
    }

    /**
     * Puts the given object into the shared state.
     *
     * <p>If any of the keys exist already in any state they will be overwritten.
     * <p>The shared state should only be used for non-sensitive information that will be signed
     * on round trips to the client.
     * <p>If any keys in the input object match the shared state keys, the new values will be used.
     * <p>The object must not contain any objects at the root level unless they are registered state containers,
     * for example objectAttributes. Nested objects are allowed inside registered state containers but will not
     * be merged.
     *
     * @param object The object to merge into the shared state.
     * @return This modified {@code NodeState} instance.
     */
    public NodeState mergeShared(Map<String, Object> object) {
        mergeObject(object, sharedState, Set.of(transientState, secureState));
        return this;
    }

    /**
     * Puts the given object into the shared state.
     *
     * <p>If any of the keys exist already in any state they will be overwritten.
     * <p>The transient state should only be used for sensitive information that will be encrypted
     * on round trips to the client.
     * <p>If any keys in the input object match the shared state keys, the new values will be used.
     * <p>The object must not contain any objects at the root level unless they are registered state containers,
     * for example objectAttributes. Nested objects are allowed inside registered state containers but will not
     * be merged.
     *
     * @param object The object to merge into the shared state.
     * @return This modified {@code NodeState} instance.
     */
    public NodeState mergeTransient(Map<String, Object> object) {
        mergeObject(object, transientState, Set.of(secureState, sharedState));
        return this;
    }

    private void mergeObject(Map<String, Object> object, JsonValue state, Set<JsonValue> otherStates) {
        Reject.ifNull(object);
        object.forEach((key, value) -> {
            if (validContainers.contains(key)) {
                if (!(value instanceof Map)) {
                    throw new IllegalArgumentException("State containers must be a JSON object.");
                }
                if (!state.isDefined(key)) {
                    state.put(key, object());
                }
                JsonValue containerState = state.get(key);
                Set<JsonValue> otherContainerStates = otherStates.stream()
                                                              .map(s -> s.get(key))
                                                              .filter(JsonValue::isNotNull)
                                                              .collect(toSet());
                Map<String, Object> containerObject = (Map<String, Object>) value;
                containerObject.forEach((containerKey, containerValue) -> {
                    otherContainerStates.forEach(s -> s.remove(containerKey));
                    containerState.put(containerKey, containerValue);
                });
            } else if (value instanceof Map) {
                throw new IllegalArgumentException("State must not contain nested objects unless they are inside "
                                                           + "registered state containers: "
                                                           + String.join(",", this.validContainers));
            } else {
                otherStates.forEach(s -> s.remove(key));
                state.put(key, value);
            }
        });
    }

    /**
     * Puts the given key/value pair in the transient state.
     *
     * <p>The transient state should only be used for sensitive information that will be encrypted
     * on round trips to the client.
     *
     * @param key The key.
     * @param value The value.
     * @return This modified {@code NodeState} instance.
     */
    public NodeState putTransient(String key, Object value) {
        transientState.put(key, value);
        return this;
    }

    /**
     * Removes the given {@literal key} from all states.
     *
     * @param key The key to remove.
     */
    public void remove(String key) {
        transientState.remove(key);
        secureState.remove(key);
        sharedState.remove(key);
    }
}
