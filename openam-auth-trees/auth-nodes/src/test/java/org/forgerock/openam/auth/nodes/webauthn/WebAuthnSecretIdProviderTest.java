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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Multimap;

@ExtendWith(MockitoExtension.class)
public class WebAuthnSecretIdProviderTest {

    @Mock
    AnnotatedServiceRegistry mockRegistry;
    @Mock
    Realm mockRealm;

    private WebAuthnSecretIdProvider provider;

    @BeforeEach
    void theSetUp() {
        provider = new WebAuthnSecretIdProvider(mockRegistry);
    }

    @Test
    void testGetRealmMultiInstanceSecretIds() throws Exception {
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
