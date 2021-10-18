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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.crypto;

import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto.NODE_SHARED_STATE_DECRYPTION_PURPOSE;
import static org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto.NODE_SHARED_STATE_ENCRYPTION_PURPOSE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.EncryptedJwtBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.secrets.SecretsUtils;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.CryptoKey;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link NodeSharedStateCrypto}.
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeSharedStateCryptoTest {

    private static final JweAlgorithm JWE_ALGORITHM = JweAlgorithm.DIRECT;
    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;
    private static final JsonValue JSON_PAYLOAD = json(object(field("testClaim", "testClaimValue")));
    private static final String ENCRYPTED_STRING = "eyJEncryptedString";
    private static final String TEST_KEY = "testKey";

    @Mock
    private JwtBuilderFactory jwtBuilderFactory;

    @Mock
    private Secrets secrets;

    private NodeSharedStateCrypto nodeSharedStateCrypto;

    private SecretsProviderFacade secretsProvider;

    @Before
    public void setUp() {
        secretsProvider = mock(SecretsProviderFacade.class);
        given(secrets.getGlobalSecrets()).willReturn(secretsProvider);
        nodeSharedStateCrypto = new NodeSharedStateCrypto(jwtBuilderFactory, secrets);
    }

    @Test
    public void shouldEncrypt() throws NoSuchSecretException {
        // Given
        DataEncryptionKey testEncryptionKey = getTestKey(DataEncryptionKey.class);
        Promise<DataEncryptionKey, NoSuchSecretException> encryptionPromise =
                Promises.newResultPromise(testEncryptionKey);
        given(secretsProvider.getActiveSecret(NODE_SHARED_STATE_ENCRYPTION_PURPOSE)).willReturn(encryptionPromise);
        given(jwtBuilderFactory.jwe(any())).willReturn(new EncryptedJwtBuilder(
                SecretsUtils.convertRawEncryptionKey(testEncryptionKey, JWE_ALGORITHM, ENCRYPTION_METHOD)));

        // When
        String encryptedString = nodeSharedStateCrypto.encrypt(JSON_PAYLOAD);

        // Then
        assertThat(encryptedString, startsWith("eyJ"));
    }

    @Test
    public void shouldDecrypt() throws NoSuchSecretException {
        // Given
        EncryptedJwt mockEncryptedJwt = getMockEncryptedJwt();
        given(jwtBuilderFactory.reconstruct(anyString(), eq(EncryptedJwt.class))).willReturn(mockEncryptedJwt);
        given(mockEncryptedJwt.getClaimsSet())
                .willReturn(new JwtClaimsSet(Collections.singletonMap("testClaim", "testClaimValue")));

        DataDecryptionKey testDecryptionKey = getTestKey(DataDecryptionKey.class);
        Promise<Stream<DataDecryptionKey>, NeverThrowsException> decryptionPromise =
                Promises.newResultPromise(Stream.of(testDecryptionKey));
        given(secretsProvider.getNamedOrValidSecrets(NODE_SHARED_STATE_DECRYPTION_PURPOSE, TEST_KEY))
                .willReturn(decryptionPromise);

        // When
        JsonValue decryptedJsonValue = nodeSharedStateCrypto.decrypt(ENCRYPTED_STRING);

        // Then
        verify(mockEncryptedJwt, times(2)).decrypt(
                SecretsUtils.convertRawEncryptionKey(testDecryptionKey, JWE_ALGORITHM, ENCRYPTION_METHOD));
        assertThat(decryptedJsonValue.getObject(), is(JSON_PAYLOAD.getObject()));
    }

    @Test
    public void shouldFailToEncryptGivenInvalidPromise() {
        // Given
        Promise<DataEncryptionKey, NoSuchSecretException> invalidEncryptionPromise =
                Promises.newExceptionPromise(new NoSuchSecretException(""));
        given(secretsProvider.getActiveSecret(NODE_SHARED_STATE_ENCRYPTION_PURPOSE))
                .willReturn(invalidEncryptionPromise);

        try {
            // When
            nodeSharedStateCrypto.encrypt(JSON_PAYLOAD);

            // Then
            shouldHaveThrown(NoSuchSecretException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("No secret found for the node shared state encryption"));
        }
    }

    @Test
    public void shouldFailToDecryptGivenNoSecret() {
        // Given
        EncryptedJwt mockEncryptedJwt = getMockEncryptedJwt();
        given(jwtBuilderFactory.reconstruct(anyString(), eq(EncryptedJwt.class))).willReturn(mockEncryptedJwt);
        given(secretsProvider.getNamedOrValidSecrets(NODE_SHARED_STATE_DECRYPTION_PURPOSE, TEST_KEY))
                .willReturn(Promises.newResultPromise(Stream.empty()));

        try {
            // When
            nodeSharedStateCrypto.decrypt(ENCRYPTED_STRING);

            // Then
            shouldHaveThrown(NoSuchSecretException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("No secret found for the node shared state decryption"));
        }
    }

    private EncryptedJwt getMockEncryptedJwt() {
        EncryptedJwt mockEncryptedJwt = mock(EncryptedJwt.class);
        JweHeader mockJweHeader = mock(JweHeader.class);
        given(mockEncryptedJwt.getHeader()).willReturn(mockJweHeader);
        given(mockEncryptedJwt.getHeader().getKeyId()).willReturn(TEST_KEY);
        return mockEncryptedJwt;
    }

    private <T extends CryptoKey> T getTestKey(Class<T> secretType) throws NoSuchSecretException {
        return new SecretBuilder()
                .secretKey(new SecretKeySpec(new byte[256], "RAW"))
                .expiresAt(Instant.MAX)
                .stableId(TEST_KEY)
                .build(secretType);
    }

}