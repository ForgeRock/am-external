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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.openam.auth.nodes.x509.CertificateUtils.getCertPathFromJwkX5c;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.openam.utils.file.FileUtils;
import org.junit.runner.RunWith;

@RunWith(CuppaRunner.class)
public class CertificateUtilsTest {

    private SignedJwt jwt;

    {
        describe(CertificateUtils.class.getSimpleName(), () -> {
            when("reading certificates from a JWK", () -> {
                beforeEach(() -> {
                    String string = readStream(getFromClasspath("forgerock-signed-jwk.jwt"));
                    jwt = new JwtReconstruction().reconstructJwt(string, SignedJwt.class);
                });
                it("passes", () -> {
                    assertThat(getCertPathFromJwkX5c(jwt.getHeader().getJsonWebKey())).isNotNull();
                });
                when("the JWK contains an invalid certificate", () -> {
                    beforeEach(() -> {
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
                    });
                    it("fails", () -> {
                        assertThatThrownBy(() -> getCertPathFromJwkX5c(jwt.getHeader().getJsonWebKey()))
                                .isInstanceOf(IllegalStateException.class);
                    });
                });
            });

        });
    }

    private static InputStream getFromClasspath(String name) {
        return FileUtils.class.getResourceAsStream("/" + name);
    }

    private static String readStream(InputStream stream) {
        try (DataInputStream din = new DataInputStream(stream)) {
            int available = din.available();
            byte[] buffer = new byte[available];
            din.readFully(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
