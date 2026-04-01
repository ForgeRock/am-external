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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_FAILURE_ID;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_SUCCESS_ID;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.WARNING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodeConnectionsValidatorTest {

    @Mock
    private StaticOutcomeProvider staticOutcomeProvider;
    @Mock
    private BoundedOutcomeProvider boundedOutcomeProvider;
    @Mock
    private Realm root;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private NodeFactory nodeFactory;

    @InjectMocks
    private NodeConnectionsValidator treeNodesValidator;

    @BeforeEach
    void setUp() throws RealmLookupException {
        given(realmLookup.lookup("/")).willReturn(root);
    }

    @Test
    void shouldReturnWarningGivenStaticConnectionLeadsToUndefinedNode() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(staticOutcomeProvider);
        given(staticOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("outcome1", "outcome1")));
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId2")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).containsExactly(new ValidationFailure(
                "Connection 'outcome1 : nodeId2' for node 'nodeId1' leads to undefined node", WARNING));
    }

    @Test
    void shouldReturnNoFailuresForValidTree() throws NodeProcessException {
        StaticOutcomeProvider staticOutcomeProvider2 = mock(StaticOutcomeProvider.class);
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(staticOutcomeProvider);
        given(nodeFactory.getOutcomeProvider(root, "nodeType2", 1)).willReturn(staticOutcomeProvider2);
        given(staticOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("outcome1", "outcome1")));
        given(staticOutcomeProvider2.getOutcomes(any(), any())).willReturn(List.of());

        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId2")
                        ))
                )),
                field("nodeId2", object(
                        field("displayName", "displayName2"),
                        field("nodeType", "nodeType2"),
                        field("connections", object())
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNoFailuresForConnectionsToSuccessAndFailure() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(staticOutcomeProvider);
        given(staticOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("outcome_success", "outcome_success"),
                        new OutcomeProvider.Outcome("outcome_failure", "outcome_failure")));
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome_success", TREE_NODE_SUCCESS_ID.toString()),
                                field("outcome_failure", TREE_NODE_FAILURE_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnFailuresForEmptyConnection() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(staticOutcomeProvider);
        given(staticOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("outcome1", "outcome1")));
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).containsExactly(new ValidationFailure("Outcome 'outcome1' for node "
                                                                 + "'nodeId1' is not connected", WARNING));
    }

    @Test
    void shouldReturnWarningWhenOutcomeIsMissing() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(staticOutcomeProvider);
        given(staticOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("outcome1", "outcome1"),
                        new OutcomeProvider.Outcome("outcome2", "outcome2")));
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).containsExactly(new ValidationFailure(
                "Outcome 'outcome2' for node 'nodeId1' is missing", WARNING));
    }

    @Test
    void shouldReturnWarningWhenOutcomeIsUnknown() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(staticOutcomeProvider);
        given(staticOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("outcome1", "outcome1")));
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_FAILURE_ID.toString()),
                                field("unknown_outcome", TREE_NODE_SUCCESS_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).containsExactly(new ValidationFailure(
                "Unknown outcome 'unknown_outcome' for node 'nodeId1'", WARNING));
    }

    @Test
    void shouldReturnErrorWhenOutcomeProviderThrowsException() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willThrow(new NodeProcessException("Error"));
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId2")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).contains(new ValidationFailure(
                "Failed to get outcome provider for node type", ERROR));
    }

    @Test
    void shouldNotValidateOutcomesForDynamicProviderAndNoConfig() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(boundedOutcomeProvider);
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_FAILURE_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldValidateOutcomesForDynamicProviderWithConfig() throws NodeProcessException {
        given(nodeFactory.getOutcomeProvider(root, "nodeType1", 1)).willReturn(boundedOutcomeProvider);
        JsonValue config = json(object(field("outcomes", List.of("dynamic_outcome"))));
        given(boundedOutcomeProvider.getOutcomes(any(), any())).willReturn(
                List.of(new OutcomeProvider.Outcome("dynamic_outcome", "dynamic_outcome")));

        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("config", config),
                        field("connections", object(
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = treeNodesValidator.validate(requestBody, null);
        assertThat(result).containsExactly(new ValidationFailure(
                "Outcome 'dynamic_outcome' for node 'nodeId1' is missing", WARNING));
    }

}