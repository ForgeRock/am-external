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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JweDecryptionCheckedException;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtClaimsSetKey;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.KeyType;
import org.forgerock.secrets.keys.KeyUsage;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PersistentJwtProviderTest extends GuiceTestCase {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());

    @InjectMocks
    private PersistentJwtProvider persistentJwtProvider;

    @Mock
    private SigningManager signingManager;

    @Mock
    private AuthKeyFactory mockAuthKeyFactory;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private Realm realm;
    @Mock
    private JwtReconstruction jwtReconstruction;
    @Mock
    private EncryptedThenSignedJwt jwt;
    @Mock
    private EncryptedJwt decryptedJwt;
    @Mock
    private PrivateKey privateKey;
    @Mock
    private ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference;
    @Mock
    private SecretReferenceCache secretReferenceCache;
    @Mock
    private SecretCache realmCache;
    @Mock
    private JweHeader jweHeader;
    @Mock
    private DataDecryptionKey dataDecryptionKey;

    @Before
    public void before() {
        given(secretReferenceCache.realm(realm)).willReturn(realmCache);
        given(jwt.getJweHeader()).willReturn(jweHeader);
        given(signingManager.newVerificationHandler(verificationKeysReference)).willReturn(mock(Promise.class));
    }

    @Test
    public void testGetValidDecryptedJwtNullJwtString() {
        assertThatThrownBy(() -> persistentJwtProvider.getValidDecryptedJwt(null, null,
                null))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("jwtString is null"));
    }

    @Test
    public void testGetValidDecryptedJwtMissingOrg() throws Exception {
        given(realmLookup.lookup("missing")).willThrow(RealmLookupException.class);
        assertThatThrownBy(() -> persistentJwtProvider.getValidDecryptedJwt("hello", "missing",
                verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("Unable to find realm for org: missing"));
    }

    @Test
    public void testGetValidDecryptedJwtBadJwtError() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willThrow(InvalidJwtException.class);
        assertThatThrownBy(() -> persistentJwtProvider.getValidDecryptedJwt("hello", null,
                verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("jwt reconstruction error"));
    }

    @Test
    public void testGetValidDecryptedJwtNullJwtError() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(null);
        assertThatThrownBy(() -> persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("jwt reconstruction error"));
    }

    @Test
    public void testGetValidDecryptedJwtNullHmacKeyError() {
        assertThatThrownBy(() -> persistentJwtProvider
                .getValidDecryptedJwt("hello", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGetValidDecryptedJwtInvalidVerifyError() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(jwt);
        given(jwt.verify(any())).willReturn(false);
        assertThatThrownBy(() -> persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("failed to verify jwt signature"));
    }

    @Test
    public void testGetValidDecryptedJwtDecryptionFileNotFoundError() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(jwt);
        given(jwt.verify(any())).willReturn(true);
        SecretsProvider provider = new SecretsProvider(FIXED_CLOCK);
        Purpose<DataDecryptionKey> purpose = Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
        ValidSecretsReference<DataDecryptionKey, NeverThrowsException> secretRef =
                ValidSecretsReference.valid(provider, purpose, FIXED_CLOCK);
        given(realmCache.namedOrValid(eq(purpose), any())).willAnswer(inv -> secretRef);
        given(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).willThrow(FileNotFoundException.class);

        assertThatThrownBy(() -> persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getCause()).isInstanceOfSatisfying(JweDecryptionCheckedException.class,
                                cause -> assertThat(cause.getMessage()).isEqualTo("Decryption failed")));
    }

    @Test
    public void testGetValidDecryptedJwtDeprecatedDecryptionReturnsJwt() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(jwt);
        given(jwt.verify(any())).willReturn(true);
        SecretsProvider provider = new SecretsProvider(FIXED_CLOCK);
        Purpose<DataDecryptionKey> purpose = Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
        ValidSecretsReference<DataDecryptionKey, NeverThrowsException> secretRef =
                ValidSecretsReference.valid(provider, purpose, FIXED_CLOCK);
        given(realmCache.namedOrValid(eq(purpose), any())).willAnswer(inv -> secretRef);
        given(jwt.decrypt(any(), eq(Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class))))
                .willAnswer(inv -> Promises.newPromise(() -> decryptedJwt));
        given(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).willReturn(privateKey);
        Date future = DateUtils.addHours(new Date(), 1);
        given(decryptedJwt.getClaimsSet()).willReturn(new JwtClaimsSet(
                Map.of(JwtClaimsSetKey.EXP.value(), future.toInstant().getEpochSecond(),
                        "tokenIdleTimeSeconds", future.toInstant().getEpochSecond())));

        assertThat(persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isEqualTo(decryptedJwt);
    }

    @Test
    public void testGetValidDecryptedJwtOutsideIdleTimeError() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(jwt);
        given(jwt.verify(any())).willReturn(true);
        SecretsProvider provider = new SecretsProvider(FIXED_CLOCK);
        Purpose<DataDecryptionKey> purpose = Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
        ValidSecretsReference<DataDecryptionKey, NeverThrowsException> secretRef =
                ValidSecretsReference.valid(provider, purpose, FIXED_CLOCK);
        provider.useSpecificSecretForPurpose(purpose, dataDecryptionKey);
        given(dataDecryptionKey.getKeyType()).willReturn(KeyType.PUBLIC);
        given(dataDecryptionKey.getKeyUsages()).willReturn(Set.of(KeyUsage.DECRYPT));
        given(realmCache.namedOrValid(eq(purpose), any())).willAnswer(inv -> secretRef);
        given(jwt.decrypt(secretRef)).willAnswer(inv -> Promises.newPromise(() -> decryptedJwt));
        Date future = DateUtils.addHours(new Date(), 1);
        Date past = DateUtils.addHours(new Date(), -1);
        given(decryptedJwt.getClaimsSet()).willReturn(new JwtClaimsSet(
                Map.of(JwtClaimsSetKey.EXP.value(), future.toInstant().getEpochSecond(),
                        "tokenIdleTimeSeconds", past.toInstant().getEpochSecond())));

        assertThatThrownBy(() -> persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("jwt is not within expiry or idle time"));
    }

    @Test
    public void testGetValidDecryptedJwtOutsideExpiryTimeError() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(jwt);
        given(jwt.verify(any())).willReturn(true);
        SecretsProvider provider = new SecretsProvider(FIXED_CLOCK);
        Purpose<DataDecryptionKey> purpose = Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
        ValidSecretsReference<DataDecryptionKey, NeverThrowsException> secretRef =
                ValidSecretsReference.valid(provider, purpose, FIXED_CLOCK);
        provider.useSpecificSecretForPurpose(purpose, dataDecryptionKey);
        given(dataDecryptionKey.getKeyType()).willReturn(KeyType.PUBLIC);
        given(dataDecryptionKey.getKeyUsages()).willReturn(Set.of(KeyUsage.DECRYPT));
        given(realmCache.namedOrValid(eq(purpose), any())).willAnswer(inv -> secretRef);
        given(jwt.decrypt(secretRef)).willAnswer(inv -> Promises.newPromise(() -> decryptedJwt));
        Date future = DateUtils.addHours(new Date(), 1);
        Date past = DateUtils.addHours(new Date(), -1);
        given(decryptedJwt.getClaimsSet()).willReturn(new JwtClaimsSet(
                Map.of(JwtClaimsSetKey.EXP.value(), past.toInstant().getEpochSecond(),
                        "tokenIdleTimeSeconds", future.toInstant().getEpochSecond())));

        assertThatThrownBy(() -> persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isInstanceOfSatisfying(InvalidPersistentJwtException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("jwt is not within expiry or idle time"));
    }

    @Test
    public void testGetValidDecryptedJwtReturnsDecryptedJwt() throws Exception {
        given(realmLookup.lookup(null)).willReturn(realm);
        given(jwtReconstruction.reconstructJwt("hello", EncryptedThenSignedJwt.class))
                .willReturn(jwt);
        given(jwt.verify(any())).willReturn(true);
        SecretsProvider provider = new SecretsProvider(FIXED_CLOCK);
        Purpose<DataDecryptionKey> purpose = Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
        provider.useSpecificSecretForPurpose(purpose, dataDecryptionKey);
        given(dataDecryptionKey.getKeyType()).willReturn(KeyType.PUBLIC);
        given(dataDecryptionKey.getKeyUsages()).willReturn(Set.of(KeyUsage.DECRYPT));
        ValidSecretsReference<DataDecryptionKey, NeverThrowsException> secretRef =
                ValidSecretsReference.valid(provider, purpose, FIXED_CLOCK);
        given(realmCache.namedOrValid(eq(purpose), any())).willAnswer(inv -> secretRef);
        given(jwt.decrypt(secretRef)).willAnswer(inv -> Promises.newPromise(() -> decryptedJwt));
        Date future = DateUtils.addHours(new Date(), 1);
        given(decryptedJwt.getClaimsSet()).willReturn(new JwtClaimsSet(
                Map.of(JwtClaimsSetKey.EXP.value(), future.toInstant().getEpochSecond(),
                        "tokenIdleTimeSeconds", future.toInstant().getEpochSecond())));

        assertThat(persistentJwtProvider
                .getValidDecryptedJwt("hello", null, verificationKeysReference))
                .isEqualTo(decryptedJwt);
    }
}
