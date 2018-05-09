/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.trees.engine;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.TARGET_AUTH_LEVEL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.Reject;

/**
 * An immutable container for the state associated with a partially or completely evaluated tree.
 */
public final class TreeState {

    /** The state populated by the nodes in the tree. */
    public final JsonValue sharedState;
    /** The transient state populated by the nodes in the tree.
     * The values stored in this state is request scoped  would not be shared across multiple requests/callbacks
     * in an authentication tree flow. Every time a callback is sent to the user the transient state would be reset
     * to an empty json value object. */
    public final JsonValue transientState;
    /** The ID of the most recently processed node. */
    public final UUID currentNodeId;
    /** Properties that will be included in the user's session if/when it is created. */
    public final Map<String, String> sessionProperties;
    /** List of classes that are run after successful authentication. */
    public final List<JsonValue> sessionHooks;
    /** List of webhooks that are run after session logout. */
    public final List<String> webhooks;

    /**
     * Constructs a new TreeState.
     * @param sharedState The state populated by the nodes in the tree.
     * @param currentNodeId The ID of the most recently processed node. May be null.
     */
    public TreeState(JsonValue sharedState, UUID currentNodeId) {
        this(sharedState, json(object()), currentNodeId, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Constructs a new TreeState.
     * @param sharedState The state populated by the nodes in the tree.
     * @param currentNodeId The ID of the most recently processed node. May be null.
     * @param sessionProperties Properties that will be included in the user's session.
     * @param sessionHooks List of classes that are run after successful authentication.
     * @param webhooks List of webhook names that are run after session logout.
     */
    public TreeState(JsonValue sharedState, UUID currentNodeId, Map<String, String> sessionProperties,
            List<JsonValue> sessionHooks, List<String> webhooks) {
        this(sharedState, json(object()), currentNodeId, sessionProperties, sessionHooks, webhooks);
    }

    /**
     * Constructs a new TreeState.
     *
     * @param sharedState The state populated by the nodes in the tree.
     * @param transientState The transient state populated by nodes in the tree.
     * @param currentNodeId The ID of the most recently processed node. May be null.
     * @param sessionProperties Properties that will be included in the user's session.
     * @param sessionHooks List of classes that are run after successful authentication.
     * @param webhooks List of webhook names that are run after session logout.
     */
    public TreeState(JsonValue sharedState, JsonValue transientState, UUID currentNodeId,
            Map<String, String> sessionProperties, List<JsonValue> sessionHooks, List<String> webhooks) {
        Reject.ifNull(sharedState);
        Reject.ifNull(transientState);
        this.sharedState = sharedState;
        this.transientState = transientState;
        this.currentNodeId = currentNodeId;
        this.sessionProperties = sessionProperties;
        this.sessionHooks = sessionHooks;
        this.webhooks = webhooks;
    }

    /**
     * Returns the JsonValue format of the TreeState object.
     * @return JsonValue object of TreeState.
     */
    public JsonValue toJson() {
        return json(object(
                field("sharedState", sharedState.getObject()),
                field("currentNodeId", currentNodeId.toString()),
                field("sessionProperties", sessionProperties),
                field("sessionHooks", sessionHooks),
                field("webhooks", webhooks)
        ));
    }

    /**
     * Returns a TreeState object from the JsonValue.
     * @param json Json representation of the TreeState object.
     * @return TreeState object
     */
    public static TreeState fromJson(JsonValue json) {
        return new TreeState(json.get("sharedState"), json(object()),
                UUID.fromString(json.get("currentNodeId").asString()),
                json.get("sessionProperties").asMap(String.class),
                json.get("sessionHooks").asList(JsonValue.class),
                json.get("webhooks").asList(String.class));
    }

    /**
     * Creates a tree state that represents start of authentication tree.
     *
     * @param realm the realm in which the authentication is taking place.
     * @param targetAuthLevel minimum auth level required for authentication.
     * @return the initial TreeState
     */
    public static TreeState createInitial(Realm realm, Integer targetAuthLevel) {
        Map<String, Object> object = object(
                field(REALM, realm.asPath()),
                field(AUTH_LEVEL, 0));
        if (targetAuthLevel != null) {
            object.put(TARGET_AUTH_LEVEL, targetAuthLevel);
        }
        return new TreeState(json(object), json(object()), null, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
    }
}