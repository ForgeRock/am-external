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
 * Copyright 2017-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.service.AMAccountLockout;
import com.sun.identity.idm.AMIdentity;

public class AccountLockoutNodeTest {

    @Mock
    AMIdentity userIdentity;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    IdentityUtils identityUtils;

    @Mock
    AMAccountLockout.Factory amAccountLockoutFactory;

    @Mock
    AMAccountLockout amAccountLockout;

    @Mock
    Realm realm;

    @Mock
    AccountLockoutNode.Config config;

    @BeforeMethod
    public void before() throws Exception {
        initMocks(this);
        when(config.lockAction()).thenReturn(AccountLockoutNode.LockStatus.LOCK);
        given(coreWrapper.getIdentity(anyString())).willReturn(userIdentity);
        given(coreWrapper.realmOf(anyString())).willReturn(realm);
        given(amAccountLockoutFactory.create(any())).willReturn(amAccountLockout);
        when(coreWrapper.getIdentity(anyString(), anyString())).thenReturn(userIdentity);
    }

    @Test
    public void testNodeCallsAMToLockAccount() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(coreWrapper, identityUtils, amAccountLockoutFactory, config);
        BDDMockito.willDoNothing().given(userIdentity).setActiveStatus(false);

        // When
        node.process(newTreeContext(json(object(field(REALM, "/"), field(USERNAME, "username")))));

        // Then verify setActiveStatus gets called
        Mockito.verify(amAccountLockout).setUserAccountActiveStatus(eq(userIdentity), eq(false));
    }

    @Test
    public void testNodeCallsAMToUnLockAccount() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(coreWrapper, identityUtils, amAccountLockoutFactory,
                new AccountLockoutNode.Config() {
            @Override
            public AccountLockoutNode.LockStatus lockAction() {
                return AccountLockoutNode.LockStatus.UNLOCK;
            }
        });

        // When
        node.process(newTreeContext(json(object(field(REALM, "/"), field(USERNAME, "username")))));

        // Then verify setActiveStatus gets called
        Mockito.verify(amAccountLockout).setUserAccountActiveStatus(eq(userIdentity), eq(true));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void testNullUsernameWillThrowNodeProcessException() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(coreWrapper, identityUtils, amAccountLockoutFactory, config);

        // When username is null
        node.process(newTreeContext(json(object(field(REALM, "/")))));

        // Then -> Throw NodeProcessException
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void testNullRealmWillThrowNodeProcessException() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(coreWrapper, identityUtils,  amAccountLockoutFactory, config);

        // When realm is null
        node.process(newTreeContext(json(object(field(USERNAME, "username")))));

        // Then -> Throw NodeProcessException

    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void testRealmLookupExceptionIsHandled() throws Exception {
        // Given
        AccountLockoutNode node = new AccountLockoutNode(coreWrapper, identityUtils, amAccountLockoutFactory, config);
        given(coreWrapper.realmOf(anyString())).willThrow(new RealmLookupException(new Exception()));

        // When
        node.process(newTreeContext(json(object(field(REALM, "/"), field(USERNAME, "username")))));

        // Then -> IdRepoException is catch and transformed into a NodeProcessException
    }

}
