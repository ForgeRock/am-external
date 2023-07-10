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
 * Copyright 2017-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sun.identity.idm.AMIdentity;

/**
 * Tests for {@link PersistentJwtClaimsHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentJwtClaimsHandlerTest {
    private static final String OPENAM_USER_CLAIM_KEY = "openam.usr";
    private static final String RLM_CLAIM_KEY = "openam.rlm";
    private static final String USR_CLAIM_KEY = "openam.usr";
    private static final String ATY_CLAIM_KEY = "openam.aty";
    private static final String CLIENTIP_CLAIM_KEY = "openam.clientip";

    private ResourceBundle resourceBundle;
    private String orgName;
    private String clientId;
    private String service;
    private String clientIp;

    @Mock
    private Jwt jwt;

    @Mock
    private JwtClaimsSet jwtClaimsSet;

    @InjectMocks
    private PersistentJwtClaimsHandler persistentJwtClaimsHandler;

    @Before
    public void before() {
        orgName = "orgname";
        clientId = "clientId";
        service = "service";
        clientIp = "clientIp";
        resourceBundle = stubResourceBundle();
    }

    @Test
    public void testCreateJwtAuthContextNullPointers() {
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
    public void testGetClaimsSetContextNullClaims() {
        given(jwt.getClaimsSet()).willReturn(jwtClaimsSet);
        lenient().when(jwtClaimsSet.getClaim(any())).thenReturn(null);
        assertThatThrownBy(() -> persistentJwtClaimsHandler.getClaimsSetContext(jwt, resourceBundle))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Claims context not found");
    }

    @Test
    public void testValidateClaimsPositive() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, clientIp, true);
    }

    @Test
    public void testValidateClaimsWrongRequestOrg() {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext("Wrong", clientId, service,
                clientIp);
        assertThatThrownBy(() -> persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, clientIp,
                true))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Wrong Realm");
    }

    @Test
    public void testValidateClaimsWrongIpNotEnforced() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, "WrongIp", false);
    }

    @Test
    public void testValidateClaimsWrongIpEnforced() {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        assertThatThrownBy(() -> persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, "WrongIp",
                true))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Authentication failed. Client IP is different.");
    }

    @Test
    public void getUsernameNullClaims() {
        assertThatThrownBy(() -> persistentJwtClaimsHandler.getUsername(null, resourceBundle))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Authentication failed. Cannot read the user from null claims.");
    }

    @Test
    public void getUsernameEmptyClaims() {
        Map<String, String> userMap = new HashMap<>();
        assertThatThrownBy(() -> persistentJwtClaimsHandler.getUsername(userMap, resourceBundle))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("Authentication failed. Cannot read the user from empty claims.");
    }

    @Test
    public void shouldGetUsername() throws Exception {
        String universalId = "id=demo,ou=user,dc=openam,dc=example,dc=com";
        String expectedUsername = "demo";
        try (MockedConstruction<AMIdentity> ignored = Mockito.mockConstructionWithAnswer(AMIdentity.class,
                invocation -> expectedUsername)) {
            // Given
            Map<String, String> userMap = Map.of(OPENAM_USER_CLAIM_KEY, universalId);

            // When
            String username = persistentJwtClaimsHandler.getUsername(userMap, resourceBundle);

            // Then
            assertThat(username).isEqualTo(expectedUsername);
        }
    }

    @Test
    public void shouldGetBadUsername() throws Exception {
        String universalId = "id=bad=demo\\,blah=blah,ou=user,dc=openam,dc=example,dc=com";
        String expectedUsername = "bad=demo,blah=blah";
        try (MockedConstruction<AMIdentity> ignored = Mockito.mockConstructionWithAnswer(AMIdentity.class,
                invocation -> expectedUsername)) {
            // Given
            Map<String, String> userMap = Map.of(OPENAM_USER_CLAIM_KEY, universalId);

            // When
            String username = persistentJwtClaimsHandler.getUsername(userMap, resourceBundle);

            // Then
            assertThat(username).isEqualTo(expectedUsername);
        }
    }

    private static ResourceBundle stubResourceBundle() {
        return new ResourceBundle() {
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
}