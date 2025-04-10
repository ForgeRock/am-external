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
 *  Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 *
 */

package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils.getFromClasspath;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils.readStream;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.junit.jupiter.api.Test;

public class CertificateUtilsTest {

    private SignedJwt jwt;

    @Test
    void testReadingCertificate() throws Exception {
        assertThat(CertificateUtils.readCertificate(
                getFromClasspath("WebAuthnRegistrationNode/forgerock.test.crt"))).isNotNull();
    }

    @Test
    void testReadingInvalidCertificate() {
        assertThatThrownBy(() -> CertificateUtils.readCertificate(
                getFromClasspath("not-a-certificate")))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void testReadingCertificateFromJwk() throws Exception {
        String string = readStream(
                getFromClasspath("forgerock-signed-jwk.jwt"));
        jwt = new JwtReconstruction().reconstructJwt(string, SignedJwt.class);

        assertThat(CertificateUtils.getCertPathFromJwkX5c(jwt.getHeader().getJsonWebKey())).isNotNull();
    }

    @Test
    void testReadingCertificateFromJwt() throws Exception {
        String string = readStream(
                getFromClasspath("forgerock-signed.jwt"));
        jwt = new JwtReconstruction().reconstructJwt(string, SignedJwt.class);

        assertThat(CertificateUtils.getCertPathFromJwtX5c(jwt)).isNotNull();
    }

    @Test
    void testReadingInvalidCertificateFromJwtX5c() {
        List<String> certificateStrings = singletonList(
                readStream(getFromClasspath("not-a-certificate")));

        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(new NOPSigningHandler());
        jwt = jwtBuilder.headers().x5c(certificateStrings).done().asJwt();

        assertThatThrownBy(() -> CertificateUtils.getCertPathFromJwtX5c(jwt))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void testReadingInvalidCertificateFromJwkX5c() throws NoSuchAlgorithmException {
        List<String> certificateStrings = singletonList(
                readStream(getFromClasspath("not-a-certificate")));
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        JWK jwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("Any")
                .algorithm(JwsAlgorithm.RS256)
                .x509Chain(certificateStrings)
                .build();
        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(new NOPSigningHandler());
        jwt = jwtBuilder.headers().jwk(jwk).done().asJwt();

        assertThatThrownBy(() -> CertificateUtils.getCertPathFromJwkX5c(jwt.getHeader().getJsonWebKey()))
                .isInstanceOf(CertificateException.class);
    }
}
