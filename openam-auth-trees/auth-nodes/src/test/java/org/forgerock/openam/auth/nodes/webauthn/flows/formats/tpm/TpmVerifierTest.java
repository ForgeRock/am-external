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

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.primitives.Bytes;

public class TpmVerifierTest {

    KeyPair ecdsaKeyPair;

    @Mock
    private TrustAnchorValidator trustAnchorValidator;

    @Mock
    private X509Certificate cert;

    @BeforeMethod
    public void setup() throws Exception {
        openMocks(this).close();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();

        // Satisfy basic checks on the cert
        given(cert.getVersion()).willReturn(3);
        given(cert.getSubjectDN()).willReturn(new X500Principal(""));
        given(cert.getCriticalExtensionOIDs()).willReturn(Set.of("2.5.29.17"));
        given(cert.getSubjectAlternativeNames())
                .willReturn(List.of(List.of(4, "2.23.133.2.1=id:49424D00")));
        given(cert.getExtendedKeyUsage()).willReturn(List.of("2.23.133.8.3"));
        given(cert.getPublicKey()).willReturn(ecdsaKeyPair.getPublic());
    }

    @DataProvider
    public Object[][] invalidEcdsaSignatures() {
        List<Object[]> cases = new ArrayList<>();
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
            cases.add(new Object[] { algorithm, Bytes.concat(bad, bad) });
            cases.add(new Object[] { algorithm, Bytes.concat(good, bad) });
            cases.add(new Object[] { algorithm, Bytes.concat(bad, good) });
        }

        return cases.toArray(Object[][]::new);
    }

    @Test(dataProvider = "invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatureValues(CoseAlgorithm algorithm, byte[] invalidSignature) {
        // Given
        TpmVerifier verifier = new TpmVerifier(trustAnchorValidator);
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, null);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(List.of(cert));
        statement.setAlg(algorithm);
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject attestationObject = new AttestationObject(verifier, authData, statement);

        // When
        VerificationResponse response = verifier.verifyX5c(attestationObject, new byte[0], null);

        // Then
        assertThat(response.isValid()).isFalse();
    }
}