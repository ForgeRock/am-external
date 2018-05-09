/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import javax.inject.Provider;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResponseEncryptionStrategyTest {

    @Mock
    private Provider<JWKSet> mockJwkSetProvider;

    private ECPublicKey publicKey;

    @BeforeClass
    public void generatePublicKey() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
        keyGenerator.initialize(SupportedEllipticCurve.P256.getParameters());
        this.publicKey = (ECPublicKey) keyGenerator.generateKeyPair().getPublic();
    }

    @BeforeMethod
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldUseEphemeralKeyPairForEcdhe() {
        assertThat(ResponseEncryptionStrategy.ECDHE.getEncryptionKeyPair(mockJwkSetProvider)).isInstanceOf(EcJWK.class);
        verifyZeroInteractions(mockJwkSetProvider);
    }

    @Test
    public void shouldUseEncryptionKeyWhenPresent() {
        // Given
        final JWK expectedJwk = new EcJWK(publicKey, KeyUse.ENC, null);
        final JWK differentJwk = new EcJWK(publicKey, KeyUse.SIG, null);
        final JWKSet jwkSet = new JWKSet(Arrays.asList(differentJwk, expectedJwk));
        given(mockJwkSetProvider.get()).willReturn(jwkSet);

        // When
        final JWK result = ResponseEncryptionStrategy.PSK.getEncryptionKeyPair(mockJwkSetProvider);

        // Then
        assertThat(JwkUtils.essentialKeys(result)).isEqualTo(JwkUtils.essentialKeys(expectedJwk));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorIfMoreThanOneEncryptionKeyRegistered() {
        // Given
        final JWK expectedJwk = new EcJWK(publicKey, KeyUse.ENC, null);
        final JWK differentJwk = new EcJWK(publicKey, KeyUse.ENC, null);
        final JWKSet jwkSet = new JWKSet(Arrays.asList(differentJwk, expectedJwk));
        given(mockJwkSetProvider.get()).willReturn(jwkSet);

        // When
        ResponseEncryptionStrategy.PSK.getEncryptionKeyPair(mockJwkSetProvider);

        // Then
        // exception expected
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorIfNoEncyrpionKeyRegistered() {
        given(mockJwkSetProvider.get()).willReturn(new JWKSet());
        ResponseEncryptionStrategy.PSK.getEncryptionKeyPair(mockJwkSetProvider);
    }
}