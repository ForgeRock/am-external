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

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.HmacSigningKeyConfig.DEFAULT_SIGNING_PURPOSE;
import static org.forgerock.openam.auth.nodes.HmacSigningKeyConfig.DEFAULT_VERIFICATION_PURPOSE;
import static org.mockito.BDDMockito.given;

import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.KeyType;
import org.forgerock.secrets.keys.KeyUsage;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.shared.encode.Base64;

@ExtendWith(MockitoExtension.class)
public class HmacSigningKeyConfigTest {

    public static final String HMAC_KEY =
            "m/qw6B3Yr2nNfie/zcnZ8JiuTo8WjSkeuofisAokxzAjBKoEgZv17kD22k96wwkW1zs5KuLLtmNSBv9c097q0Q==";
    public static final String SIGNING_KEY_PURPOSE = "signingKeyPurpose";
    private HmacSigningKeyConfig hmacSigningKeyConfig;
    @Mock
    private SecretCache secretCache;
    @Mock
    private SigningKey signingKey;
    @Mock
    private VerificationKey verificationKey;

    @BeforeEach
    void setUp() throws Exception {
        hmacSigningKeyConfig = new HmacSigningKeyConfig() {
            @Override
            public Optional<Purpose<SigningKey>> signingKeyPurpose() {
                return Optional.of(Purpose.purpose(SIGNING_KEY_PURPOSE, SigningKey.class));
            }

            @Override
            public Optional<char[]> hmacSigningKey() {
                return Optional.of(HMAC_KEY.toCharArray());
            }
        };
    }

