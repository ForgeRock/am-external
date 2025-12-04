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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oauth.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.social.idp.GoogleClientConfig;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.openam.social.idp.TwitterClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Multimap;

@ExtendWith(MockitoExtension.class)
public class OidcRpTrustStoreSecretIdProviderTest {

    @Mock
    private SocialIdentityProviders providerConfigStore;

    @Mock
    private Realm mockRealm;

    @Mock
    private OpenIDConnectClientConfig oidcConfigUsingCustomTrustStore;

    @Mock
    private GoogleClientConfig oidcSubConfigUsingCustomTrustStore;

    @Mock
    private OpenIDConnectClientConfig oidcConfigUsingDefaultTrustStore;

    @Mock
    private TwitterClientConfig configWithNoOptionToConfigureCustomTrustStore;

    private OidcRpTrustStoreSecretIdProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OidcRpTrustStoreSecretIdProvider(providerConfigStore);
    }

    @Test
    void testRealmMultiInstanceSecretIds() {

        //given
        Map<String, OAuthClientConfig> configs = generateConfigs();
        given(providerConfigStore.getProviders(mockRealm)).willReturn(configs);

        //when
        Multimap<String, String> result = provider.getRealmMultiInstanceSecretIds(null, mockRealm);

        //then
        assertThat(result.asMap()).containsOnlyKeys("OIDC Reliant Party CA Trust Stores");
        assertThat(result.get("OIDC Reliant Party CA Trust Stores"))
                .containsExactlyInAnyOrder(
                        "am.services.oidc.reliant.party.oidcConfigWithAlias.truststore",
                        "am.services.oidc.reliant.party.oidcSubConfigWithAlias.truststore");
    }

    private Map<String, OAuthClientConfig> generateConfigs() {
        given(oidcConfigUsingCustomTrustStore.provider()).willReturn("oidcConfigWithAlias");
        given(oidcSubConfigUsingCustomTrustStore.provider()).willReturn("oidcSubConfigWithAlias");
        given(oidcConfigUsingCustomTrustStore.useCustomTrustStore()).willReturn(true);
        given(oidcSubConfigUsingCustomTrustStore.useCustomTrustStore()).willReturn(true);
        given(oidcConfigUsingDefaultTrustStore.useCustomTrustStore()).willReturn(false);
        return Map.of(
                "configWithNoOptionToConfigureCustomTrustStore", configWithNoOptionToConfigureCustomTrustStore,
                "oidcConfigUsingCustomTrustStore", oidcConfigUsingCustomTrustStore,
                "oidcSubConfigUsingCustomTrustStore", oidcSubConfigUsingCustomTrustStore,
                "oidcConfigUsingDefaultTrustStore", oidcConfigUsingDefaultTrustStore
        );
    }
}
