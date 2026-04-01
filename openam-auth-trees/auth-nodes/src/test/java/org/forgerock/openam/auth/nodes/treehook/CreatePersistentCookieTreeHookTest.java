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
 * Copyright 2023-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.treehook;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.forgerock.openam.utils.Time.instant;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import org.forgerock.http.protocol.Cookie;
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
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.keys.SigningKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.NonExistentSsoToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * Tests for {@link CreatePersistentCookieTreeHook}.
 */
@ExtendWith(MockitoExtension.class)
public class CreatePersistentCookieTreeHookTest {

    private static final Duration DEFAULT_DURATION = Duration.ofHours(5);

    @Mock
    private SetPersistentCookieNode.Config config;
    @Mock
    private Request request;
    @Mock
    private Response response;
    @Mock
    private PersistentJwtStringSupplier persistentJwtStringSupplier;
    @Mock
    private PersistentCookieResponseHandler persistentCookieResponseHandler;
    @Mock
    private PersistentJwtClaimsHandler persistentJwtClaimsHandler;
    @Mock
    private SecretCache secretCache;
    @Mock
    private SSOToken ssoToken;

    private static Stream<Request> requestProvider() {
        return Stream.of(mock(Request.class), null);
    }

    @ParameterizedTest
    @MethodSource("requestProvider")
    void shouldAcceptGiven(Request request) throws Exception {
        // Given
        Map<String, String> authContext = getJwtAuthContext();
        SigningKey signingKey = mockConfigWithSigningKey();
        given(persistentJwtStringSupplier.createJwtString("orgName", authContext, DEFAULT_DURATION.toHours(),
                DEFAULT_DURATION.toHours(), signingKey, signingKey.getStableId())).willReturn("jwtString");

        CreatePersistentCookieTreeHook treeHook = createPersistentCookieTreeHook(request);

        // When
        try (var h = Mockito.mockStatic(PersistentCookieResponseHandler.class)) {
            h.when(() -> PersistentCookieResponseHandler.getOrgName(response)).thenReturn("orgName");
            treeHook.accept();
        }

        // Then
        ArgumentCaptor<Date> expiryDateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(persistentCookieResponseHandler).setCookieOnResponse(eq(response), eq(request), eq(null),
                eq("jwtString"), expiryDateCaptor.capture(), eq(false), eq(false),
                eq(Cookie.SameSite.LAX));
        assertThat(expiryDateCaptor.getValue()).isCloseTo(instant().plus(DEFAULT_DURATION), 1000);
    }

    @Test
    void shouldFailToAcceptGivenNoSigningKeyAvailable() throws NoSuchSecretException {
        SecretReference<SigningKey> signingKeySecretReference = mock(SecretReference.class);
        given(signingKeySecretReference.get()).willThrow(new NoSuchSecretException("No secret found"));
        given(config.signingKeyReference(secretCache)).willReturn(signingKeySecretReference);

        CreatePersistentCookieTreeHook treeHook = createPersistentCookieTreeHook(request);

        try (var h = Mockito.mockStatic(PersistentCookieResponseHandler.class)) {
            h.when(() -> PersistentCookieResponseHandler.getOrgName(response)).thenReturn("orgName");
            assertThatThrownBy(() -> treeHook.accept()).isInstanceOf(TreeHookException.class);
        }
    }

    @Test
    void shouldFailToInstantiateGivenNullSsoToken() {
        // Given
        SSOToken testToken = null;

        // When - Then
        assertThatThrownBy(() -> new CreatePersistentCookieTreeHook(testToken, response, config, request,
                mock(Realm.class), persistentJwtStringSupplier, persistentCookieResponseHandler,
                persistentJwtClaimsHandler, mock(SecretReferenceCache.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to create persistent cookie: SSO token not available");
    }

    @Test
    void shouldFailToInstantiateGivenNonExistentSsoToken() {
        // Given
        SSOToken testToken = new NonExistentSsoToken();

        // When - Then
        assertThatThrownBy(() -> new CreatePersistentCookieTreeHook(testToken, response, config, request,
                mock(Realm.class), persistentJwtStringSupplier, persistentCookieResponseHandler,
                persistentJwtClaimsHandler, mock(SecretReferenceCache.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to create persistent cookie: SSO token not available");
    }

    private CreatePersistentCookieTreeHook createPersistentCookieTreeHook(Request request) {
        Realm realm = mock(Realm.class);
        SecretReferenceCache secretReferenceCache = mock(SecretReferenceCache.class);
        given(secretReferenceCache.realm(realm)).willReturn(secretCache);
        return new CreatePersistentCookieTreeHook(ssoToken, response, config, request, realm,
                persistentJwtStringSupplier, persistentCookieResponseHandler, persistentJwtClaimsHandler,
                secretReferenceCache);
    }

    private Map<String, String> getJwtAuthContext() throws SSOException {
        Map<String, String> authContext = emptyMap();
        given(ssoToken.getProperty("sun.am.UniversalIdentifier")).willReturn("clientId");
        given(ssoToken.getProperty("Service")).willReturn("service");
        given(ssoToken.getProperty("Host")).willReturn("123.456.789.1");
        given(persistentJwtClaimsHandler.createJwtAuthContext("orgName", "clientId", "service", "123.456.789.1"))
                .willReturn(authContext);
        return authContext;
    }

    private SigningKey mockConfigWithSigningKey() throws NoSuchSecretException {
        given(config.maxLife()).willReturn(DEFAULT_DURATION);
        given(config.idleTimeout()).willReturn(DEFAULT_DURATION);
        given(config.sameSite()).willReturn(Cookie.SameSite.LAX);
        SecretReference<SigningKey> signingKeySecretReference = mock(SecretReference.class);
        SigningKey signingKey = mock(SigningKey.class);
        given(signingKeySecretReference.get()).willReturn(signingKey);
        given(config.signingKeyReference(secretCache)).willReturn(signingKeySecretReference);
        return signingKey;
    }
}
