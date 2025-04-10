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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.plugin.datastore.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.stream.Stream;

import org.forgerock.am.identity.application.IdentityService;
import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.openam.ldap.LDAPUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IdRepoDataStoreProviderTest {

    @Mock
    private LegacyIdentityService legacyIdentityService;
    @Mock
    private IdentityStoreFactory identityStoreFactory;
    @Mock
    private IdentityService identityService;

    private IdRepoDataStoreProvider idRepoDataStoreProvider;

    @BeforeEach
    void setUp() throws Exception {
        idRepoDataStoreProvider = new IdRepoDataStoreProvider(legacyIdentityService, identityStoreFactory,
            identityService);
    }

    @Disabled // not testing class under test but demonstrates problem
    @ParameterizedTest
    @MethodSource("exceptionThrowingUsernames")
    public void testValuesWillThrowExceptionIfAttemptToConvertToDn(String username) {
        // when - then
        assertThatThrownBy(() -> LDAPUtils.newDN(username))
            .isInstanceOf(LocalizedIllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("exceptionThrowingUsernames")
    public void testConvertUserIdThatWillThrowRuntimeExceptionsToUniversalId(String userid) {
        // given - when
        idRepoDataStoreProvider.convertUserIdToUniversalId(userid, "/");

        // then
        // does not propagate the exception
    }

    static Stream<Arguments> exceptionThrowingUsernames() {
        return Stream.of(
            Arguments.of("oiNPFA9XhdSRgfSn3Yj/pt6P8qTch/Cu6amH1Q=="),
            Arguments.of("LblhK1Y+0Wy0V0iwwPvCBmRh7F//yD1MTyGTRqggl38="),
            Arguments.of("9dW1Ce8ca0Mk606EMBXPFkKG43cwKVmd3NkBRXTpfg=")
        );
    }
}
