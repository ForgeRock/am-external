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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

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

    private PersistentJwtClaimsHandler persistentJwtClaimsHandler = new PersistentJwtClaimsHandler();
    private ResourceBundle resourceBundle;
    private String orgName;
    private String clientId;
    private String service;
    private String clientIp;

    @BeforeMethod
    public void before() throws IdRepoException, SSOException {
        initMocks(this);
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
    public void testCreateJwtAuthContextNullPointers() throws Exception {
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

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetClaimsSetContextNullClaims() throws Exception {
        given(jwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(jwtClaimsSet.getClaim(any())).willReturn(null);
        persistentJwtClaimsHandler.getClaimsSetContext(jwt, resourceBundle);
    }

    @Test
    public void testValidateClaimsPositive() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, clientIp, true);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testValidateClaimsWrongRequestOrg() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext("Wrong", clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, clientIp, true);
    }

    @Test
    public void testValidateClaimsWrongIpNotEnforced() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, "WrongIp", false);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testValidateClaimsWrongIpEnforced() throws Exception {
        Map<String, String> claims = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIp);
        persistentJwtClaimsHandler.validateClaims(claims, resourceBundle, orgName, "WrongIp", true);
    }

    @Test
    public void getUsernameValidUser() throws Exception {
        Map<String, String> userMap = new HashMap<>();
        userMap.put(OPENAM_USER_CLAIM_KEY, ID_ATTRIBUTE + "=" + "testUser,otherfield=value");
        String userName = persistentJwtClaimsHandler.getUsername(userMap, resourceBundle);
        assertThat(userName).isEqualTo("testUser");
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void getUsernameNullClaims() throws Exception {
        persistentJwtClaimsHandler.getUsername(null, resourceBundle);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void getUsernameEmptyClaims() throws Exception {
        Map<String, String> userMap = new HashMap<>();
        persistentJwtClaimsHandler.getUsername(userMap, resourceBundle);
    }

    @Test
    public void getUsernameNoUser() throws Exception {
        Map<String, String> userMap = new HashMap<>();
        userMap.put(OPENAM_USER_CLAIM_KEY, "field=value,otherfield=value");
        String userName = persistentJwtClaimsHandler.getUsername(userMap, resourceBundle);
        assertThat(userName).isNull();
    }

}