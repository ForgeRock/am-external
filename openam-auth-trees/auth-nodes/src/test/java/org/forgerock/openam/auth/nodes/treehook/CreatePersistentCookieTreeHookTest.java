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

package org.forgerock.openam.auth.nodes.treehook;


import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtClaimsHandler;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtStringSupplier;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.keys.SigningKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.iplanet.sso.SSOToken;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreatePersistentCookieTreeHookTest {

    private final SetPersistentCookieNode.Config config = new TestConfigWithSigningKey();
    private static final Purpose<SigningKey> NODE_DEFINED_PURPOSE = Purpose.purpose("nodeDefinedPurpose",
            SigningKey.class);
    private static final String CONFIG_SIGNING_KEY = "configSigningKey";
    @Mock
    private Request request;
    @Mock
    private Response response;
    @Mock
    private Realm realm;
    @Mock
    private SigningKey signingKey;
    @Mock
    private PersistentJwtStringSupplier persistentJwtStringSupplier;
    @Mock
    private PersistentCookieResponseHandler persistentCookieResponseHandler;
    @Mock
    private PersistentJwtClaimsHandler persistentJwtClaimsHandler;
    private CreatePersistentCookieTreeHook treeHook;
    @Mock
    private SecretReferenceCache secretReferenceCache;
    @Mock
    private SecretCache secretCache;
    @Mock
    private SecretReference<SigningKey> signingKeySecretReference;
    @Mock
    private SSOToken ssoToken;

    @BeforeEach
    void setUp() throws Exception {
        given(secretReferenceCache.realm(realm)).willReturn(secretCache);
        treeHook = new CreatePersistentCookieTreeHook(ssoToken, response, config, request, realm,
                persistentJwtStringSupplier, persistentCookieResponseHandler, persistentJwtClaimsHandler,
                secretReferenceCache);
        given(ssoToken.getProperty("sun.am.UniversalIdentifier")).willReturn("clientId");
        given(ssoToken.getProperty("Service")).willReturn("service");
        given(ssoToken.getProperty("Host")).willReturn("123.456.789.1");
        given(persistentJwtClaimsHandler.createJwtAuthContext(any(), any(), any(), any())).willReturn(emptyMap());
        given(signingKeySecretReference.get()).willReturn(signingKey);
    }

    @Test
    void setsCookieWithJwtStringOnResponse() throws Exception {
        // Given
        given(secretCache.active(NODE_DEFINED_PURPOSE)).willReturn(signingKeySecretReference);
        given(signingKey.getStableId()).willReturn("test-stable-id");
        given(persistentJwtStringSupplier.createJwtString(any(), any(), anyLong(), anyLong(), any(), any()))
                .willReturn("jwtString");

        // When
        try (var h = Mockito.mockStatic(PersistentCookieResponseHandler.class)) {
            h.when(() -> PersistentCookieResponseHandler.getOrgName(response)).thenReturn("orgName");
            treeHook.accept();
        }

        // Then
        verify(persistentCookieResponseHandler).setCookieOnResponse(
                eq(response),
                eq(request),
                eq(config.persistentCookieName()),
                eq("jwtString"),
                any(),
                eq(config.useSecureCookie()),
                eq(config.useHttpOnlyCookie()));
    }

    @Test
    void throwsExceptionIfNoSigningKeyAvailable() throws NoSuchSecretException {
        var badSecretReference = mock(SecretReference.class);
        given(badSecretReference.get()).willThrow(new NoSuchSecretException("No secret found"));
        given(secretCache.active(any())).willReturn(badSecretReference);

        try (var h = Mockito.mockStatic(PersistentCookieResponseHandler.class)) {
            h.when(() -> PersistentCookieResponseHandler.getOrgName(response)).thenReturn("orgName");
            assertThatThrownBy(() -> treeHook.accept()).isInstanceOf(TreeHookException.class);
        }
    }

    private static class TestConfigWithSigningKey implements SetPersistentCookieNode.Config {
        @Override
        public Optional<char[]> hmacSigningKey() {
            return Optional.of(CONFIG_SIGNING_KEY.toCharArray());
        }

        @Override
        public Optional<Purpose<SigningKey>> signingKeyPurpose() {
            return Optional.of(NODE_DEFINED_PURPOSE);
        }
    }
}
