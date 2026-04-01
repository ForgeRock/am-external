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

import static org.forgerock.am.trees.api.Outcome.TREE_NODE_FAILURE_ID;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_SUCCESS_ID;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;

import com.google.common.collect.Streams;

/**
 * Validator to ensure that reserved node IDs are not used in the tree definition.
 */
public class NodeIdValidator implements Validator {

    private static final Set<String> RESERVED_STATIC_NODES_IDS = Set.of("startNode", TREE_NODE_SUCCESS_ID.toString(),
            TREE_NODE_FAILURE_ID.toString());

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        return Streams.concat(
                validateNodes(requestBody),
                validateStaticNodes(requestBody)
        ).toList();
    }

    private Stream<ValidationFailure> validateNodes(JsonValue requestBody) {
        JsonValue nodes = requestBody.get("nodes");
        if (nodes.isNotNull()) {
            var nodeIds = nodes.asMap().keySet();
            return nodeIds.stream()
                    .filter(RESERVED_STATIC_NODES_IDS::contains)
                    .map(nodeId -> {
                        String message = "Node id '" + nodeId + "' is reserved and cannot be used for a new node";
                        return new ValidationFailure(message, ValidationFailure.Severity.WARNING);
                    });
        } else {
            return Stream.empty();
        }
    }

    private Stream<ValidationFailure> validateStaticNodes(JsonValue requestBody) {
        JsonValue staticNodes = requestBody.get("staticNodes");
        if (staticNodes.isNotNull()) {
            var staticNodeIds = staticNodes.asMap().keySet();
            return staticNodeIds.stream()
                    .filter(staticNodeId -> !RESERVED_STATIC_NODES_IDS.contains(staticNodeId))
                    .map(nodeId -> {
                        String message = "Static node id '" + nodeId
                                + "' is invalid and cannot be used for a static node";
                        return new ValidationFailure(message, ValidationFailure.Severity.WARNING);
                    });
        } else  {
            return Stream.empty();
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
