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
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import java.util.Optional;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.service.AMAccountLockoutTrees;
import com.sun.identity.idm.AMIdentity;

@ExtendWith(MockitoExtension.class)
public class AccountLockoutNodeTest {

    @Mock
    AMIdentity userIdentity;

    @Mock
    AMAccountLockoutTrees.Factory amAccountLockoutFactory;

    @Mock
    AMAccountLockoutTrees amAccountLockout;

    @Mock
    Realm realm;

    @Mock
    AccountLockoutNode.Config config;

    @Mock
    NodeUserIdentityProvider identityProvider;

    @BeforeEach
    void before() throws Exception {
        lenient().when(config.lockAction()).thenReturn(AccountLockoutNode.LockStatus.LOCK);
        lenient().when(identityProvider.getAMIdentity(any(), any())).thenReturn(Optional.of(userIdentity));
        lenient().when(amAccountLockoutFactory.create(any())).thenReturn(amAccountLockout);
    }

    @Test
    void testNodeCallsAMToLockAccount() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(amAccountLockoutFactory, config, identityProvider, realm);

        // When
        node.process(newTreeContext(json(object(field(REALM, "/"), field(USERNAME, "username")))));

        // Then verify setActiveStatus gets called
        Mockito.verify(amAccountLockout).setUserAccountActiveStatus(eq(userIdentity), eq(false));
    }

    @Test
    void testNodeCallsAMToUnLockAccount() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(amAccountLockoutFactory,
                new AccountLockoutNode.Config() {
            @Override
            public AccountLockoutNode.LockStatus lockAction() {
                return AccountLockoutNode.LockStatus.UNLOCK;
            }
        },  identityProvider, realm);

        // When
        node.process(newTreeContext(json(object(field(REALM, "/"), field(USERNAME, "username")))));

        // Then verify setActiveStatus gets called
        Mockito.verify(amAccountLockout).setUserAccountActiveStatus(eq(userIdentity), eq(true));
    }

    @Test
    void testNullUsernameWillThrowNodeProcessException() {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(amAccountLockoutFactory, config, identityProvider, realm);

        // When username is null
        assertThatThrownBy(() -> node.process(newTreeContext(json(object(field(REALM, "/"))))))
                .isInstanceOf(NodeProcessException.class);
    }

}
