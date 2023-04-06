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
 * Copyright 2021-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bouncycastle.jcajce.spec.EdDSAParameterSpec.Ed25519;

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
import java.security.spec.ECGenParameterSpec;
import java.util.List;

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
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for {@link PackedVerifier}.
 */
public class PackedVerifierTest {

    private ListAppender<ILoggingEvent> appender;

    private FlowUtilities flowUtilities;

    private PackedVerifier verifier;

    @BeforeMethod
    public void setUp() {
        flowUtilities = new FlowUtilities(null);
        verifier = new PackedVerifier(flowUtilities, null);

        appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(PackedVerifier.class);
        logger.addAppender(appender);
    }

    @DataProvider
    public Object[][] invalidEcdsaSignatures() {
        return new Object[][]{
                {CoseAlgorithm.ES256, new byte[64]},
                {CoseAlgorithm.ES384, new byte[96]},
                {CoseAlgorithm.ES512, new byte[132]},
        };
    }

    @Test(dataProvider = "invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatures(CoseAlgorithm algorithm, byte[] invalidSignature) throws Exception {
        // Given
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair ecdsaKeyPair = keyPairGenerator.generateKeyPair();

        PackedVerifier verifier = new PackedVerifier(null, null);
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(algorithm);
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject object = new AttestationObject(verifier, authData, statement);

        // When
        boolean valid = verifier.isSignatureValid(object, new byte[0], ecdsaKeyPair.getPublic());

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    public void shouldAcceptEd25519Signature() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        PackedVerifier verifier = new PackedVerifier(null, null);
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject object = new AttestationObject(verifier, authData, statement);

        // When
        boolean valid = verifier.isSignatureValid(object, clientDataHash, ed25519KeyPair.getPublic());

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    public void shouldRejectInvalidEd25519Signature() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "invalidClientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject object = new AttestationObject(verifier, authData, statement);

        // When
        boolean valid = verifier.isSignatureValid(object,
                "validClientDataHash".getBytes(StandardCharsets.UTF_8), ed25519KeyPair.getPublic());

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    public void shouldVerifyValidEd25519SignedAttestation() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        PackedVerifier verifier = new PackedVerifier(flowUtilities, null);
        JWK publicKeyJwk = OkpJWK.builder().publicKey(ed25519KeyPair.getPublic()).build();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null,
                publicKeyJwk, CoseAlgorithm.EDDSA.getExactAlgorithmName());
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject object = new AttestationObject(verifier, authData, statement);

        // When
        VerificationResponse verificationResponse = verifier.verify(object, clientDataHash);

        // Then
        assertThat(verificationResponse.isValid()).isTrue();
    }

    @Test
    public void shouldFailToVerifyGivenNoCertificates() {
        // Given
        List<X509Certificate> attestationCerts = emptyList();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(attestationCerts);
        AttestationObject attestationObject = new AttestationObject(verifier, authData, statement);

        // When
        VerificationResponse result = verifier.verify(attestationObject, new byte[32]);

        // Then
        assertLoggerMessage("webauthn authentication attestation certificates could not be found");
        assertThat(result.isValid()).isFalse();
    }

    private void assertLoggerMessage(String actualMessage) {
        assertThat(1).isEqualTo(appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertThat(Level.ERROR).isEqualTo(event.getLevel());
        assertThat(actualMessage).isEqualTo(event.getFormattedMessage());
    }

    private byte[] sign(CoseAlgorithm algorithm, byte[] rawAuthenticatorData, byte[] clientDataHash,
            PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        ByteBuffer verificationDataBuffer = ByteBuffer.allocate(rawAuthenticatorData.length + clientDataHash.length);
        verificationDataBuffer.put(rawAuthenticatorData);
        verificationDataBuffer.put(clientDataHash);

        Signature signature = Signature.getInstance(algorithm.getExactAlgorithmName());
        signature.initSign(privateKey);
        signature.update(verificationDataBuffer.array());
        return signature.sign();
    }
}