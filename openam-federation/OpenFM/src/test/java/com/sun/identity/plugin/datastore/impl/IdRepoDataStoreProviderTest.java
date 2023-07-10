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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.plugin.datastore.impl;

import static org.mockito.BDDMockito.given;

import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.ldap.LDAPUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Test
public class IdRepoDataStoreProviderTest {

    @Mock
    IdentityUtils identityUtils;

    IdRepoDataStoreProvider idRepoDataStoreProvider;

    @BeforeClass
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        idRepoDataStoreProvider = new IdRepoDataStoreProvider(identityUtils);
    }

    @Ignore // not testing class under test but demonstrates problem
    @Test(expectedExceptions = LocalizedIllegalArgumentException.class, dataProvider = "exceptionThrowingUsernames")
    public void testValuesWillThrowExceptionIfAttemptToConvertToDn(String username) {
        // when
        LDAPUtils.newDN(username);
        // then
        // expect exception
    }

    @Test(dataProvider = "exceptionThrowingUsernames")
    public void testConvertUserIdThatWillThrowRuntimeExceptionsToUniversalId(String userid) {
        // given
        given(identityUtils.getIdentityName(userid)).willThrow(LocalizedIllegalArgumentException.class);

        // when
        idRepoDataStoreProvider.convertUserIdToUniversalId(userid, "/");

        // then
        // does not propagate the exception
    }

    @DataProvider(name = "exceptionThrowingUsernames")
    public Object[][] exceptionThrowingUsernames() {
        return new Object[][]{
                {"oiNPFA9XhdSRgfSn3Yj/pt6P8qTch/Cu6amH1Q=="},
                {"LblhK1Y+0Wy0V0iwwPvCBmRh7F//yD1MTyGTRqggl38="},
                {"9dW1Ce8ca0Mk606EMBXPFkKG43cwKVmd3NkBRXTpfg="}};
    }
}