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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.soap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.forgerock.http.Handler;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.http.CloseableHttpClientHandlerFactory;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Secret;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_MTLS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SamlMtlsHandlerFactoryTest {

    @Mock
    private Secrets secrets;
    @Mock(answer=Answers.RETURNS_DEEP_STUBS)
    private SecretsProviderFacade secretsProviderFacade;
    @Mock
    private Secret secret;
    @Mock
    private CloseableHttpClientHandlerFactory factory;
    @Mock
    private LoadingCache<SamlMtlsHandlerFactory.CacheKey, Handler> cache;
    @Mock(answer=Answers.RETURNS_DEEP_STUBS)
    private CacheBuilder<SamlMtlsHandlerFactory.CacheKey, Handler> cacheBuilder;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private ConcurrentMap<SamlMtlsHandlerFactory.CacheKey, Handler> map;
    @Mock
    private Realm realm;
    @Mock
    private Handler handler;
    @Captor
    private ArgumentCaptor<SamlMtlsHandlerFactory.CacheKey> captor;


    private SamlMtlsHandlerFactory samlMtlsHandlerFactory;
    private final String secretLabel = "am.applications.federation.entity.providers.saml2.test.mtls";

    @BeforeEach
    void setUp() {
        MockedStatic<CacheBuilder> mockMessageFactory = mockStatic(CacheBuilder.class);
        mockMessageFactory.when(CacheBuilder::newBuilder).thenReturn(cacheBuilder);
        given(cacheBuilder.maximumSize(any(long.class)).build(any())).willReturn(cache);
        samlMtlsHandlerFactory = new SamlMtlsHandlerFactory(factory, secrets, realmLookup);
        mockMessageFactory.close();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testHandlerIsFromCustomLabel () throws Exception {
        // given
        given(secrets.getRealmSecrets(realm)).willReturn(secretsProviderFacade);
        given(secretsProviderFacade.getActiveSecret(notNull()).getOrThrowIfInterrupted()).willReturn(secret);
        given(cache.get(captor.capture())).willReturn(handler);

        // when
        samlMtlsHandlerFactory.getHandler(realm, "test");

        // then
        SamlMtlsHandlerFactory.CacheKey testCacheKey = new SamlMtlsHandlerFactory.CacheKey(realm, secretLabel);
        SamlMtlsHandlerFactory.CacheKey cacheKey = captor.getValue();
        assertThat(testCacheKey).isEqualTo(cacheKey);
    }

    @Test
    void testHandlerIsFromDefaultLabel () throws Exception {
        // given
        given(secrets.getRealmSecrets(realm)).willReturn(secretsProviderFacade);
        given(secretsProviderFacade.getActiveSecret(notNull()).getOrThrowIfInterrupted())
                .willThrow(NoSuchSecretException.class);
        given(cache.get(captor.capture())).willReturn(handler);

        // when
        samlMtlsHandlerFactory.getHandler(realm, "test");

        // then
        SamlMtlsHandlerFactory.CacheKey testCacheKey = new SamlMtlsHandlerFactory.CacheKey(realm, SAML2_DEFAULT_SP_MTLS);
        SamlMtlsHandlerFactory.CacheKey cacheKey = captor.getValue();
        assertThat(testCacheKey).isEqualTo(cacheKey);
    }

    @Test
    void testRealmStoreChangeExpiresAllCachesForRealm() throws RealmLookupException {
        // given
        given(realmLookup.lookup("alpha")).willReturn(realm);
        given(cache.asMap()).willReturn(map);

        // when
        samlMtlsHandlerFactory.secretStoreHasChanged(null, "alpha", 1);

        // then
        verify(cache).invalidateAll(any());
    }

    @Test
    void testGlobalStoreChangeExpiresAllCaches() {
        // when
        samlMtlsHandlerFactory.secretStoreHasChanged(null, null, 1);

        // then
        verify(cache).invalidateAll();
    }

    @Test
    void testRealmLookupFailedChangeExpiresAllCaches() throws RealmLookupException {
        given(realmLookup.lookup("alpha")).willThrow(RealmLookupException.class);

        // when
        samlMtlsHandlerFactory.secretStoreHasChanged(null, "alpha", 1);

        // then
        verify(cache).invalidateAll();
    }

    @Test
    void testPurposeMappingChangeExpiresCachesForLabel() throws RealmLookupException {
        // given
        given(realmLookup.lookup("alpha")).willReturn(realm);
        PurposeMapping purposeMapping = new TestPurposeMapping(secretLabel);

        // when
        samlMtlsHandlerFactory.secretStoreMappingHasChanged(purposeMapping, "alpha", 2);

        // then
        verify(cache).invalidate(any(SamlMtlsHandlerFactory.CacheKey.class));
    }

    @Test
    void testDefaultPurposeMappingChangeExpiresCachesForLabel() throws RealmLookupException {
        // given
        given(realmLookup.lookup("alpha")).willReturn(realm);
        PurposeMapping purposeMapping = new TestPurposeMapping(SAML2_DEFAULT_SP_MTLS);

        // when
        samlMtlsHandlerFactory.secretStoreMappingHasChanged(purposeMapping, "alpha", 2);

        // then
        verify(cache).invalidate(any(SamlMtlsHandlerFactory.CacheKey.class));
    }

    private static class TestPurposeMapping implements PurposeMapping {
        String secretId;

        TestPurposeMapping(String secretId) {
            this.secretId = secretId;
        }

        @Override
        public String secretId() {
            return secretId;
        }

        @Override
        public List<String> aliases() {
            return List.of();
        }
    }
}
