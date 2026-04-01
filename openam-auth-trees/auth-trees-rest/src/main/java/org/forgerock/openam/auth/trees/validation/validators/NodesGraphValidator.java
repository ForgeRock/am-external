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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.validation.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;

/**
 * Validator to check that all node are accessible from the start.
 */
public class NodesGraphValidator implements Validator {
    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        List<ValidationFailure> failures = new ArrayList<>();
        JsonValue nodes = requestBody.get("nodes");
        String entryNodeId = requestBody.get("entryNodeId").asString();
        Set<String> nodeIds = nodes.asMap().keySet();
        if (nodeIds.isEmpty()) {
            String message = "The set of nodes is empty";
            failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            return failures;
        } else if (!nodeIds.contains(entryNodeId)) {
            String message = "'entryNodeId' '" + entryNodeId + "' cannot be found in the set of nodes";
            failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            return failures;
        }
        Map<String, Boolean> visitedNodes = new HashMap<>();
        nodeIds.forEach(nodeId -> {
            visitedNodes.put(nodeId, false);
        });
        visitNode(nodes, entryNodeId, visitedNodes);
        visitedNodes.forEach((nodeId, value) -> {
            if (!visitedNodes.get(nodeId)) {
                failures.add(new ValidationFailure("Node '" + nodeId + "' is not reachable from the start",
                        ValidationFailure.Severity.WARNING));
            }
        });
        return failures;
    }

    private void visitNode(JsonValue nodes, String nodeId, Map<String, Boolean> visitedNodes) {
        visitedNodes.put(nodeId, true);
        JsonValue node = nodes.get(nodeId);
        Map<String, Object> connections = node.get("connections").asMap();
        for (Object connectionNodeId : connections.values()) {
            if (visitedNodes.containsKey((String) connectionNodeId)) {
                if (!visitedNodes.get((String) connectionNodeId)) {
                    visitNode(nodes, (String) connectionNodeId, visitedNodes);
                }
            }
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
