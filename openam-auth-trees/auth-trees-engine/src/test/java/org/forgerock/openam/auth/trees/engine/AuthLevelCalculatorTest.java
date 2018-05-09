package org.forgerock.openam.auth.trees.engine;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.nodes.framework.ModifyAuthLevelNode;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuthLevelCalculatorTest {

    private static final String AUTH_LEVEL_NODE_TYPE = ModifyAuthLevelNode.class.getSimpleName();
    private static final String TREE_NAME = "treename";

    @Mock
    Realm realm;

    @Mock
    NodeFactory nodeFactory;

    @Mock
    NodeRegistry nodeRegistry;

    @BeforeMethod
    public void before() {
        initMocks(this);
        when(nodeRegistry.getNodeType(AUTH_LEVEL_NODE_TYPE)).thenReturn((Class) ModifyAuthLevelNode.class);
    }

    @Test
    public void treeWithOneCollectorNode() throws Exception {
        UUID nodeId = mockNode();

        given(nodeRegistry.getNodeType("MyNode")).willReturn((Class) Node.class);

        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(nodeId)
            .name(TREE_NAME)
            .node(nodeId, "MyNode", "My Node")
            .connect("outcome", TREE_NODE_SUCCESS_ID).done()
            .build();

        assertThat(authTree.getMaxAuthLevel().get()).isEqualTo(0);
    }

    @Test
    public void treeWithTwoNodesAndNoAuthLevel() throws Exception {
        UUID nodeIdOne = mockNode();
        UUID nodeIdTwo = mockNode();

        given(nodeRegistry.getNodeType("MyNode")).willReturn((Class) Node.class);
        given(nodeRegistry.getNodeType("OtherNode")).willReturn((Class) Node.class);

        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(nodeIdOne)
            .name(TREE_NAME)
            .node(nodeIdOne, "MyNode", "My Node")
            .connect("outcome", nodeIdTwo).done()
            .node(nodeIdTwo, "OtherNode", "Other Node")
            .connect("outcome", TREE_NODE_SUCCESS_ID).done()
            .build();

        assertThat(authTree.getMaxAuthLevel().get()).isEqualTo(0);
    }

    @Test
    public void treeWithUsernameCollectorNodeAndModifyAuthLevelNode() throws Exception {
        UUID aNodeId = mockNode();
        UUID modifyAuthLevelNodeId = mockModifyAuthLevel(7);

        given(nodeRegistry.getNodeType("MyNode")).willReturn((Class) Node.class);

        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(aNodeId)
            .name(TREE_NAME)
            .node(aNodeId, "MyNode", "My Node")
            .connect("outcome", modifyAuthLevelNodeId).done()
            .node(modifyAuthLevelNodeId, AUTH_LEVEL_NODE_TYPE, "Modify Auth Level")
            .connect("outcome", TREE_NODE_SUCCESS_ID).done()
            .build();

        assertThat(authTree.getMaxAuthLevel().get()).isEqualTo(7);
    }

    @Test
    public void treeWithChoiceCollectorAndFourModifyAuthLevels() throws Exception {
        UUID multiOutcomeNodeId = mockNode();
        UUID modifyAuthLevelNodeId1 = mockModifyAuthLevel(1);
        UUID modifyAuthLevelNodeId2 = mockModifyAuthLevel(4);
        UUID modifyAuthLevelNodeId3 = mockModifyAuthLevel(2);
        UUID modifyAuthLevelNodeId4 = mockModifyAuthLevel(3);

        given(nodeRegistry.getNodeType("MultiOutcomeNode")).willReturn((Class) Node.class);

        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(multiOutcomeNodeId)
            .name(TREE_NAME)
            .node(multiOutcomeNodeId, "MultiOutcomeNode", "Multi Outcome Node")
            .connect("1", modifyAuthLevelNodeId1)
            .connect("2", modifyAuthLevelNodeId2)
            .connect("3", modifyAuthLevelNodeId3)
            .connect("4", modifyAuthLevelNodeId4).done()
            .node(modifyAuthLevelNodeId1, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", TREE_NODE_SUCCESS_ID).done()
            .node(modifyAuthLevelNodeId2, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", TREE_NODE_FAILURE_ID).done()
            .node(modifyAuthLevelNodeId3, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", TREE_NODE_SUCCESS_ID).done()
            .node(modifyAuthLevelNodeId4, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", TREE_NODE_FAILURE_ID).done()
            .build();

        assertThat(authTree.getMaxAuthLevel().get()).isEqualTo(2);
    }

    @Test
    public void treeWithBranchesWhichGrowTogether() throws Exception {
        UUID multiOutcomeNodeId = mockNode();
        UUID modifyAuthLevelNodeId1 = mockModifyAuthLevel(1);
        UUID modifyAuthLevelNodeId2 = mockModifyAuthLevel(2);
        UUID otherNodeId = mockNode();

        given(nodeRegistry.getNodeType("MultiOutcomeNode")).willReturn((Class) Node.class);
        given(nodeRegistry.getNodeType("DecisionNode")).willReturn((Class) Node.class);

        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(multiOutcomeNodeId)
            .name(TREE_NAME)
            .node(multiOutcomeNodeId, "MultiOutcomeNode", "Multi Outcome Node")
            .connect("1", modifyAuthLevelNodeId1)
            .connect("2", modifyAuthLevelNodeId2).done()
            .node(modifyAuthLevelNodeId1, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", otherNodeId).done()
            .node(modifyAuthLevelNodeId2, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", otherNodeId).done()
            .node(otherNodeId, "DecisionNode", "Other Node")
            .connect("true", TREE_NODE_SUCCESS_ID)
            .connect("false", TREE_NODE_FAILURE_ID).done()
            .build();

        assertThat(authTree.getMaxAuthLevel().get()).isEqualTo(2);
    }

    @Test
    public void treeWithCycle() throws Exception {
        UUID node = mockNode();

        given(nodeRegistry.getNodeType("MyNode")).willReturn((Class) Node.class);

        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(node)
            .name(TREE_NAME)
            .node(node, "MyNode", "My Node")
            .connect("true", TREE_NODE_SUCCESS_ID)
            .connect("false", node).done()
            .build();

        assertThat(authTree.getMaxAuthLevel().get()).isEqualTo(0);
    }

    @Test
    public void treeWithinTree() throws Exception {
        UUID modifyAuthLevelNodeId = mockModifyAuthLevel(1);

        given(nodeRegistry.getNodeType(InnerTreeEvaluatorNode.class.getSimpleName()))
                .willReturn((Class) InnerTreeEvaluatorNode.class);

        AuthTree innerTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .name(TREE_NAME)
            .entryNodeId(modifyAuthLevelNodeId)
            .node(modifyAuthLevelNodeId, AUTH_LEVEL_NODE_TYPE, "modifyAuthLevel")
            .connect("outcome", TREE_NODE_SUCCESS_ID).done()
            .build();

        UUID innerTreeEvaluatorId = mockInnerTreeEvaluator(innerTree);

        AuthTree outerTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .name(TREE_NAME)
            .entryNodeId(innerTreeEvaluatorId)
            .node(innerTreeEvaluatorId, InnerTreeEvaluatorNode.class.getSimpleName(), "innerTreeEvaluator")
            .connect("true", TREE_NODE_SUCCESS_ID)
            .connect("false", TREE_NODE_FAILURE_ID).done()
            .build();

        assertThat(outerTree.getMaxAuthLevel().get()).isEqualTo(1);
    }

    private UUID mockInnerTreeEvaluator(AuthTree innerTree) throws Exception {
        InnerTreeEvaluatorNode node = mock(InnerTreeEvaluatorNode.class);
        when(node.getTree()).thenReturn(innerTree);
        return mockNode(node);
    }

    private UUID mockModifyAuthLevel(int authLevelIncrement) throws Exception {
        ModifyAuthLevelNode node = mock(ModifyAuthLevelNode.class);
        when(node.getAuthLevelIncrement()).thenReturn(authLevelIncrement);
        return mockNode(node);
    }

    private UUID mockNode() throws Exception {
        Node node = mock(Node.class);
        return mockNode(node);
    }

    private UUID mockNode(Node node) throws Exception {
        UUID nodeId = UUID.randomUUID();
        when(nodeFactory.createNode(anyString(), eq(nodeId), eq(realm), anyObject())).thenReturn(node);
        return nodeId;
    }
}
