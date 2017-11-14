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
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JwkUtilsTest {

    private EcJWK ecKeyPair;
    private RsaJWK rsaKeyPair;
    private OctJWK secretKey;

    @BeforeClass
    public void createKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(SupportedEllipticCurve.P256.getParameters());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        ecKeyPair = new EcJWK((ECPublicKey) keyPair.getPublic(), (ECPrivateKey) keyPair.getPrivate(),
                KeyUse.SIG, "some kid");

        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        rsaKeyPair = new RsaJWK((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate(), KeyUse.ENC,
                "RSA-OAEP-256", "some kid", "some x5u", "some x5t", null);

        secretKey = new OctJWK(KeyUse.ENC, "dir", "some kid", "key bytes", null, null, null);
    }

    @Test
    public void shouldReturnEssentialKeysForEcJwk() throws Exception {
        // https://tools.ietf.org/html/rfc7638#section-3.2
        assertThat(new ArrayList<>(JwkUtils.essentialKeys(ecKeyPair).keySet())).containsExactly("crv", "kty", "x", "y");
    }

    @Test
    public void shouldReturnEssentialKeysForRsaJwk() throws Exception {
        // https://tools.ietf.org/html/rfc7638#section-3.2
        assertThat(new ArrayList<>(JwkUtils.essentialKeys(rsaKeyPair).keySet())).containsExactly("e", "kty", "n");
    }

    @Test
    public void shouldReturnEssentialKeysForOctJWK() throws Exception {
        // https://tools.ietf.org/html/rfc7638#section-3.2
        assertThat(new ArrayList<>(JwkUtils.essentialKeys(secretKey).keySet())).containsExactly("k", "kty");
    }

    @Test
    public void shouldReturnCorrectKeyType() throws Exception {
        assertThat(JwkUtils.keyType(ecKeyPair)).isEqualTo(KeyType.EC);
        assertThat(JwkUtils.keyType(rsaKeyPair)).isEqualTo(KeyType.RSA);
        assertThat(JwkUtils.keyType(secretKey)).isEqualTo(KeyType.OCT);
    }

    @Test
    public void shouldMatchThumbprintRfcExample() throws Exception {
        // https://tools.ietf.org/html/rfc7638#section-3.1
        // Given
        final RsaJWK jwk = RsaJWK.parse("{"
                + "      \"kty\": \"RSA\","
                + "      \"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAt"
                + "            VT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn6"
                + "            4tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FD"
                + "            W2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n9"
                + "            1CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINH"
                + "            aQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\","
                + "      \"e\": \"AQAB\","
                + "      \"alg\": \"RS256\","
                + "      \"kid\": \"2011-04-29\""
                + "     }");

        // When
        final byte[] thumbprint = JwkUtils.thumbprint("SHA-256", jwk);

        // Then
        assertThat(Base64url.encode(thumbprint)).isEqualTo("NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs");
    }
}