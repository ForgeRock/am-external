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
 * Copyright 2026 ForgeRock AS.
 */
package com.sun.identity.authentication.modules.msisdn;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.ldap.ModuleState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.Constants;

public class MSISDNValidationTest {

    /**
     * {@code CollectionHelper.getServerMapAttr} dereferences {@code com.iplanet.am.server.host} on this
     * branch without a null guard (the guard arrived on master later), so the property must be set for
     * the config-loading path to run in a unit test.
     */
    @BeforeAll
    static void setServerHostProperty() {
        System.setProperty(Constants.AM_SERVER_HOST, "localhost");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "*                              | (sunIdentityMSISDNNumber=\\2A)",
        "1555)(sunIdentityMSISDNNumber=*| (sunIdentityMSISDNNumber=1555\\29\\28sunIdentityMSISDNNumber=\\2A)",
        "1555\\                          | (sunIdentityMSISDNNumber=1555\\5C)",
        "15551234567                    | (sunIdentityMSISDNNumber=15551234567)"
    })
    void shouldEscapeMsisdnNumberInLdapSearchFilter(String msisdnNumber, String expectedFilter)
            throws AuthLoginException {
        // Given
        LDAPAuthUtils ldapAuthUtils = mock(LDAPAuthUtils.class);
        given(ldapAuthUtils.getState()).willReturn(ModuleState.USER_FOUND);
        given(ldapAuthUtils.getUserId()).willReturn("uid=alice,ou=people,dc=example,dc=com");
        MSISDNValidation msisdnValidation = new StubbedMSISDNValidation(validOptions(), ldapAuthUtils);

        // When
        msisdnValidation.getUserId(msisdnNumber);

        // Then
        verify(ldapAuthUtils).setFilter(expectedFilter);
    }

    private static Map<String, Set<String>> validOptions() {
        return Map.of(
                "sunAMAuthMSISDNUserSearchAttribute", Set.of("sunIdentityMSISDNNumber"),
                "sunAMAuthMSISDNPrincipalUser", Set.of("cn=Directory Manager"),
                "sunAMAuthMSISDNPrincipalPasswd", Set.of("password"),
                "sunAMAuthMSISDNLdapProviderUrl", Set.of("localhost:389"),
                "sunAMAuthMSISDNBaseDn", Set.of("dc=example,dc=com"));
    }

    /**
     * Subclass seam that substitutes a mock {@link LDAPAuthUtils} for the one {@link MSISDNValidation#getUserId}
     * would otherwise create, so the filter it builds can be observed without a real directory connection.
     */
    private static final class StubbedMSISDNValidation extends MSISDNValidation {

        private final LDAPAuthUtils ldapAuthUtils;

        StubbedMSISDNValidation(Map<String, Set<String>> options, LDAPAuthUtils ldapAuthUtils)
                throws AuthLoginException {
            super(options, LoggerFactory.getLogger(StubbedMSISDNValidation.class), null, Locale.ENGLISH);
            this.ldapAuthUtils = ldapAuthUtils;
        }

        @Override
        LDAPAuthUtils createLdapAuthUtils() {
            return ldapAuthUtils;
        }
    }
}
