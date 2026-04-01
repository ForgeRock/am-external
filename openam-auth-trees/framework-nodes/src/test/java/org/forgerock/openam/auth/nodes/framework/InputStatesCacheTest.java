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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.core.realms.Realm;

import com.google.common.base.Ticker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputStatesCacheTest {

    private InputStatesCache inputStatesCache;
    private FakeTicker fakeTicker;
    private Tree tree;

    @BeforeEach
    void setUp() {
        fakeTicker = new FakeTicker();
        inputStatesCache = new InputStatesCache(fakeTicker);

        tree = mock(Tree.class);
        when(tree.getName()).thenReturn("DefaultTree");
        when(tree.getRealm()).thenReturn(toRealm("/alpha"));
    }

    @Test
    void shouldReturnCachedValueOnSecondCall() throws Exception {
        // Given
        when(tree.visitNodes(any())).thenReturn(List.of(new InputState("username", true)));

        // When
        List<InputState> firstCall = inputStatesCache.get(tree);
        List<InputState> secondCall = inputStatesCache.get(tree);

        // Then
        assertSame(firstCall, secondCall, "Should return exactly the same list object from cache");
        verify(tree, times(1)).visitNodes(any());
    }

    @Test
    void shouldExpireValueAfterFiveSeconds() throws Exception {
        // Given
        when(tree.visitNodes(any())).thenReturn(List.of(new InputState("state", true)));
        inputStatesCache.get(tree);

        // When
        fakeTicker.advance(6, TimeUnit.SECONDS);
        inputStatesCache.get(tree);

        // Then
        verify(tree, times(2)).visitNodes(any());
    }

    @Test
    void shouldDistinguishBetweenDifferentTrees() throws Exception {
        // Given
        Tree tree1Realm = createMockTree("Tree1", "Realm");
        when(tree1Realm.visitNodes(any())).thenReturn(List.of(new InputState("state")));
        Tree tree2Realm = createMockTree("Tree2", "Realm");
        when(tree2Realm.visitNodes(any())).thenReturn(List.of(new InputState("state")));

        // When
        inputStatesCache.get(tree1Realm);
        inputStatesCache.get(tree2Realm);

        // Then
        verify(tree1Realm, times(1)).visitNodes(any());
        verify(tree2Realm, times(1)).visitNodes(any());
    }

    @Test
    void shouldDistinguishBetweenDifferentRealms() throws Exception {
        // Given
        Tree treeRealm1 = createMockTree("Tree", "Realm1");
        when(treeRealm1.visitNodes(any())).thenReturn(List.of(new InputState("state", true)));
        Tree treeRealm2 = createMockTree("Tree", "Realm2");
        when(treeRealm2.visitNodes(any())).thenReturn(List.of(new InputState("state", true)));

        // When
        inputStatesCache.get(treeRealm1);
        inputStatesCache.get(treeRealm2);

        // Then
        verify(treeRealm1, times(1)).visitNodes(any());
        verify(treeRealm2, times(1)).visitNodes(any());
    }

    private Tree createMockTree(String name, String realm) {
        Tree tree = mock(Tree.class);
        when(tree.getName()).thenReturn(name);
        when(tree.getRealm()).thenReturn(toRealm(realm));
        return tree;
    }

    private Realm toRealm(String path) {
        return new Realm() {

            @Override
            public String asPath() {
                return path;
            }

            @Override
            public String asDN() {
                return "dc=" + path;
            }

            @Override
            public String asRoutingPath() {
                return path;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Realm && ((Realm) obj).asPath().equals(path);
            }

            @Override
            public int hashCode() {
                return path.hashCode();
            }
        };
    }

    static class FakeTicker extends Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        public void advance(long duration, TimeUnit unit) {
            nanos.addAndGet(unit.toNanos(duration));
        }
    }
}