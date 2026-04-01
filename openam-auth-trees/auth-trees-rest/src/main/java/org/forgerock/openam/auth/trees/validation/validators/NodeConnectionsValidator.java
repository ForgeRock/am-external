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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator to check that all node connections are valid. This validator assumes already that the node type and
 * version exist.
 */
public class NodeConnectionsValidator implements Validator {
    private static final Logger logger = LoggerFactory.getLogger(NodeConnectionsValidator.class);
    private final NodeFactory nodeFactory;
    private final RealmLookup realmLookup;

    @Inject
    NodeConnectionsValidator(NodeFactory nodeFactory, RealmLookup realmLookup) {
        this.nodeFactory = nodeFactory;
        this.realmLookup = realmLookup;
    }

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        List<ValidationFailure> failures = new ArrayList<>();
        JsonValue nodes = requestBody.get("nodes");
        nodes.asMap().keySet().forEach(nodeId -> {
            JsonValue node = nodes.get(nodeId);
            Map<String, String> connections = node.get("connections").asMap(String.class);
            checkAllConnectionsExist(nodeId, node, connections.keySet(), failures);
            checkAllConnectionsConnected(nodeId, connections, nodes, failures);
        });
        return failures;
    }

    private void checkAllConnectionsExist(String nodeId, JsonValue node, Set<String> connectionKeys,
            List<ValidationFailure> failures) {
        try {
            OutcomeProvider outcomeProvider = nodeFactory.getOutcomeProvider(realmLookup.lookup("/"),
                    node.get("nodeType").asString(), node.get("version").defaultTo(1).asInteger());
            if (!(outcomeProvider instanceof StaticOutcomeProvider)
                        && !node.isDefined("config")) {
                // Dynamic outcomes, cannot validate
                logger.debug("Node '{}' has dynamic outcomes and no config, skipping outcome validation.", nodeId);
                return;
            }
            Set<String> outcomeSet = getNodeOutcomes(outcomeProvider, node.get("config").defaultTo(json(object())));

            // Find missing outcomes (expected but not present in connections)
            Set<String> missingOutcomes = new HashSet<>(outcomeSet);
            missingOutcomes.removeAll(connectionKeys);
            for (String missingOutcome : missingOutcomes) {
                String message = "Outcome '" + missingOutcome + "' for node '" + nodeId + "' is missing";
                failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            }

            // Find unknown outcomes (present in connections but not expected)
            Set<String> unknownOutcomes = new HashSet<>(connectionKeys);
            unknownOutcomes.removeAll(outcomeSet);
            for (String unknownOutcome : unknownOutcomes) {
                String message = "Unknown outcome '" + unknownOutcome + "' for node '" + nodeId + "'";
                failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            }
        } catch (NodeProcessException | RealmLookupException e) {
            logger.warn("Failed to get outcome provider for node type.", e);
            failures.add(new ValidationFailure("Failed to get outcome provider for node type",
                    ValidationFailure.Severity.ERROR));
        }
    }

    private Set<String> getNodeOutcomes(OutcomeProvider outcomeProvider, JsonValue nodeConfig)
            throws NodeProcessException {
        PreferredLocales locales = new PreferredLocales();
        return outcomeProvider.getOutcomes(locales, nodeConfig).stream()
                       .map(o -> o.id).collect(Collectors.toSet());

    }

    private static void checkAllConnectionsConnected(String nodeId, Map<String, String> connections, JsonValue nodes,
            List<ValidationFailure> failures) {
        Set<String> nodeIds = new HashSet<>(nodes.asMap().keySet());
        nodeIds.add(TREE_NODE_SUCCESS_ID.toString());
        nodeIds.add(TREE_NODE_FAILURE_ID.toString());
        connections.forEach((connectionOutcomeId, connectionNodeId) -> {
            if (connectionNodeId == null || connectionNodeId.isEmpty()) {
                String message = "Outcome '%s' for node '%s' is not connected"
                                         .formatted(connectionOutcomeId, nodeId);
                failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            } else if (!nodeIds.contains(connectionNodeId)) {
                String message = "Connection '%s : %s' for node '%s' leads to undefined node"
                                         .formatted(connectionOutcomeId, connectionNodeId, nodeId);
                failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            }
        });
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
