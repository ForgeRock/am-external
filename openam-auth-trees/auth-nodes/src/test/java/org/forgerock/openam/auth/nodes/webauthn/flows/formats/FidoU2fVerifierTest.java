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

package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.List;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.utils.BigIntegerUtils;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.primitives.Bytes;

public class FidoU2fVerifierTest {

    @Mock
    private TrustAnchorValidator trustAnchorValidator;

    @Mock
    private X509Certificate certificate;

    private FidoU2fVerifier fidoU2fVerifier;

    private KeyPair ecdsaKeyPair;

    @BeforeClass
    public void generateKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();
    }

    @BeforeMethod
    public void setup() throws Exception {
        openMocks(this).close();

        given(certificate.getPublicKey()).willReturn(ecdsaKeyPair.getPublic());

        fidoU2fVerifier = new FidoU2fVerifier(trustAnchorValidator);
    }

    @DataProvider
    public Object[][] invalidEcdsaSignatures() {
        BigInteger order = ((ECPublicKey) ecdsaKeyPair.getPublic()).getParams().getOrder();
        byte[] tooBig = BigIntegerUtils.toBytesUnsigned(order);
        byte[] tooSmall = new byte[32];
        byte[] justRight = BigIntegerUtils.toBytesUnsigned(order.subtract(BigInteger.ONE));
        return new Object[][] {
                { Bytes.concat(tooSmall, tooSmall) },
                { Bytes.concat(tooSmall, justRight) },
                { Bytes.concat(justRight, tooSmall) },
                { Bytes.concat(tooBig, tooBig) },
                { Bytes.concat(tooBig, justRight) },
                { Bytes.concat(justRight, tooBig) },
        };
    }

    @Test(dataProvider = "invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatures(byte[] invalidSignature) {
        // Given
        EcJWK jwk = EcJWK.builder((ECPublicKey) ecdsaKeyPair.getPublic()).build();
        AttestedCredentialData attestedCredentialData = new AttestedCredentialData(null, 0, new byte[0], jwk,
                "SHA256WithECDSA");
        AuthData authData = new AuthData(new byte[0], null, 0, attestedCredentialData, null);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(certificate));
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject attestationObject = new AttestationObject(fidoU2fVerifier, authData, statement);
        byte[] clientDataHash = new byte[32];

        // When
        VerificationResponse result = fidoU2fVerifier.verify(attestationObject, clientDataHash);

        // Then
        assertThat(result.isValid()).isFalse();
    }
}