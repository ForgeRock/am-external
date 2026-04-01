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
package org.forgerock.openam.radius.server.spi.handlers.authentication;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.am.trees.api.Outcome;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeExecutor;
import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.am.trees.model.TreeState;
import org.forgerock.am.trees.model.TreeStateFactory;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TreeAuthenticationStrategy}.
 */
@ExtendWith(MockitoExtension.class)
class TreeAuthenticationStrategyTest {

    @Mock
    private TreeExecutor treeExecutor;

    @Mock
    private Realm realm;

    @Mock
    private Tree tree;

    @InjectMocks
    private TreeAuthenticationStrategy strategy;

    private ContextHolder createHolderWithRealm() {
        given(realm.asPath()).willReturn("/");
        return new ContextHolder("cache-key");
    }

    @Test
    void shouldStartAuthentication() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        String clientIPAddress = "127.0.0.1";
        TreeResult treeResult = new TreeResult(TreeStateFactory.createInitial(realm, null, null, 1),
                Outcome.NEED_INPUT);
        given(treeExecutor.process(eq(realm), eq(tree), any(TreeState.class),
                eq(emptyList()), eq(false), any(ExternalRequestContext.class))).willReturn(treeResult);

        // When
        boolean started = strategy.startAuthentication(holder, clientIPAddress);

        // Then
        assertThat(started).isTrue();
        assertThat(holder.getTreeResult()).isSameAs(treeResult);
        assertThat(holder.getRequestContext()).isNotNull();
    }

    @Test
    void shouldStartAuthenticationGivenNullClientIp() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeResult treeResult = new TreeResult(TreeStateFactory.createInitial(realm, null, null, 1),
                Outcome.NEED_INPUT);
        given(treeExecutor.process(eq(realm), eq(tree), any(TreeState.class),
                eq(emptyList()), eq(false), any(ExternalRequestContext.class))).willReturn(treeResult);

        // When
        boolean started = strategy.startAuthentication(holder, null);

        // Then
        assertThat(started).isTrue();
        assertThat(holder.getRequestContext()).isNotNull();
    }

    @Test
    void shouldGetNextCallbacksGivenNoCallbacks() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeState treeState = TreeStateFactory.createInitial(realm, null, null, 1);
        holder.setTreeResult(new TreeResult(treeState, Outcome.NEED_INPUT, null, null));

        // When
        Callback[] callbacks = strategy.getNextCallbacks(holder);

        // Then
        assertThat(callbacks).isEmpty();
    }

    @Test
    void shouldGetNextCallbacksGivenCallbacks() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeState treeState = TreeStateFactory.createInitial(realm, null, null, 1);
        Callback name = new NameCallback("name");
        Callback password = new PasswordCallback("password", false);
        holder.setTreeResult(new TreeResult(treeState, Outcome.NEED_INPUT, null, List.of(name, password)));

        // When
        Callback[] callbacks = strategy.getNextCallbacks(holder);

        // Then
        assertThat(callbacks).containsExactly(name, password);
    }

    @Test
    void shouldSubmitCallbacks() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeState treeState = TreeStateFactory.createInitial(realm, null, null, 1);
        TreeResult initialResult = new TreeResult(treeState, Outcome.NEED_INPUT, null, List.of());
        holder.setTreeResult(initialResult);
        holder.setRequestContext(new ExternalRequestContext.Builder().build());
        Callback[] callbacks = { new NameCallback("name") };
        TreeResult nextResult = new TreeResult(treeState, Outcome.TRUE);
        given(treeExecutor.process(eq(realm), eq(tree), eq(treeState),
                argThat(submittedCallbacks -> submittedCallbacks.equals(List.of(callbacks))),
                eq(false), any(ExternalRequestContext.class))).willReturn(nextResult);

        // When
        strategy.submitCallbacks(holder, callbacks);

        // Then
        assertThat(holder.getTreeResult()).isSameAs(nextResult);
    }

    @Test
    void shouldReturnAuthenticationSuccessful() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeState treeState = TreeStateFactory.createInitial(realm, null, null, 1);
        holder.setTreeResult(new TreeResult(treeState, Outcome.TRUE));

        // When - Then
        assertThat(strategy.isAuthenticationSuccessful(holder)).isTrue();
    }

    @Test
    void shouldNotReturnAuthenticationSuccessfulGivenFailureOutcome() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeState treeState = TreeStateFactory.createInitial(realm, null, null, 1);
        holder.setTreeResult(new TreeResult(treeState, Outcome.FALSE));

        // When - Then
        assertThat(strategy.isAuthenticationSuccessful(holder)).isFalse();
    }

    @Test
    void shouldTerminateAuthentication() {
        // Given
        ContextHolder holder = createHolderWithRealm();
        TreeState treeState = TreeStateFactory.createInitial(realm, null, null, 1);
        holder.setTreeResult(new TreeResult(treeState, Outcome.TRUE));

        // When
        boolean terminated = strategy.terminateAuthentication(holder);

        // Then
        assertThat(terminated).isTrue();
        assertThat(holder.getTreeResult()).isNull();
        assertThat(holder.getRequestContext()).isNull();
    }

    @Test
    void shouldGetUniversalIdGivenTreeState() {
        // Given
        ContextHolder holder = new ContextHolder("cache-key");
        TreeState treeState = org.mockito.Mockito.mock(TreeState.class);
        given(treeState.universalId()).willReturn(Optional.of("uid-123"));
        holder.setTreeResult(new TreeResult(treeState, Outcome.TRUE));

        // When - Then
        assertThat(strategy.getUniversalId(holder)).isEqualTo("uid-123");
    }

    @Test
    void shouldGetUniversalIdGivenNoUniversalId() {
        // Given
        ContextHolder holder = new ContextHolder("cache-key");
        TreeState treeState = org.mockito.Mockito.mock(TreeState.class);
        given(treeState.universalId()).willReturn(Optional.empty());
        holder.setTreeResult(new TreeResult(treeState, Outcome.TRUE));

        // When - Then
        assertThat(strategy.getUniversalId(holder)).isNull();
    }
}
