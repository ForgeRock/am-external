/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework.validators;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper.RealmFixture;
import org.forgerock.openam.core.realms.Realms;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

@Listeners(RealmFixture.class)
public class NodeConfigValidatorTest {
    private static final String INNER_TREE_TYPE = InnerTreeEvaluatorNode.class.getSimpleName();

    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private AuthTreeService authTreeService;
    @Mock
    private Debug debug;

    private NodeConfigValidator validator;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class)).thenReturn(INNER_TREE_TYPE);
        when(nodeRegistry.getNodeType(INNER_TREE_TYPE)).thenReturn((Class) InnerTreeEvaluatorNode.class);
        validator = new NodeConfigValidator(nodeFactory, nodeRegistry, authTreeService, debug);
    }

    @Test
    public void shouldSucceedIfNoInnerTreeNodes() throws Exception {
        // When
        validator.validate(Realms.root(), configPath("tree", nodeId()), details("otherNodeType", "Other Node"));

        // Then - no exception
    }

    @Test(expectedExceptions = SMSException.class)
    public void shouldFailIfRefersBackToSameTree() throws Exception {
        // Given
        UUID nodeId = nodeId();
        Realm realm = Realms.root();
        AuthTree tree = makeTree("tree", nodeId, realm);
        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, nodeId, realm, tree)).willReturn(node);

        // When
        validator.validate(realm, configPath("tree", nodeId), details(INNER_TREE_TYPE, "Tree"));

        // Then - exception
        throw new Exception("should have thrown an exception by now");
    }

    @Test(expectedExceptions = SMSException.class)
    public void shouldFailIfLoopsBackToSameTree() throws Exception {
        // Given
        Realm realm = Realms.root();

        UUID node1Id = nodeId();
        AuthTree tree1 = makeTree("tree1", node1Id, realm);

        UUID node2Id = nodeId();
        AuthTree tree2 = makeTree("tree2", node2Id, realm, (builder) -> {
            builder.node(node2Id, INNER_TREE_TYPE, "Tree1").connect("outcome", TREE_NODE_SUCCESS_ID).done();
        });

        InnerTreeEvaluatorNode node1 = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree2"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, node1Id, realm, tree1)).willReturn(node1);
        InnerTreeEvaluatorNode node2 = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree1"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, node2Id, realm, tree2)).willReturn(node2);

        // When
        validator.validate(realm, configPath("tree1", node1Id), details(INNER_TREE_TYPE, "Tree2"));

        // Then - exception
        throw new Exception("should have thrown an exception by now");
    }

    @Test
    public void shouldSucceedIfNoLoop() throws Exception {
        // Given
        Realm realm = Realms.root();

        UUID node1Id = nodeId();
        AuthTree tree1 = makeTree("tree1", node1Id, realm);

        UUID node2Id = nodeId();
        AuthTree tree2 = makeTree("tree2", node2Id, realm, (builder) -> {
            builder.node(node2Id, INNER_TREE_TYPE, "Tree3").connect("outcome", TREE_NODE_SUCCESS_ID).done();
        });

        makeTree("tree3", TREE_NODE_SUCCESS_ID, realm);

        InnerTreeEvaluatorNode node1 = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree2"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, node1Id, realm, tree1)).willReturn(node1);
        InnerTreeEvaluatorNode node2 = new InnerTreeEvaluatorNode(authTreeService, null, realm, nodeConfig("tree3"));
        given(nodeFactory.createNode(INNER_TREE_TYPE, node2Id, realm, tree2)).willReturn(node2);

        // When
        validator.validate(realm, configPath("tree1", node1Id), details(INNER_TREE_TYPE, "Tree2"));

        // Then - no exception
    }

    private List<String> configPath(String treeName, UUID nodeId) {
        return asList("treesContainer", treeName, nodeId.toString());
    }

    private InnerTreeEvaluatorNode.Config nodeConfig(String treeName) {
        InnerTreeEvaluatorNode.Config config = mock(InnerTreeEvaluatorNode.Config.class);
        when(config.tree()).thenReturn(treeName);
        return config;
    }

    private Map<String, Set<String>> details(String nodeType, String displayName) {
        return ImmutableMap.of("nodeType", singleton(nodeType), "displayName", singleton(displayName));
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