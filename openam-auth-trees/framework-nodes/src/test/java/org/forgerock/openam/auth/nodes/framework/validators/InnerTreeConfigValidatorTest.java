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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.framework.validators;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.nodes.helpers.AuthSessionHelper;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.sm.SMSException;

@ExtendWith(MockitoExtension.class)
public class InnerTreeConfigValidatorTest {

    @Mock
    private TreeProvider authTreeService;
    @Mock
    private Realm realm;
    @Mock
    private AuthSessionHelper authSessionHelper;
    private InnerTreeConfigValidator validator;
    private Set<String> stateContainers;

    @BeforeEach
    void setup() {
        validator = new InnerTreeConfigValidator(authTreeService);
        stateContainers = Set.of();
    }

    @Test
    void shouldSucceedIfNoInnerTreeNodes() throws Exception {
        // Given
        UUID nodeId = nodeId();
        var tree = makeTree("tree", realm);
        given(tree.visitNodes(any())).willReturn(false);

        // When
        validator.validate(realm, singletonList(nodeId.toString()), singletonMap("tree", singleton("tree")));

        // Then - no exception
    }

    @Test
    void shouldFailIfRefersBackToSameTree() throws Exception {
        // Given
        UUID nodeId = nodeId();
        var tree = makeTree("tree", realm);
        given(tree.visitNodes(any())).willReturn(true);

        // When / Then
        assertThatThrownBy(() -> validator.validate(realm, singletonList(nodeId.toString()),
                singletonMap("tree", singleton("tree")))).isInstanceOf(SMSException.class);
    }

    @Test
    void shouldFailIfLoopsBackToSameTree() throws Exception {
        // Given
        UUID nodeId = nodeId();
        makeTree("tree1", realm);

        Tree tree = makeTree("tree2", realm);
        given(tree.visitNodes(any())).willReturn(true);

        InnerTreeEvaluatorNode loopNode = new InnerTreeEvaluatorNode(authTreeService, null, realm,
                nodeConfig("tree1"), stateContainers, authSessionHelper
        );
        // When
       assertThatThrownBy(() -> validator.validate(realm, singletonList(nodeId.toString()),
               singletonMap("tree", singleton("tree2")))).isInstanceOf(SMSException.class);
    }

    @Test
    void shouldSucceedIfNoLoop() throws Exception {
        // Given
        UUID nodeId = nodeId();
        makeTree("tree1", realm);

        Tree tree = makeTree("tree2", realm);
        given(tree.visitNodes(any())).willReturn(false);

        makeTree("tree3", realm);

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree3"),
                stateContainers, authSessionHelper);

        // When
        validator.validate(realm, singletonList(nodeId.toString()), singletonMap("tree", singleton("tree2")));

        // Then - no exception
    }

    private InnerTreeEvaluatorNode.Config nodeConfig(String treeName) {
        InnerTreeEvaluatorNode.Config config = mock(InnerTreeEvaluatorNode.Config.class);
        when(config.tree()).thenReturn(treeName);
        return config;
    }

    private UUID nodeId() {
        return UUID.randomUUID();
    }

    private Tree makeTree(String name, Realm realm) {
        Tree tree = Mockito.mock(Tree.class);
        lenient().when(authTreeService.getTree(realm, name)).thenAnswer(inv -> Optional.of(tree));
        lenient().when(tree.isEnabled()).thenReturn(true);
        return tree;
    }
}
