/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework.validators;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.util.Collections.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.core.realms.Realms;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

@Listeners(RealmTestHelper.RealmFixture.class)
public class InnerTreeConfigValidatorTest {
    private static final String INNER_TREE_TYPE = InnerTreeEvaluatorNode.class.getSimpleName();

    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private AuthTreeService authTreeService;
    @Mock
    private Debug debug;
    @Mock
    private NodeRegistry nodeRegistry;
    private InnerTreeConfigValidator validator;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        validator = new InnerTreeConfigValidator(authTreeService, debug);
        when(nodeRegistry.getNodeType(INNER_TREE_TYPE)).thenReturn((Class) InnerTreeEvaluatorNode.class);
    }

    @Test
    public void shouldSucceedIfNoInnerTreeNodes() throws Exception {
        // Given
        UUID nodeId = nodeId();
        Realm realm = Realms.root();
        makeTree("tree", TREE_NODE_SUCCESS_ID, realm);

        // When
        validator.validate(realm, singletonList(nodeId.toString()), singletonMap("tree", singleton("tree")));

        // Then - no exception
    }

    @Test(expectedExceptions = SMSException.class)
    public void shouldFailIfRefersBackToSameTree() throws Exception {
        // Given
        UUID nodeId = nodeId();
        Realm realm = Realms.root();
        makeTree("tree", nodeId, realm);

        // When
        validator.validate(realm, singletonList(nodeId.toString()), singletonMap("tree", singleton("tree")));

        // Then - should have thrown exception
        throw new Exception("Should have thrown an exception by now");
    }

    @Test(expectedExceptions = SMSException.class)
    public void shouldFailIfLoopsBackToSameTree() throws Exception {
        // Given
        UUID nodeId = nodeId();
        Realm realm = Realms.root();
        makeTree("tree1", nodeId, realm);

        UUID loopNodeId = nodeId();
        AuthTree tree = makeTree("tree2", loopNodeId, realm, builder -> {
            builder.node(loopNodeId, INNER_TREE_TYPE, "Tree1").connect("outcome", TREE_NODE_SUCCESS_ID).done();
        });

        InnerTreeEvaluatorNode loopNode = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree1"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, loopNodeId, realm, tree)).willReturn(loopNode);

        // When
        validator.validate(realm, singletonList(nodeId.toString()), singletonMap("tree", singleton("tree2")));

        // Then - should have thrown exception
        throw new Exception("Should have thrown an exception by now");
    }

    @Test
    public void shouldSucceedIfNoLoop() throws Exception {
        // Given
        UUID nodeId = nodeId();
        Realm realm = Realms.root();
        makeTree("tree1", nodeId, realm);

        UUID noLoopNodeId = nodeId();
        AuthTree tree = makeTree("tree2", noLoopNodeId, realm, builder -> {
            builder.node(noLoopNodeId, INNER_TREE_TYPE, "Tree3").connect("outcome", TREE_NODE_SUCCESS_ID).done();
        });

        makeTree("tree3", TREE_NODE_SUCCESS_ID, realm);

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree3"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, noLoopNodeId, realm, tree)).willReturn(node);


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

    @SafeVarargs
    private final AuthTree makeTree(String name, UUID entryNodeId, Realm realm,
            Consumer<AuthTree.Builder>... augmenters) {
        AuthTree.Builder builder = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm).name(name)
                .entryNodeId(entryNodeId);
        for (Consumer<AuthTree.Builder> augmenter : augmenters) {
            augmenter.accept(builder);
        }
        AuthTree tree = builder.build();
        given(authTreeService.getTree(realm, name)).willReturn(Optional.of(tree));
        return tree;
    }
}