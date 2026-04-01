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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.radius.server.InvalidRadiusClientConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AuthenticationStrategyFactory}.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationStrategyFactoryTest {

    @Mock
    private RealmLookup realmLookup;

    @Mock
    private TreeProvider treeProvider;

    @InjectMocks
    private AuthenticationStrategyFactory factory;

    @ParameterizedTest
    @ValueSource(strings = {"testChain", " ", ""})
    @NullSource
    void shouldCreateGivenTreeAndChainName(String chainName) throws Exception {
        // Given
        String treeName = "testTree";
        Realm realm = mock(Realm.class);
        given(realmLookup.lookup("testRealm")).willReturn(realm);
        given(treeProvider.getTree(realm, treeName)).willAnswer(inv -> Optional.of(mock(Tree.class)));

        // When
        AuthenticationStrategy strategy = factory.create("testRealm", treeName, chainName);

        // Then
        assertThat(strategy).isInstanceOf(TreeAuthenticationStrategy.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldCreateGivenChainAndTreeName(String treeName) throws Exception {
        // Given
        String chainName = "testChain";

        // When
        AuthenticationStrategy strategy = factory.create("testRealm", treeName, chainName);

        // Then
        assertThat(strategy).isInstanceOf(ChainAuthenticationStrategy.class);
    }

    @Test
    void shouldCreateRejectAllGivenNoTreeOrChainName() {
        // Given
        String treeName = " ";
        String chainName = null;

        // Then
        assertThatThrownBy(() -> factory.create("testRealm", treeName, chainName))
                .isInstanceOf(InvalidRadiusClientConfigurationException.class)
                .hasMessage("No authentication service configured.");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", ""})
    void shouldCreateRejectAllGivenMissingRealm(String realmName) {
        // Then
        assertThatThrownBy(() -> factory.create(realmName, "testTree", "testChain"))
                .isInstanceOf(InvalidRadiusClientConfigurationException.class)
                .hasMessage("Missing realm configuration.");
    }

    @Test
    void shouldCreateRejectAllGivenNullRealmName() {
        // Given
        String realmName = null;

        // Then
        assertThatThrownBy(() -> factory.create(realmName, "testTree", "testChain"))
                .isInstanceOf(InvalidRadiusClientConfigurationException.class)
                .hasMessage("Missing realm configuration.");
    }

    @Test
    void shouldCreateGivenNonExistentRealm() throws RealmLookupException {
        // Given
        String realmName = "nonExistentRealm";
        given(realmLookup.lookup(realmName)).willThrow(RealmLookupException.class);

        // Then
        assertThatThrownBy(() -> factory.create(realmName, "testTree", "testChain"))
                .isInstanceOf(InvalidRadiusClientConfigurationException.class)
                .hasMessage("Invalid tree 'testTree' in realm 'nonExistentRealm'");
    }

    @Test
    void shouldCreateGivenInvalidTree() throws RealmLookupException {
        // Given
        String realmName = "testRealm";
        String treeName = "invalidTree";
        Realm realm = mock(Realm.class);
        given(realmLookup.lookup(realmName)).willReturn(realm);
        given(treeProvider.getTree(realm, treeName)).willReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> factory.create(realmName, treeName, null))
                .isInstanceOf(InvalidRadiusClientConfigurationException.class)
                .hasMessage("Invalid tree 'invalidTree' in realm 'testRealm'");
    }

}
