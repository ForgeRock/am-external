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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Supported;
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
@SupportedAll(scriptingApi = true, javaApi = false)
public final class NodeStateScriptWrapper {

    private final NodeState nodeState;


    /**
     * Constructor for wrapping a NodeState object. Not exposed to scripts.
     * @param nodeState the NodeState object to wrap.
     */
    @Supported(scriptingApi = false, javaApi = false)
    public NodeStateScriptWrapper(NodeState nodeState) {
        this.nodeState = nodeState;
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
    public Object get(String key) {
        JsonValue jsonValue = nodeState.get(key);
        return jsonValue == null ? null : jsonValue.getObject();
    }

    /**
     * Get the value for the given key from the state. Combines state from transient, secure and shared when values are
     * maps into an immutable object.
     * @param key The key.
     * @return The value or null if the key is not defined.
     */
    public Object getObject(String key) {
        JsonValue jsonValue = nodeState.getObject(key);
        return jsonValue == null ? null : jsonValue.getObject();
    }

    /**
     * Checks if the given {@literal key} is defined in any of the types of state.
     *
     * @param key The key.
     * @return {@code true} if the key is defined, otherwise {@code false}.
     */
    public boolean isDefined(String key) {
        return nodeState.isDefined(key);
    }

    /**
     * Gets the distinct keys from across all types of state.
     *
     * @return The set of all keys.
     */
    public Set<String> keys() {
        return nodeState.keys();
    }

    /**
     * Puts the given key/value pair in the shared state.
     *
     * <p>The shared state should only be used for non-sensitive information that will be signed
     * on round trips to the client.
     *
     * @param key The key.
     * @param value The value.
     * @return This modified {@code NodeStateScriptWrapper} instance.
     */
    public NodeStateScriptWrapper putShared(String key, Object value) {
        nodeState.putShared(key, value);
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
     * @return This modified {@code NodeStateScriptWrapper} instance.
     */
    public NodeStateScriptWrapper putTransient(String key, Object value) {
        nodeState.putTransient(key, value);
        return this;
    }

    /**
     * Puts the given object into the shared state.
     *
     * <p>If any of the keys exist already in any state they will be overwritten.
     * <p>The shared state should only be used for non-sensitive information that will be signed
     * on round trips to the client.
     * <p>If any keys in the input object match the shared state keys, the new values will be used.
     * <p>The object must not contain any nested objects unless they are registered state containers,
     * for example objectAttributes.
     *
     * @param object The object to merge into the shared state.
     * @return This modified {@code NodeStateScriptWrapper} instance.
     */
    public NodeStateScriptWrapper mergeShared(Map<String, Object> object) {
        nodeState.mergeShared(object);
        return this;
    }

    /**
     * Puts the given object into the shared state.
     *
     * <p>If any of the keys exist already in any state they will be overwritten.
     * <p>The transient state should only be used for sensitive information that will be encrypted
     * on round trips to the client.
     * <p>If any keys in the input object match the shared state keys, the new values will be used.
     * <p>The object must not contain any nested objects unless they are registered state containers,
     * for example objectAttributes.
     *
     * @param object The object to merge into the shared state.
     * @return This modified {@code NodeState} instance.
     */
    public NodeStateScriptWrapper mergeTransient(Map<String, Object> object) {
        nodeState.mergeTransient(object);
        return this;
    }


    /**
     * Removes the given {@literal key} from all states.
     *
     * @param key The key to remove.
     */
    public void remove(String key) {
        nodeState.remove(key);
    }
}
