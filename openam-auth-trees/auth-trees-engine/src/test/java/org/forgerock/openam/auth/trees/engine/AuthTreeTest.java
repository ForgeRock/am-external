/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class AuthTreeTest {
    public static final String TREE_NAME = "treename";
    @RealmTestHelper.RealmHelper
    private static Realm realm;
    @Mock
    NodeFactory nodeFactory;
    @Mock
    NodeRegistry nodeRegistry;

    @Test
    public void getEntryNodeIdReturnsEntryNodeId() {
        UUID nodeId = UUID.randomUUID();
        AuthTree tree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId)
                .name(TREE_NAME)
                .build();
        assertThat(tree.getEntryNodeId()).isEqualTo(nodeId);
    }

    @Test
    public void getNodeTypeForIdReturnsTypeForNodeId() {
        UUID nodeId = UUID.randomUUID();
        AuthTree tree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId)
                .name(TREE_NAME)
                .node(nodeId, "type1", "display1").done()
                .build();
        assertThat(tree.getNodeTypeForId(nodeId)).isEqualTo("type1");
    }

    @Test
    public void getNextNodeIdReturnsOutputNodeForMatchingOutcome() {
        UUID nodeId = UUID.randomUUID();
        UUID nextNodeId1 = UUID.randomUUID();
        UUID nextNodeId2 = UUID.randomUUID();
        AuthTree tree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId)
                .name(TREE_NAME)
                .node(nodeId, "type1", "display1")
                .connect("outcome1", nextNodeId1)
                .connect("outcome2", nextNodeId2)
                .done()
                .build();
        assertThat(tree.getNextNodeId(nodeId, "outcome1")).isEqualTo(nextNodeId1);
        assertThat(tree.getNextNodeId(nodeId, "outcome2")).isEqualTo(nextNodeId2);
    }
}
