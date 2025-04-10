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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bouncycastle.jcajce.spec.EdDSAParameterSpec.Ed25519;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
import java.util.stream.Stream;

import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.OkpJWK;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.metadata.AuthenticatorDetails;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataService;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataServiceCheckStatus;
import org.forgerock.openam.auth.nodes.webauthn.metadata.NoOpMetadataService;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatus;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for {@link PackedVerifier}.
 */
@ExtendWith(MockitoExtension.class)
public class PackedVerifierTest {
    private static final String FORMAT = "packed";
    @Mock
    TrustAnchorValidator mockTrustAnchorValidator;
    @Mock
    X509Certificate mockCert;

    private ListAppender<ILoggingEvent> appender;
    private FlowUtilities flowUtilities;
    private PackedVerifier defaultVerifier;

    public static Stream<Arguments> invalidEcdsaSignatures() {
        return Stream.of(
                arguments(CoseAlgorithm.ES256, new byte[64]),
                arguments(CoseAlgorithm.ES384, new byte[96]),
                arguments(CoseAlgorithm.ES512, new byte[132])
        );
    }

    @MethodSource
    public static Stream<Arguments> metadataServiceCheckData() {
        return Stream.of(
                arguments(null, MetadataServiceCheckStatus.NOT_APPLICABLE),
                arguments(AuthenticatorStatus.REVOKED, MetadataServiceCheckStatus.FAILED),
                arguments(AuthenticatorStatus.NOT_FIDO_CERTIFIED, MetadataServiceCheckStatus.PASSED),
                arguments(AuthenticatorStatus.FIDO_CERTIFIED_L1, MetadataServiceCheckStatus.PASSED)
        );
    }

    @BeforeEach
    void setUp() {
        flowUtilities = new FlowUtilities(null);
        defaultVerifier = new PackedVerifier(flowUtilities, mockTrustAnchorValidator, new NoOpMetadataService());

        appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(PackedVerifier.class);
        logger.addAppender(appender);
    }

    @ParameterizedTest
    @MethodSource("invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatures(CoseAlgorithm algorithm, byte[] invalidSignature) throws Exception {
        // Given
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair ecdsaKeyPair = keyPairGenerator.generateKeyPair();

        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(algorithm);
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject object = new AttestationObject(FORMAT, authData, statement);

        // When
        boolean valid = defaultVerifier.isSignatureValid(object, new byte[0], ecdsaKeyPair.getPublic());

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

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject object = new AttestationObject(FORMAT, authData, statement);

        // When
        boolean valid = defaultVerifier.isSignatureValid(object, clientDataHash, ed25519KeyPair.getPublic());

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectInvalidEd25519Signature() throws Exception {
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
        AttestationObject object = new AttestationObject(FORMAT, authData, statement);

        // When
        boolean valid = defaultVerifier.isSignatureValid(object,
                "validClientDataHash".getBytes(StandardCharsets.UTF_8), ed25519KeyPair.getPublic());

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void shouldVerifyValidEd25519SignedAttestation() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        // Illustrate that the metadata is not used for self-signed attestation
        PackedVerifier verifier = new PackedVerifier(flowUtilities, null, new FailingMetadataService());
        JWK publicKeyJwk = OkpJWK.builder().publicKey(ed25519KeyPair.getPublic()).build();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null,
                publicKeyJwk, CoseAlgorithm.EDDSA.getExactAlgorithmName());
        AuthData authData = new AuthData(null, null, 1, credentialData, rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        AttestationObject object = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse verificationResponse = verifier.verify(object, clientDataHash);

        // Then
        assertThat(verificationResponse.isValid()).isTrue();
        assertThat(verificationResponse.getAttestationType()).isEqualTo(AttestationType.SELF);
    }

    @Test
    void shouldFailToVerifyGivenNoCertificates() {
        // Given
        List<X509Certificate> attestationCerts = emptyList();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(attestationCerts);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = defaultVerifier.verify(attestationObject, new byte[32]);

        // Then
        assertLoggerMessage("webauthn authentication attestation certificates could not be found");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldAcceptMissingOIDInCertAttestation() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();

        // mock a certificate with no OID
        given(mockCert.getVersion()).willReturn(3);
        given(mockCert.getPublicKey()).willReturn(ed25519KeyPair.getPublic());
        given(mockCert.getExtensionValue(eq("1.3.6.1.4.1.45724.1.1.4"))).willReturn(null);
        given(mockTrustAnchorValidator.getAttestationType(any())).willReturn(AttestationType.NONE);

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0,
                null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData,
                rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        statement.setAttestnCerts(List.of(mockCert));
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = defaultVerifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldFailVerifyDueToMetadata() throws Exception {
        // Given
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Ed25519);
        keyPairGenerator.initialize(new EdDSAParameterSpec(Ed25519));
        KeyPair ed25519KeyPair = keyPairGenerator.generateKeyPair();
        PackedVerifier verifier = new PackedVerifier(flowUtilities, mockTrustAnchorValidator,
                new FailingMetadataService());

        byte[] rawAuthenticatorData = "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = "clientDataHash".getBytes(StandardCharsets.UTF_8);
        byte[] signedData = sign(CoseAlgorithm.EDDSA, rawAuthenticatorData,
                clientDataHash, ed25519KeyPair.getPrivate());

        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0,
                null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData,
                rawAuthenticatorData);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        statement.setSig(signedData);
        statement.setAttestnCerts(List.of(mockCert));
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = verifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("metadataServiceCheckData")
    public void performMetadataServiceCheckShouldReturnCorrectValues(AuthenticatorStatus status,
            MetadataServiceCheckStatus expected) throws MetadataException {
        MetadataService mockMetadataService = mock(MetadataService.class);
        AuthenticatorDetails mockAuthDetails = mock(AuthenticatorDetails.class);
        PackedVerifier verifier = new PackedVerifier(flowUtilities, mockTrustAnchorValidator, mockMetadataService);
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0,
                null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, new AttestationStatement());

        // Given
        if (status != null) {
            given(mockMetadataService.determineAuthenticatorStatus(any(), any())).willReturn(mockAuthDetails);
            given(mockAuthDetails.getMaxCertificationStatus()).willReturn(status);
        } else {
            given(mockMetadataService.determineAuthenticatorStatus(any(), any())).willReturn(null);
        }

        // When
        MetadataServiceCheckStatus result = verifier.performMetadataServiceCheck(attestationObject);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    private void assertLoggerMessage(String actualMessage) {
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(actualMessage).isEqualTo(event.getFormattedMessage());
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

    private static class FailingMetadataService implements MetadataService {
        @Override
        public AuthenticatorDetails determineAuthenticatorStatus(Aaguid deviceAaguid, List<X509Certificate> attestCerts)
                throws MetadataException {
            throw new MetadataException("Exceptional exception");
        }
    }
}
