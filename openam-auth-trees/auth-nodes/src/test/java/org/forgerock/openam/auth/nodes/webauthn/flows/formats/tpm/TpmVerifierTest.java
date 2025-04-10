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

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.given;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.VerificationResponse;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.primitives.Bytes;

@ExtendWith(MockitoExtension.class)
public class TpmVerifierTest {

    private static final X500Principal DEFAULT_PRINCIPAL = new X500Principal("");
    private static final String FORMAT = "tpm";
    private KeyPair ecdsaKeyPair;
    private KeyPair rsaKeyPair;

    @Mock
    private TrustAnchorValidator trustAnchorValidator;

    @Mock
    private X509Certificate cert;

    public static Stream<Arguments> invalidEcdsaSignatures() {
        List<Arguments> cases = new ArrayList<>();
        for (CoseAlgorithm algorithm : CoseAlgorithm.values()) {
            if (algorithm.getKeyType() != KeyType.EC) {
                continue;
            }

            int fieldSize;
            switch (algorithm) {
            case ES256:
                fieldSize = 32;
                break;
            case ES384:
                fieldSize = 48;
                break;
            case ES512:
                fieldSize = 66;
                break;
            default:
                throw new AssertionError("Unknown EC COSE algorithm");
            }

            byte[] bad = new byte[fieldSize];
            byte[] good = new byte[fieldSize];
            good[good.length - 1] = 1;
            cases.add(arguments(algorithm, Bytes.concat(bad, bad)));
            cases.add(arguments(algorithm, Bytes.concat(good, bad)));
            cases.add(arguments(algorithm, Bytes.concat(bad, good)));
        }

        return cases.stream();
    }

    public static Stream<Arguments> validSignatures() {
        return Stream.of(
                arguments("RSA", CoseAlgorithm.RS1),
                arguments("RSA", CoseAlgorithm.RS256),
                arguments("RSA", CoseAlgorithm.RS384),
                arguments("RSA", CoseAlgorithm.RS512),
                arguments("EC", CoseAlgorithm.ES256),
                arguments("EC", CoseAlgorithm.ES384),
                arguments("EC", CoseAlgorithm.ES512)
        );
    }

    public static Stream<Arguments> invalidConfigurations() {
        return Stream.of(
                arguments(2, DEFAULT_PRINCIPAL),
                arguments(3, new X500Principal("cn=invalid"))
        );
    }

    @BeforeEach
    void setup() throws Exception {
        KeyPairGenerator ecKeyPairGenerator = KeyPairGenerator.getInstance("EC");
        ecKeyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = ecKeyPairGenerator.generateKeyPair();

        KeyPairGenerator rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
        rsaKeyPair = rsaKeyPairGenerator.generateKeyPair();
    }

    private void mockCertificate(KeyPair keyPair) throws CertificateParsingException {
        mockCertificate(keyPair, 3, DEFAULT_PRINCIPAL);
    }

    private void mockCertificate(KeyPair keyPair, int version, X500Principal subjectDN)
            throws CertificateParsingException {
        given(cert.getVersion()).willReturn(version);
        given(cert.getSubjectDN()).willReturn(subjectDN);
        given(cert.getCriticalExtensionOIDs()).willReturn(Set.of("2.5.29.17"));
        given(cert.getSubjectAlternativeNames())
                .willReturn(List.of(List.of(4, "2.23.133.2.1=id:49424D00")));
        given(cert.getExtendedKeyUsage()).willReturn(List.of("2.23.133.8.3"));
        given(cert.getPublicKey()).willReturn(keyPair.getPublic());
    }

    @ParameterizedTest
    @MethodSource("invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatureValues(CoseAlgorithm algorithm, byte[] invalidSignature)
            throws Exception {
        // Satisfy basic checks on the cert
        mockCertificate(ecdsaKeyPair);

        // Given
        Set<TpmManufacturer> tpmManufacturers = new HashSet<>(Arrays.asList(TpmManufacturerId.values()));
        TpmVerifier verifier = new TpmVerifier(trustAnchorValidator, tpmManufacturers);
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, null);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(algorithm);
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse response = verifier.verifyX5c(attestationObject, new byte[0]);

        // Then
        assertThat(response.isValid()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("validSignatures")
    public void shouldAcceptValidSignatureValues(String algName, CoseAlgorithm algorithm) throws Exception {
        // Given
        KeyPair pair = getKeyPair(algName);
        mockCertificate(pair);

        byte[] message = "mySignedMessage".getBytes();
        byte[] sigBytes = createSignature(algorithm, pair, message);

        // Setup attestation object
        AttestationObject attestationObject = createAttestationObject(algorithm, sigBytes);

        // When
        Set<TpmManufacturer> tpmManufacturers = new HashSet<>(Arrays.asList(TpmManufacturerId.values()));
        TpmVerifier verifier = new TpmVerifier(trustAnchorValidator, tpmManufacturers);
        VerificationResponse response = verifier.verifyX5c(attestationObject, message);

        // Then
        assertThat(response.isValid()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidConfigurations")
    public void invalidCertificateVersion(int version, X500Principal principal) throws Exception {
        // Given
        KeyPair pair = rsaKeyPair;
        CoseAlgorithm algorithm = CoseAlgorithm.RS256;
        given(cert.getVersion()).willReturn(version);
        if (principal != DEFAULT_PRINCIPAL) {
            given(cert.getSubjectDN()).willReturn(principal);
        }

        byte[] message = "mySignedMessage".getBytes();
        byte[] sigBytes = createSignature(algorithm, pair, message);

        // Setup attestation object
        AttestationObject attestationObject = createAttestationObject(algorithm, sigBytes);

        // When
        Set<TpmManufacturer> tpmManufacturers = new HashSet<>(Arrays.asList(TpmManufacturerId.values()));
        TpmVerifier verifier = new TpmVerifier(trustAnchorValidator, tpmManufacturers);
        VerificationResponse response = verifier.verifyX5c(attestationObject, message);

        // Then
        assertThat(response.isValid()).isFalse();
    }

    private AttestationObject createAttestationObject(CoseAlgorithm algorithm, byte[] sigBytes) {
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, null);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(algorithm);
        statement.setSig(sigBytes);
        return new AttestationObject(FORMAT, authData, statement);
    }

    private byte[] createSignature(CoseAlgorithm algorithm, KeyPair pair, byte[] message) throws Exception {
        Signature signature = Signature.getInstance(algorithm.getExactAlgorithmName());
        signature.initSign(pair.getPrivate());
        signature.update(message);
        return signature.sign();
    }

    private KeyPair getKeyPair(String algName) throws NoSuchAlgorithmException {
        KeyPair pair;
        if (algName.equals("RSA")) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algName);
            pair = keyPairGenerator.generateKeyPair();
        } else {
            pair = ecdsaKeyPair;
        }
        return pair;
    }
}
