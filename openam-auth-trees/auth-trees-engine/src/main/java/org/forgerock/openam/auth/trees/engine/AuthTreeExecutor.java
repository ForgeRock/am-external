/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.trees.engine;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.TARGET_AUTH_LEVEL;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sun.identity.authentication.util.ISAuthConstants;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.audit.AuthenticationNodeEventAuditor;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the algorithm for processing an authentication tree.
 *
 * @since AM 5.5.0
 */
@Singleton
public class AuthTreeExecutor {

    private static final String TARGET_AUTH_LEVEL_CANNOT_BE_FULFILLED =
            "The target authentication level cannot be fulfilled along this path of the authentication tree";

    private final NodeFactory nodeFactory;
    private final AuthenticationNodeEventAuditor auditor;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Constructs a new AuthTreeExecutor.
     * @param nodeFactory a node factory.
     * @param auditor a node event auditor.
     */
    @Inject
    public AuthTreeExecutor(NodeFactory nodeFactory, AuthenticationNodeEventAuditor auditor) {
        this.nodeFactory = nodeFactory;
        this.auditor = auditor;
    }

    /**
     * Process an authentication tree until either:
     * <ul>
     *     <li>A node requests input from the user.</li>
     *     <li>The tree completes with a success or failure.</li>
     * </ul>
     *
     * <p>The tree is defined as complete when the processing reaches either the
     * {@link ISAuthConstants#TREE_NODE_SUCCESS_ID} or {@link ISAuthConstants#TREE_NODE_FAILURE_ID} node.</p>
     *
     * @param realm The realm the tree is in.
     * @param tree The tree to process
     * @param previousState A tree state that was previously returned by this method.
     * @param callbacks Completed callbacks that have been submitted by the user. May be null.
     * @param request The request associated with the current authentication request.
     * @return The result of processing the tree.
     *
     * @throws NodeProcessException If there was a problem processing a node in the tree that could not be resolved to a
     * single result
     */
    public TreeResult process(Realm realm, AuthTree tree, TreeState previousState, List<? extends Callback> callbacks,
            ExternalRequestContext request) throws NodeProcessException {
        UUID nodeId = getNextNodeIdToProcess(tree, previousState);
        logger.debug("nextNodeId {}", nodeId);

        failIfAuthNLevelCannotBeFulfilled(tree, previousState, nodeId);

        String nodeType = tree.getNodeTypeForId(nodeId);
        logger.debug("nodeType {}", nodeType);

        Node node = nodeFactory.createNode(nodeType, nodeId, realm, tree);
        logger.debug("realm {} \n tree {}", realm.asPath(), tree.getName());

        Action action;
        try {
            TreeContext context = new TreeContext(previousState.sharedState,
                    previousState.transientState, request, callbacks);
            action = node.process(context);
        } catch (RuntimeException e) {
            logger.error("Node processing failed", e);
            throw new NodeProcessException("Node processing failed", e);
        }
        if (action == null) {
            throw new NodeProcessException("Node " + node.getClass() + " must not return null");
        }

        JsonValue newSharedState = action.sharedState != null ? action.sharedState : previousState.sharedState;
        logger.debug("newSharedState {}", newSharedState.getObject().toString());
        JsonValue newTransientState = action.transientState != null
                ? action.transientState : previousState.transientState;
        auditor.logAuditEvent(tree, action, nodeType, nodeId, newSharedState, node.getAuditEntryDetail());

        Map<String, String> newSessionProperties = addSessionProperties(previousState, action);

        List<JsonValue> newSessionHooks = addSessionHooks(previousState, action);

        List<String> newWebhooks = addWebhooks(previousState, action);

        if (action.sendingCallbacks()) {
            logger.debug("sending callbacks");
            return new TreeResult(new TreeState(newSharedState, json(object()), nodeId, newSessionProperties,
                    newSessionHooks, newWebhooks), Outcome.NEED_INPUT, action.callbacks);
        }

        UUID nextNodeId = tree.getNextNodeId(nodeId, action.outcome);
        logger.debug("nextNodeId {}", nextNodeId);
        if (nextNodeId.equals(TREE_NODE_SUCCESS_ID) || nextNodeId.equals(TREE_NODE_FAILURE_ID)) {
            Outcome outcome = Outcome.getFromNodeID(nextNodeId);
            logger.debug("Success or failure result - outcome {}", outcome);
            return new TreeResult(new TreeState(newSharedState,  json(object()), nodeId, newSessionProperties,
                    newSessionHooks, newWebhooks), outcome, emptyList());
        }
        TreeState newTreeState = new TreeState(newSharedState, newTransientState, nextNodeId, newSessionProperties,
                newSessionHooks, newWebhooks);
        logger.debug("continue processing the tree");
        return process(realm, tree, newTreeState, emptyList(), request);
    }

    private Map<String, String> addSessionProperties(TreeState previousState, Action action) {
        Map<String, String> newSessionProperties = new HashMap<>();
        newSessionProperties.putAll(previousState.sessionProperties);
        newSessionProperties.putAll(action.sessionProperties);
        newSessionProperties = unmodifiableMap(newSessionProperties);
        logger.debug("newSessionProperties {}", newSessionProperties);
        return newSessionProperties;
    }

    private List<JsonValue> addSessionHooks(TreeState previousState, Action action) {
        List<JsonValue> newSessionHooks = new ArrayList<>();
        newSessionHooks.addAll(previousState.sessionHooks);
        newSessionHooks.addAll(action.sessionHooks);
        logger.debug("newSessionHooks {}", newSessionHooks);
        return newSessionHooks;
    }

    private List<String> addWebhooks(TreeState previousState, Action action) {
        List<String> newWebhooks = new ArrayList<>();
        newWebhooks.addAll(previousState.webhooks);
        newWebhooks.addAll(action.webhooks);
        logger.debug("newWebhooks {}", newWebhooks);
        return newWebhooks;
    }

    private void failIfAuthNLevelCannotBeFulfilled(AuthTree tree, TreeState previousState,
            UUID nodeId) throws NodeProcessException {
        if (previousState.sharedState.isDefined(TARGET_AUTH_LEVEL)) {
            int targetAuthLevel = previousState.sharedState.get(TARGET_AUTH_LEVEL).asInteger();
            int currentAuthLevel = previousState.sharedState.get(AUTH_LEVEL).asInteger();
            Optional<Integer> treeMaxAuthLevel = tree.getMaxAuthLevel(nodeId);
            logger.debug("targetAuthLevel {} ; currentAuthLevel {} ; maxAuthLevel {}", targetAuthLevel,
                    currentAuthLevel, treeMaxAuthLevel.get());
            if (targetAuthLevel > currentAuthLevel + treeMaxAuthLevel.get()) {
                throw new NodeProcessException(TARGET_AUTH_LEVEL_CANNOT_BE_FULFILLED);
            }
        }
    }

    private UUID getNextNodeIdToProcess(AuthTree tree, TreeState previousState) {
        return previousState.currentNodeId == null
            ? tree.getEntryNodeId()
            : previousState.currentNodeId;
    }
}
