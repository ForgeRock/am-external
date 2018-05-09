/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.TARGET_AUTH_LEVEL;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.ChoiceCollectorNode;
import org.forgerock.openam.auth.nodes.framework.ChoiceCollectorNode.Config;
import org.forgerock.openam.auth.nodes.framework.ModifyAuthLevelNode;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.util.Pair;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class ChoiceCollectorNodeTest {

    Config config;

    @Mock
    NodeFactory nodeFactory;
    @Mock
    NodeRegistry nodeRegistry;
    @RealmTestHelper.RealmHelper
    static Realm realm;

    ChoiceCollectorNode choiceCollectorNode;

    @BeforeMethod
    public void before() throws NodeProcessException {
        initMocks(this);
        config = whenNodeConfigHasAttributes(2);
    }

    @DataProvider(name = "choices")
    public Object[][] choices() {
        return new Object[][] {
            { 0, "choice0" },
            { 1, "choice1" }
        };
    }

    @Test(dataProvider = "choices")
    public void shouldGetCorrectOutcomeForChoiceIndex(int index, String choice) throws Exception {
        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"}, 0, false);
        choiceCallback.setSelectedIndex(index);

        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback)));

        assertThat(action.outcome).isEqualTo(choice);
    }

    @Test
    public void testProcessWithChoiceCallbackNotSelectedIndexReturnsInitialCallback() throws Exception {
        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"}, 0, false);

        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback)));

        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isEqualToComparingFieldByField(
            new ChoiceCallback("prompt", new String[]{"choice0", "choice1"}, 1, false));
    }

    @Test
    public void testProcessWithNoCallbacksReturnsInitialCallback() throws Exception {
        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(emptyList()));

        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isEqualToComparingFieldByField(
            new ChoiceCallback("prompt", new String[]{"choice0", "choice1"}, 1, false));
    }

    @Test
    public void testOutcomesInCallbackFilteredByAuthLevel() throws Exception {
        // given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        AuthTree tree = tree(choiceCollectorNodeId,
            Pair.of(mockModifyAuthLevel(4), TREE_NODE_SUCCESS_ID),
            Pair.of(mockModifyAuthLevel(5), TREE_NODE_SUCCESS_ID),
            Pair.of(mockModifyAuthLevel(6), TREE_NODE_SUCCESS_ID));

        Config config = whenNodeConfigHasAttributes(3);

        choiceCollectorNode = new ChoiceCollectorNode(config, tree, choiceCollectorNodeId);

        // when
        Action action = choiceCollectorNode.process(getContext(emptyList(), json(object(
                field(AUTH_LEVEL, 5),
                field(TARGET_AUTH_LEVEL, 10)))));

        // then
        ChoiceCallback callback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(callback.getChoices()).isEqualTo(new String[]{"choice1", "choice2"});
        assertThat(callback.getDefaultChoice()).isEqualTo(0);
    }

    @Test
    public void testOutcomesInCallbackFilteredByLeafNode() throws Exception {
        // given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        AuthTree tree = tree(choiceCollectorNodeId,
            Pair.of(mockModifyAuthLevel(1), TREE_NODE_FAILURE_ID),
            Pair.of(mockModifyAuthLevel(1), TREE_NODE_SUCCESS_ID),
            Pair.of(mockModifyAuthLevel(1), TREE_NODE_SUCCESS_ID));

        Config config = whenNodeConfigHasAttributes(3);

        choiceCollectorNode = new ChoiceCollectorNode(config, tree, choiceCollectorNodeId);

        // when
        Action action = choiceCollectorNode.process(getContext(emptyList(), json(object(
                field("authLevel", 11),
                field("targetAuthLevel", 10)))));

        // then
        ChoiceCallback callback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(callback.getChoices()).isEqualTo(new String[]{"choice1", "choice2"});
        assertThat(callback.getDefaultChoice()).isEqualTo(0);
    }

    @Test
    public void testOnlyOneOutcomeLeftIsImmediatelyReturnedWithoutCallback() throws Exception {
        //given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        AuthTree tree = tree(choiceCollectorNodeId,
            Pair.of(mockModifyAuthLevel(4), TREE_NODE_SUCCESS_ID),
            Pair.of(mockModifyAuthLevel(6), TREE_NODE_SUCCESS_ID));

        choiceCollectorNode = new ChoiceCollectorNode(config, tree, choiceCollectorNodeId);

        // when
        Action action = choiceCollectorNode.process(getContext(emptyList(), json(object(
                field("authLevel", 5),
                field("targetAuthLevel", 10)))));

        //then
        assertThat(action.outcome).isEqualTo("choice1");
        assertThat(action.callbacks).isEmpty();
    }

    @Test
    public void testReturnedOutcomeFilteredByAuthLevel() throws Exception {
        //when
        UUID choiceCollectorNodeId = UUID.randomUUID();

        AuthTree tree = tree(choiceCollectorNodeId,
            Pair.of(mockModifyAuthLevel(4), TREE_NODE_SUCCESS_ID),
            Pair.of(mockModifyAuthLevel(5), TREE_NODE_SUCCESS_ID),
            Pair.of(mockModifyAuthLevel(6), TREE_NODE_SUCCESS_ID));

        Config config = whenNodeConfigHasAttributes(3);

        choiceCollectorNode = new ChoiceCollectorNode(config, tree, choiceCollectorNodeId);

        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice1", "choice2"}, 0, false);
        choiceCallback.setSelectedIndex(0);

        // given
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback), json(object(
                field(AUTH_LEVEL, 5),
                field(TARGET_AUTH_LEVEL, 10)))));

        // then
        assertThat(action.outcome).isEqualTo("choice1");
    }

    private UUID mockModifyAuthLevel(int authLevelIncrement) throws Exception {
        ModifyAuthLevelNode node = mock(ModifyAuthLevelNode.class);
        when(node.getAuthLevelIncrement()).thenReturn(authLevelIncrement);
        UUID nodeId = UUID.randomUUID();
        Class<? extends Node> modifyAuthLevelNodeClass = ModifyAuthLevelNode.class;
        when(nodeRegistry.getNodeType("ModifyAuthLevelNode")).thenReturn((Class) modifyAuthLevelNodeClass);
        when(nodeFactory.createNode(eq("ModifyAuthLevelNode"), eq(nodeId), eq(realm), anyObject())).thenReturn(node);
        return nodeId;
    }

    private Config whenNodeConfigHasAttributes(int numberOfChoices) {
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < numberOfChoices; i++) {
            choices.add("choice" + i);
        }
        config = mock(Config.class);
        given(config.choices()).willReturn(choices);
        given(config.defaultChoice()).willReturn("choice1");
        given(config.prompt()).willReturn("prompt");
        return config;
    }

    @SafeVarargs
    private final AuthTree tree(UUID choiceCollectorNodeId, Pair<UUID, UUID>... connections) throws Exception {
        AuthTree.ConnectionBuilder connectionBuilder = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
            .entryNodeId(choiceCollectorNodeId)
            .name("Tree")
            .node(choiceCollectorNodeId, "ChoiceCollectorNode", "choiceCollector");
        int i = 0;
        for (Pair<UUID, UUID> connection : connections) {
            connectionBuilder.connect("choice" + i++, connection.getFirst());
        }
        AuthTree.Builder treeBuilder = connectionBuilder.done();
        for (Pair<UUID, UUID> connection : connections) {
            treeBuilder
                .node(connection.getFirst(), "ModifyAuthLevelNode", "modifyAuthLevel")
                .connect("outcome", connection.getSecond());
        }
        return treeBuilder.build();
    }

    private TreeContext getContext(List<? extends Callback> callbacks) {
        return getContext(callbacks, json(object()));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), callbacks);
    }
}
