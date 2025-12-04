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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bouncycastle.jcajce.spec.EdDSAParameterSpec.Ed25519;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.OkpJWK;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class AndroidKeyVerifierTest {
    private static final String FORMAT = "android-key";
    private static final String ANDROID_KEY_EXTENSION_DATA_OID = "1.3.6.1.4.1.11129.2.1.17";

    @Mock
    private TrustAnchorValidator trustAnchorValidator;

    @Mock
    private X509Certificate cert;

    private AndroidKeyVerifier verifier;

    public static Stream<Arguments> invalidEcdsaSignatures() {
        return Stream.of(
                arguments(CoseAlgorithm.ES256, new byte[64]),
                arguments(CoseAlgorithm.ES384, new byte[96]),
                arguments(CoseAlgorithm.ES512, new byte[132])
        );
    }

    @BeforeEach
    void setup() {
        FlowUtilities flowUtilities = new FlowUtilities(null);
        verifier = new AndroidKeyVerifier(trustAnchorValidator, flowUtilities);
    }

    @ParameterizedTest
    @MethodSource("invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatures(CoseAlgorithm algorithm, byte[] invalidSignature) throws Exception {
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, "EC");
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(algorithm);
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        boolean valid = verifier.verifySignature(attestationObject, new byte[0]);

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void shouldAcceptEd25519Signature() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();
        given(cert.getPublicKey()).willReturn(ed25519KeyPair.getPublic());

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        boolean valid = verifier.verifySignature(attestationObject, clientDataHash);

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectMissingOIDCertExtension() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();
        given(cert.getPublicKey()).willReturn(ed25519KeyPair.getPublic());
        given(cert.getExtensionValue(eq(ANDROID_KEY_EXTENSION_DATA_OID))).willReturn(null);

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        JWK publicKeyJwk = OkpJWK.builder().publicKey(ed25519KeyPair.getPublic()).build();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, publicKeyJwk, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = verifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldRejectCertExtensionWithPublicKeyMismatch() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();
        given(cert.getPublicKey()).willReturn(ed25519KeyPair.getPublic());

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        KeyPair differentKeyPair = keyPairGenerator.generateKeyPair();
        JWK publicKeyJwk = OkpJWK.builder().publicKey(differentKeyPair.getPublic()).build();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, publicKeyJwk, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = verifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldRejectCertExtensionWithExtensionChallengeMismatch() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();
        given(cert.getPublicKey()).willReturn(ed25519KeyPair.getPublic());
        given(cert.getExtensionValue(eq(ANDROID_KEY_EXTENSION_DATA_OID))).willReturn(null);

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        JWK publicKeyJwk = OkpJWK.builder().publicKey(ed25519KeyPair.getPublic()).build();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, publicKeyJwk, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = verifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    private byte[] sign(CoseAlgorithm algorithm, byte[] rawAuthenticatorData, byte[] clientDataHash,
            PrivateKey privateKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        ByteBuffer verificationDataBuffer = ByteBuffer.allocate(rawAuthenticatorData.length + clientDataHash.length);
        verificationDataBuffer.put(rawAuthenticatorData);
        verificationDataBuffer.put(clientDataHash);

        Signature signature = Signature.getInstance(algorithm.getExactAlgorithmName());
        signature.initSign(privateKey);
        signature.update(verificationDataBuffer.array());
        return signature.sign();
    }
}
