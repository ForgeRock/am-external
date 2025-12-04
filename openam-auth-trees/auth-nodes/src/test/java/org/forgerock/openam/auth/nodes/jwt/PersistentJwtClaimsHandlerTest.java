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
package org.forgerock.openam.auth.nodes.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

@ExtendWith(MockitoExtension.class)
public class PersistentJwtClaimsHandlerTest {
    private static final String ID_ATTRIBUTE = "id";
    private static final String OPENAM_USER_CLAIM_KEY = "openam.usr";
    private static final String RLM_CLAIM_KEY = "openam.rlm";
    private static final String USR_CLAIM_KEY = "openam.usr";
    private static final String ATY_CLAIM_KEY = "openam.aty";
    private static final String CLIENTIP_CLAIM_KEY = "openam.clientip";

    @Mock
    private Jwt jwt;

    @Mock
    private JwtClaimsSet jwtClaimsSet;

    private static RealmTestHelper realmHelper;

    private PersistentJwtClaimsHandler persistentJwtClaimsHandler = new PersistentJwtClaimsHandler();
    private ResourceBundle resourceBundle;
    private String orgName;
    private String clientId;
    private String service;
    private String clientIp;

    @BeforeAll
    static void setupClass() {
        realmHelper = new RealmTestHelper();
        realmHelper.setupRealmClass();
    }

    @AfterAll
    static void tearDownClass() {
        realmHelper.tearDownRealmClass();
    }

    @BeforeEach
    void before() throws IdRepoException, SSOException {
        orgName = "orgname";
        clientId = "clientId";
        service = "service";
        clientIp = "clientIp";

        resourceBundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                switch (key) {
                case "claims.context.not.found":
                    return "Claims context not found";
                case "auth.failed.diff.realm":
                    return "Wrong Realm";
                case "auth.failed.ip.mismatch":
                    return "Authentication failed. Client IP is different.";
                case "auth.failed.no.user.null.claims":
                    return "Authentication failed. Cannot read the user from null claims.";
                case "auth.failed.no.user.empty.claim":
                    return "Authentication failed. Cannot read the user from empty claims.";
                case "auth.failed.no.user.empty.claims":
                    return "Authentication failed. Cannot read the user from empty claims.";
                default:
                    return null;
                }
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        };
    }

    @Test
    void testCreateJwtAuthContextNullPointers() throws Exception {
        Map<String, String> authContext = persistentJwtClaimsHandler.createJwtAuthContext(null, clientId, service,
                clientIp);
        assertThat(authContext.get(RLM_CLAIM_KEY)).isNull();
        authContext = persistentJwtClaimsHandler.createJwtAuthContext(orgName, null, service, clientIp);
        assertThat(authContext.get(USR_CLAIM_KEY)).isNull();
        authContext = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, null, clientIp);
        assertThat(authContext.get(ATY_CLAIM_KEY)).isNull();
        authContext = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service, null);
        assertThat(authContext.get(CLIENTIP_CLAIM_KEY)).isNull();
    }

    @Test
    void testGetClaimsSetContextNullClaims() throws Exception {
        // given
        given(jwt.getClaimsSet()).willReturn(jwtClaimsSet);
        // when
        assertThatThrownBy(() ->
                persistentJwtClaimsHandler.getClaimsSetContext(jwt, resourceBundle))
        // then
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Claims context not found");
    }

    @Test
    void testValidateClaimsPositive() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, clientIp, true);
    }

    @Test
    void testValidateClaimsWrongRequestOrg() {
        // given
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext("Wrong", clientId, service,
                clientIp);
        // when
        assertThatThrownBy(() ->
                persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, clientIp, true))
        // then
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Wrong Realm");
    }

    @Test
    void testValidateClaimsWrongIpNotEnforced() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, "WrongIp", false);
    }

    @Test
    void testValidateClaimsWrongIpEnforced() {
        // given
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        // when
        assertThatThrownBy(() ->
                persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, "WrongIp", true))
        // then
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Authentication failed. Client IP is different.");
    }

    @Test
    void getUsernameNullClaims() {
        assertThatThrownBy(() ->
                persistentJwtClaimsHandler.getUsername(null, resourceBundle))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Authentication failed. Cannot read the user from null claims.");
    }

    @Test
    void getUsernameEmptyClaims() {
        Map<String, String> userMap = new HashMap<>();
        assertThatThrownBy(() ->
                persistentJwtClaimsHandler.getUsername(userMap, resourceBundle))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Authentication failed. Cannot read the user from empty claims.");
    }

    private static Stream<Arguments> usernamesFromUniversalIds() {
        return Stream.of(
                Arguments.of("demo", "id=demo,ou=user,dc=openam,dc=example,dc=com"),
                Arguments.of("demo=bad", "id=demo=bad,ou=user,dc=openam,dc=example,dc=com"),
                Arguments.of("bad=demo", "id=bad=demo,ou=user,dc=openam,dc=example,dc=com"),
                Arguments.of("demo=bad,blah=blah", "id=demo=bad\\,blah=blah,ou=user,dc=openam,dc=example,dc=com"),
                Arguments.of("bad=demo,blah=blah", "id=bad=demo\\,blah=blah,ou=user,dc=openam,dc=example,dc=com"),
                Arguments.of("demo=bad", "id=demo=bad,ou=user,dc=openam,o=sub,ou=services,dc=openam,dc=example,dc=com"),
                Arguments.of("bad=demo", "id=bad=demo,ou=user,dc=openam,o=sub,ou=services,dc=openam,dc=example,dc=com"),
                Arguments.of(
                "demo=bad,blah=blah", "id=demo=bad\\,blah=blah,ou=user,o=sub,ou=services,dc=openam,dc=example,dc=com"),
                Arguments.of(
                "bad=demo,blah=blah", "id=bad=demo\\,blah=blah,ou=user,o=sub,ou=services,dc=openam,dc=example,dc=com")
        );
    }

    @ParameterizedTest
    @MethodSource("usernamesFromUniversalIds")
    public void ensureCorrectUsernameFromUniversalId(String expectedUsername, String universalId) throws Exception {
        Map<String, String> userMap = Map.of(OPENAM_USER_CLAIM_KEY, universalId);
        String username = persistentJwtClaimsHandler.getUsername(userMap, resourceBundle);
        assertThat(username).isEqualTo(expectedUsername);
    }
}
