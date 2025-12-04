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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.soap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.RandomStringUtils;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.http.CloseableHttpClientHandler;
import org.forgerock.openam.http.CloseableHttpClientHandlerFactory;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.secrets.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class SamlMtlsHandlerCachingTest {

    @Mock
    private Secrets secrets;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SecretsProviderFacade secretsProviderFacade;
    @Mock
    private Secret secret;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private CloseableHttpClientHandlerFactory factory;
    @Mock
    private Realm realm;
    @Mock
    private CloseableHttpClientHandler handler;
    @Mock
    private Logger logger;

    private SamlMtlsHandlerFactory samlMtlsHandlerFactory;

    private static final String CACHE_SIZE_LIMIT_REACHED_MESSAGE = "The SAML mtls handler cache is full.";

    @BeforeEach
    void setUp() throws Exception {
        try (MockedStatic<LoggerFactory> mockMessageFactory = mockStatic(LoggerFactory.class)) {
            mockMessageFactory.when(() -> LoggerFactory.getLogger(SamlMtlsHandlerFactory.class))
                    .thenReturn(logger);

            samlMtlsHandlerFactory = new SamlMtlsHandlerFactory(factory, secrets, realmLookup);
        }

        given(secrets.getRealmSecrets(realm)).willReturn(secretsProviderFacade);
        given(secretsProviderFacade.getActiveSecret(notNull()).getOrThrowIfInterrupted()).willReturn(secret);
        given(factory.create(any())).willReturn(handler);
    }

    @Test
    void cacheSizeLimitMessageNotLoggedOnRepeatedCacheHit() {
        // given
        String label = RandomStringUtils.randomAlphabetic(20);

        // when
        for (int i = 0; i <= 100; i++) {
            samlMtlsHandlerFactory.getHandler(realm, label);
        }

        // then
        verify(logger, never()).warn(contains(CACHE_SIZE_LIMIT_REACHED_MESSAGE));
    }

    @Test
    void cacheSizeLimitMessageNotLoggedWhenCacheSizeBelowMax() {
        // when
        for (int i = 0; i <= 50; i++) {
            samlMtlsHandlerFactory.getHandler(realm, RandomStringUtils.randomAlphabetic(20));
        }

        // then
        verify(logger, never()).warn(contains(CACHE_SIZE_LIMIT_REACHED_MESSAGE));
    }

    @Test
    void cacheSizeLimitMessageLoggedWhenCacheSizeReachesMax() {

        // when
        for (int i = 0; i <= 100; i++) {
            samlMtlsHandlerFactory.getHandler(realm, RandomStringUtils.randomAlphabetic(20));
        }

        // then
        verify(logger, atLeastOnce()).warn(contains(CACHE_SIZE_LIMIT_REACHED_MESSAGE), any(), any());
    }
}
