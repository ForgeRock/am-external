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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.trees;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.am.trees.api.TreeUsage;
import org.forgerock.am.trees.api.TreeUsageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;

@ExtendWith({MockitoExtension.class})
class RemoteSPTreeUsageTest {

    private static final String TREE_TO_BE_DELETED = "treeToBeDeleted";
    private static final String REALM_PATH = "/myRealm";

    @Mock
    private Realm realm;
    @Mock
    SAML2MetaManager saml2MetaManager;

    MockedStatic<SAML2Utils> saml2Utils = mockStatic(SAML2Utils.class);
    private RemoteSPTreeUsage usage;

    @BeforeEach
    void setup() {
        usage = new RemoteSPTreeUsage();
        saml2Utils.when(SAML2Utils::getSAML2MetaManager).thenReturn(saml2MetaManager);
        given(realm.asPath()).willReturn(REALM_PATH);
    }

    @AfterEach
    void teardown() {
        saml2Utils.close();
    }

    @Test
    void shouldReportNoUsagesGivenTreeNotInUseByAnyRemoteSP() throws Exception {
        // Given
        given(saml2MetaManager.getAllRemoteEntities(realm.asPath())).willReturn(List.of("remoteSPEntity1",
                "remoteSPEntity2"));
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity1"))
                .thenReturn("[Empty]");
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity2"))
                .thenReturn("[Empty]");
        // When
        TreeUsage.Response response = usage.reportUsages(realm, TREE_TO_BE_DELETED);
        // Then
        assertThat(response.inUse()).isFalse();
        assertThat(response.message()).isEqualTo("Tree is not in use by any SAML Remote SP in the realm.");
    }

    @Test
    void shouldReportUsagesGivenTreeIsInUseByOneRemoteSP() throws Exception {
        // Given
        given(saml2MetaManager.getAllRemoteEntities(realm.asPath())).willReturn(List.of("remoteSPEntity1",
                "remoteSPEntity2"));
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity1"))
                .thenReturn(TREE_TO_BE_DELETED);
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity2"))
                .thenReturn("[Empty]");
        // When
        TreeUsage.Response response = usage.reportUsages(realm, TREE_TO_BE_DELETED);
        // Then
        assertThat(response.inUse()).isTrue();
        assertThat(response.message()).isEqualTo(("1 SAML Remote SP(s)"));
        assertThat(response.appNames()).isEqualTo(List.of("remoteSPEntity1"));
    }

    @Test
    void shouldReportUsagesGivenTreeIsInUseByMoreThanThreeRemoteSPs() throws Exception {
        // Given
        given(saml2MetaManager.getAllRemoteEntities(realm.asPath())).willReturn(List.of("remoteSPEntity1",
                "remoteSPEntity2", "remoteSPEntity3", "remoteSPEntity4", "remoteSPEntity5", "remoteSPEntity6"));
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity1"))
                .thenReturn(TREE_TO_BE_DELETED);
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity2"))
                .thenReturn(TREE_TO_BE_DELETED);
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity3"))
                .thenReturn(TREE_TO_BE_DELETED);
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity4"))
                .thenReturn(TREE_TO_BE_DELETED);
        saml2Utils.when(() -> SAML2Utils.getConfiguredServiceForSP(realm.asPath(), "remoteSPEntity5"))
                .thenReturn("[Empty]");
        // When
        TreeUsage.Response response = usage.reportUsages(realm, TREE_TO_BE_DELETED);

        // Then
        assertThat(response.inUse()).isTrue();
        assertThat(response.message()).isEqualTo(("4 SAML Remote SP(s)"));
        assertThat(response.appNames()).isEqualTo(List.of("remoteSPEntity1", "remoteSPEntity2", "remoteSPEntity3"));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSAML2MetaException() throws Exception {
        given(saml2MetaManager.getAllRemoteEntities(realm.asPath()))
                .willThrow(new SAML2MetaException("Error retrieving remote entities"));

        assertThatThrownBy(() ->
                usage.reportUsages(realm, TREE_TO_BE_DELETED))
                .isInstanceOf(TreeUsageException.class)
                .hasMessage("Unable to find tree usages for the tree '%s'.".formatted(TREE_TO_BE_DELETED));
    }
}
