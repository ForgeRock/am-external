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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.utils.BigIntegerUtils;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.util.encode.Base64url;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.primitives.Bytes;

@ExtendWith(MockitoExtension.class)
public class FidoU2fVerifierTest {
    private static final String FORMAT = "fido-u2f";

    private static KeyPair ecdsaKeyPair;
    @Mock
    private TrustAnchorValidator trustAnchorValidator;
    @Mock
    private X509Certificate certificate;
    private FidoU2fVerifier fidoU2fVerifier;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();
    }

    public static Stream<Arguments> invalidEcdsaSignatures() {
        BigInteger order = ((ECPublicKey) ecdsaKeyPair.getPublic()).getParams().getOrder();
        byte[] tooBig = BigIntegerUtils.toBytesUnsigned(order);
        byte[] tooSmall = new byte[32];
        byte[] justRight = BigIntegerUtils.toBytesUnsigned(order.subtract(BigInteger.ONE));

        return Stream.of(
                Arguments.of(Bytes.concat(tooSmall, tooSmall)),
                Arguments.of(Bytes.concat(tooSmall, justRight)),
                Arguments.of(Bytes.concat(justRight, tooSmall)),
                Arguments.of(Bytes.concat(tooBig, tooBig)),
                Arguments.of(Bytes.concat(tooBig, justRight)),
                Arguments.of(Bytes.concat(justRight, tooBig))
        );
    }

    public static Stream<Arguments> aaguids() {
        Aaguid validAaguid = new Aaguid(new byte[16]);
        Aaguid nonZeroAaguid = new Aaguid(UUID.randomUUID());

        return Stream.of(
                Arguments.of(validAaguid, true),
                Arguments.of(nonZeroAaguid, false)
        );
    }

    @BeforeEach
    void setup() throws Exception {


        fidoU2fVerifier = new FidoU2fVerifier(trustAnchorValidator, true);
    }

    @ParameterizedTest
    @MethodSource("invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatures(byte[] invalidSignature) {
        // Given
        EcJWK jwk = EcJWK.builder((ECPublicKey) ecdsaKeyPair.getPublic()).build();
        AttestedCredentialData attestedCredentialData =
                new AttestedCredentialData(new Aaguid(UUID.randomUUID()), 0, new byte[0],
                        jwk, "SHA256WithECDSA");
        AuthData authData = new AuthData(new byte[0], null, 0, attestedCredentialData, null);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(certificate));
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);
        byte[] clientDataHash = new byte[32];

        // When
        VerificationResponse result = fidoU2fVerifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("aaguids")
    public void shouldRejectInvalidAaguidsWithValidSignature(Aaguid aaguid, boolean expectedResult) {
        // Given
        if (expectedResult) {
            given(certificate.getPublicKey()).willReturn(ecdsaKeyPair.getPublic());
        }
        EcJWK jwk = EcJWK.builder((ECPublicKey) ecdsaKeyPair.getPublic()).build();
        byte[] publicKeyU2F = getKeyBytes(jwk);
        byte[] clientDataHash = new byte[32];
        AttestedCredentialData attestedCredentialData = new AttestedCredentialData(aaguid, 0, new byte[0],
                jwk, "SHA256WithECDSA");
        AuthData authData = new AuthData(new byte[0], null, 0, attestedCredentialData, null);
        AttestationStatement statement = new AttestationStatement();
        byte[] sigBytes;

        try {
            sigBytes = getValidSignatureBytes(authData, clientDataHash, attestedCredentialData, publicKeyU2F);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
        statement.setAttestnCerts(List.of(certificate));
        statement.setSig(sigBytes);
        AttestationObject attestationObject = new AttestationObject(FORMAT, authData, statement);

        // When
        VerificationResponse result = fidoU2fVerifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isEqualTo(expectedResult);
    }

    private byte[] getValidSignatureBytes(AuthData authData,
            byte[] clientDataHash,
            AttestedCredentialData attestedCredentialData,
            byte[] publicKeyU2F)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // Construct verification data to create valid signature
        ByteBuffer buffer = ByteBuffer.allocate(authData.rpIdHash.length
                + clientDataHash.length
                + attestedCredentialData.credentialIdLength
                + publicKeyU2F.length + 1);
        buffer.put((byte) 0x00);
        buffer.put(authData.rpIdHash);
        buffer.put(clientDataHash);
        buffer.put(attestedCredentialData.credentialId);
        buffer.put(publicKeyU2F);
        byte[] verificationData = buffer.array();

        Signature ecdsaSign = Signature.getInstance("SHA256WithECDSA");
        ecdsaSign.initSign(ecdsaKeyPair.getPrivate());
        ecdsaSign.update(verificationData);

        return ecdsaSign.sign();
    }

    private byte[] getKeyBytes(EcJWK keyData) {
        byte[] x = Base64url.decode(keyData.getX());
        byte[] y = Base64url.decode(keyData.getY());

        ByteBuffer buffer = ByteBuffer.allocate(x.length + y.length + 1);
        buffer.put((byte) 0x04);
        buffer.put(x);
        buffer.put(y);
        return buffer.array();
    }
}