    @Test
    void testHmacSigningKeyConfigVerificationKeyPurpose() {
        Purpose<VerificationKey> verificationPurpose =
                hmacSigningKeyConfig.verificationKeyPurpose()
                        .orElseThrow(() -> new RuntimeException("Verification purpose is not present"));
        assertThat(verificationPurpose.getLabel()).isEqualTo(SIGNING_KEY_PURPOSE);
        assertThat(verificationPurpose.getSecretType()).isEqualTo(VerificationKey.class);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testHmacSigningKeyConfigWrappedSigningKey() {
        SigningKey signingKey = hmacSigningKeyConfig.wrappedHmacSigningKey()
                                        .orElseThrow(() -> new RuntimeException("Signing key is not present"));
        assertThat(signingKey.getKeyType()).isEqualTo(KeyType.SECRET);
        assertThat(signingKey.getKeyUsages()).containsExactly(KeyUsage.SIGN);
        assertThat(signingKey.getStableId()).isEqualTo(HmacSigningKeyConfig.DEPRECATED_STABLE_ID);
        assertThat(Base64.encode(signingKey.reveal(Key::getEncoded))).isEqualTo(HMAC_KEY);
        assertThat(signingKey.getExpiryTime()).isEqualTo(Instant.MAX);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testHmacSigningKeyConfigWrappedVerificationKey() {
        VerificationKey verificationKey = hmacSigningKeyConfig.wrappedHmacVerificationKey()
                                          .orElseThrow(() -> new RuntimeException("Verification key is not present"));
        assertThat(verificationKey.getKeyType()).isEqualTo(KeyType.SECRET);
        assertThat(verificationKey.getKeyUsages()).containsExactly(KeyUsage.VERIFY);
        assertThat(verificationKey.getStableId()).isEqualTo(HmacSigningKeyConfig.DEPRECATED_STABLE_ID);
        assertThat(Base64.encode(verificationKey.reveal(Key::getEncoded))).isEqualTo(HMAC_KEY);
        assertThat(verificationKey.getExpiryTime()).isEqualTo(Instant.MAX);
    }

    @Test
    void testHmacSigningKeyConfigSigningKeyReferenceFirstPurpose() throws NoSuchSecretException {
        // Given
        given(secretCache.active(hmacSigningKeyConfig.signingKeyPurpose().orElseThrow()))
                .willReturn(SecretReference.constant(signingKey));

        // When
        SecretReference<SigningKey> signingKeyReference =
                hmacSigningKeyConfig.signingKeyReference(secretCache);

        // Then
        assertThat(signingKeyReference.get()).isEqualTo(signingKey);

    }

    @Test
    void testHmacSigningKeyConfigSigningKeyReferenceDeprecated() throws NoSuchSecretException {
        // Given
        hmacSigningKeyConfig = new HmacSigningKeyConfig() {
            @Override
            public Optional<Purpose<SigningKey>> signingKeyPurpose() {
                return Optional.empty();
            }

            @Override
            public Optional<char[]> hmacSigningKey() {
                return Optional.of(HMAC_KEY.toCharArray());
            }
        };

        // When
        SecretReference<SigningKey> signingKeyReference =
                hmacSigningKeyConfig.signingKeyReference(secretCache);
        String hmacKey = signingKeyReference.get().reveal(key -> Base64.encode(key.getEncoded()));

        // Then
        assertThat(hmacKey).isEqualTo(HMAC_KEY);

    }

    @Test
    void testHmacSigningKeyConfigSigningKeyReferenceDefault() throws NoSuchSecretException {
        // Given
        hmacSigningKeyConfig = new HmacSigningKeyConfig() {
            @Override
            public Optional<Purpose<SigningKey>> signingKeyPurpose() {
                return Optional.empty();
            }

            @Override
            public Optional<char[]> hmacSigningKey() {
                return Optional.empty();
            }
        };
        given(secretCache.active(DEFAULT_SIGNING_PURPOSE))
                .willReturn(SecretReference.constant(signingKey));

        // When
        SecretReference<SigningKey> signingKeyReference =
                hmacSigningKeyConfig.signingKeyReference(secretCache);

        // Then
        assertThat(signingKeyReference.get()).isEqualTo(signingKey);

    }

    @Test
    void testHmacSigningKeyConfigVerificationKeyReferenceFirstPurpose() throws NoSuchSecretException {
        // Given
        given(verificationKey.getKeyType()).willReturn(KeyType.PUBLIC);
        given(verificationKey.getKeyUsages()).willReturn(Set.of(KeyUsage.VERIFY));
        Purpose<VerificationKey> verificationKeyPurpose = hmacSigningKeyConfig.verificationKeyPurpose().orElseThrow();
        ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference =
                new SecretsProvider(Clock.fixed(Instant.MAX, ZoneId.systemDefault()))
                        .useSpecificSecretForPurpose(verificationKeyPurpose, verificationKey)
                        .createValidReference(verificationKeyPurpose);
        given(secretCache.namedOrValid(verificationKeyPurpose, "keyId")).willReturn(verificationKeysReference);

        // When
        ValidSecretsReference<VerificationKey, NeverThrowsException> actual =
                hmacSigningKeyConfig.verificationKeysReference("keyId", secretCache);

        // Then
        assertThat(actual.get()).containsExactly(verificationKey);

    }

    @Test
    void testHmacSigningKeyConfigVerificationKeyReferenceDeprecated() throws NoSuchSecretException {
        // Given
        hmacSigningKeyConfig = new HmacSigningKeyConfig() {
            @Override
            public Optional<Purpose<SigningKey>> signingKeyPurpose() {
                return Optional.empty();
            }

            @Override
            public Optional<char[]> hmacSigningKey() {
                return Optional.of(HMAC_KEY.toCharArray());
            }
        };

        // When
        ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference =
                hmacSigningKeyConfig.verificationKeysReference("kid", secretCache);
        String hmacKey = verificationKeysReference.get().get(0).reveal(key -> Base64.encode(key.getEncoded()));

        // Then
        assertThat(hmacKey).isEqualTo(HMAC_KEY);

    }

    @Test
    void testHmacSigningKeyConfigVerificationKeyReferenceDefault() throws NoSuchSecretException {
        // Given
        hmacSigningKeyConfig = new HmacSigningKeyConfig() {
            @Override
            public Optional<Purpose<SigningKey>> signingKeyPurpose() {
                return Optional.empty();
            }

            @Override
            public Optional<char[]> hmacSigningKey() {
                return Optional.empty();
            }
        };
        given(verificationKey.getKeyType()).willReturn(KeyType.PUBLIC);
        given(verificationKey.getKeyUsages()).willReturn(Set.of(KeyUsage.VERIFY));
        ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference =
                new SecretsProvider(Clock.fixed(Instant.MAX, ZoneId.systemDefault()))
                        .useSpecificSecretForPurpose(DEFAULT_VERIFICATION_PURPOSE, verificationKey)
                        .createValidReference(DEFAULT_VERIFICATION_PURPOSE);
        given(secretCache.namedOrValid(DEFAULT_VERIFICATION_PURPOSE, "keyId")).willReturn(verificationKeysReference);

        // When
        ValidSecretsReference<VerificationKey, NeverThrowsException> actual =
                hmacSigningKeyConfig.verificationKeysReference("keyId", secretCache);

        // Then
        assertThat(actual.get()).containsExactly(verificationKey);

    }

}
