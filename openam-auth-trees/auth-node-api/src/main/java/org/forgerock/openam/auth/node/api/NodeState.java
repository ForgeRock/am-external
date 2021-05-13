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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;

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
    NodeState(List<String> stateFilter, JsonValue transientState, JsonValue secureState, JsonValue sharedState) {
        if (stateFilter.contains(STATE_FILTER_WILDCARD)) {
            this.stateFilter = Collections.emptyList();
        } else {
            this.stateFilter = stateFilter;
        }
        this.transientState = transientState;
        this.secureState = secureState;
        this.sharedState = sharedState;
    }

    /**
     * Create an instance of the {@link NodeState} with the provided state {@link JsonValue}.
     *
     * <p>This constructor variant will allow access to all keys in the node state. If the caller intends
     * to limit which keys can be accessed then {@link NodeState#NodeState(List, JsonValue, JsonValue, JsonValue)}
     * should be used instead.

     * @param transientState A non-null {@link JsonValue} which represents the transient state.
     * @param secureState A non-null {@link JsonValue} which represents the secure state.
     * @param sharedState A non-null {@link JsonValue} which represents the shared state.
     */
    NodeState(JsonValue transientState, JsonValue secureState, JsonValue sharedState) {
        this(singletonList(STATE_FILTER_WILDCARD), transientState, secureState, sharedState);
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
                .filter(key -> !stateFilter.contains(key))
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
