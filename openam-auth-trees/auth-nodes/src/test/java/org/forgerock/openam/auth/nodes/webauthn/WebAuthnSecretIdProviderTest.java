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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.Multimap;

public class WebAuthnSecretIdProviderTest {

    @Mock
    AnnotatedServiceRegistry mockRegistry;
    @Mock
    Realm mockRealm;

    private WebAuthnSecretIdProvider provider;

    @BeforeTest
    public void theSetUp() {
        initMocks(this);
        provider = new WebAuthnSecretIdProvider(mockRegistry);
    }

    @Test
    public void testGetRealmMultiInstanceSecretIds() throws Exception {
        //given
        given(mockRegistry.getRealmInstances(WebAuthnRegistrationNode.Config.class, mockRealm))
                .willReturn(generateConfigs());

        //when
        Multimap<String, String> result = provider.getRealmMultiInstanceSecretIds(null, mockRealm);

        //then
        assertThat(result.asMap()).containsOnlyKeys("WebAuthn CA Trust Stores");
        assertThat(result.get("WebAuthn CA Trust Stores"))
                .containsExactlyInAnyOrder("am.authentication.nodes.webauthn.truststore.trustalias",
                        "am.authentication.nodes.webauthn.truststore.configuredValue");

    }

    private Set<WebAuthnRegistrationNode.Config> generateConfigs() {
        WebAuthnRegistrationNode.Config defaultTrustStoreAliasConfig = new WebAuthnRegistrationNode.Config() {
            @Override
            public Optional<String> relyingPartyDomain() {
                return Optional.empty();
            }

            @Override
            public Optional<String> displayNameSharedState() {
                return Optional.empty();
            }
        };

        WebAuthnRegistrationNode.Config configuredTrustStoreAlias = new WebAuthnRegistrationNode.Config() {
            @Override
            public Optional<String> relyingPartyDomain() {
                return Optional.empty();
            }

            @Override
            public Optional<String> displayNameSharedState() {
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreAlias() {
                return Optional.of("configuredValue");
            }
        };

        WebAuthnRegistrationNode.Config emptyTrustStoreAlias = new WebAuthnRegistrationNode.Config() {
            @Override
            public Optional<String> relyingPartyDomain() {
                return Optional.empty();
            }

            @Override
            public Optional<String> displayNameSharedState() {
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreAlias() {
                return Optional.empty();
            }
        };

        return Set.of(defaultTrustStoreAliasConfig, configuredTrustStoreAlias, emptyTrustStoreAlias);
    }


}
