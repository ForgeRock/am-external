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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.jose.jwk.KeyUseConstants.ENC;
import static org.forgerock.json.jose.jwk.KeyUseConstants.SIG;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import javax.inject.Provider;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResponseEncryptionStrategyTest {

    private static ECPublicKey publicKey;
    @Mock
    private Provider<JWKSet> mockJwkSetProvider;

    @BeforeAll
    static void generatePublicKey() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
        keyGenerator.initialize(SupportedEllipticCurve.P256.getParameters());
        publicKey = (ECPublicKey) keyGenerator.generateKeyPair().getPublic();
    }

    @Test
    void shouldUseEphemeralKeyPairForEcdhe() {
        assertThat(ResponseEncryptionStrategy.ECDHE.getEncryptionKeyPair(mockJwkSetProvider)).isInstanceOf(EcJWK.class);
        verifyNoInteractions(mockJwkSetProvider);
    }

    @Test
    void shouldUseEncryptionKeyWhenPresent() {
        // Given
        final JWK expectedJwk = new EcJWK(publicKey, ENC, null);
        final JWK differentJwk = new EcJWK(publicKey, SIG, null);
        final JWKSet jwkSet = new JWKSet(Arrays.asList(differentJwk, expectedJwk));
        given(mockJwkSetProvider.get()).willReturn(jwkSet);

        // When
        final JWK result = ResponseEncryptionStrategy.PSK.getEncryptionKeyPair(mockJwkSetProvider);

        // Then
        assertThat(JwkUtils.essentialKeys(result)).isEqualTo(JwkUtils.essentialKeys(expectedJwk));
    }

    @Test
    void shouldErrorIfMoreThanOneEncryptionKeyRegistered() {
        // Given
        final JWK expectedJwk = new EcJWK(publicKey, ENC, null);
        final JWK differentJwk = new EcJWK(publicKey, ENC, null);
        final JWKSet jwkSet = new JWKSet(Arrays.asList(differentJwk, expectedJwk));
        given(mockJwkSetProvider.get()).willReturn(jwkSet);

        // When
        assertThatThrownBy(() -> ResponseEncryptionStrategy.PSK.getEncryptionKeyPair(mockJwkSetProvider))
                // Then
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldErrorIfNoEncyrpionKeyRegistered() {
        given(mockJwkSetProvider.get()).willReturn(new JWKSet());
        assertThatThrownBy(() -> ResponseEncryptionStrategy.PSK.getEncryptionKeyPair(mockJwkSetProvider))
                .isInstanceOf(IllegalStateException.class);
    }
}
