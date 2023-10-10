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

package org.forgerock.openam.auth.nodes;


import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SecretRSASigningHandler;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.SigningKey;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

/**
 * Test for Device Binding common utility method
 */
public class DeviceBindingTest {

    public static final String ISS = "com.example.app";
    private DeviceBinding deviceBinding;


    @BeforeMethod
    public void setup() throws IdRepoException, SSOException {
        deviceBinding = new DeviceBinding() {
        };
    }

    @Test
    public void testValidateSignature() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, ISS, "bob",
                deviceBinding.createRandomBytes(), null);
        deviceBinding.validateSignature(signedJwt, rsaJwk);
    }

    @Test(expectedExceptions = InvalidKeyException.class)
    public void testWithNullKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, ISS, "bob",
                deviceBinding.createRandomBytes(), null);
        deviceBinding.validateSignature(signedJwt, null);
    }

    @Test(expectedExceptions = SignatureException.class)
    public void testSignWithInvalidKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        KeyPair differentKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        SignedJwt signedJwt = buildSignedJwt(differentKey, kid, ISS, "bob",
                deviceBinding.createRandomBytes(), null);
        deviceBinding.validateSignature(signedJwt, rsaJwk);
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void testSignWithExpiredJwt() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -30);
        Date exp = calendar.getTime();

        String challenge = deviceBinding.createRandomBytes();
        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, ISS, "bob",
                challenge, exp);
        deviceBinding.validateClaim(signedJwt, challenge, Set.of(ISS));
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void testSignWithInvalidChallenge() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        String challenge = deviceBinding.createRandomBytes();
        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, ISS, "bob",
                "invalidChallenge", null);
        deviceBinding.validateClaim(signedJwt, challenge, Set.of(ISS));
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void testSignWithInvalidIssuer() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        String challenge = deviceBinding.createRandomBytes();
        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, "invalidIssuer", "bob",
                challenge, null);
        deviceBinding.validateClaim(signedJwt, challenge, Set.of(ISS));
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void testSignWithoutSub() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        String challenge = deviceBinding.createRandomBytes();
        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, ISS, null,
                challenge, null);
        deviceBinding.validateClaim(signedJwt, challenge, Set.of(ISS));
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void testSignWithoutIss() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        String challenge = deviceBinding.createRandomBytes();
        SignedJwt signedJwt = buildSignedJwt(keyPair, kid, null, "bob",
                challenge, null);
        deviceBinding.validateClaim(signedJwt, challenge, Set.of(ISS));
    }


    private SignedJwt buildSignedJwt(KeyPair keyPair, String kid, String iss, String sub, String challenge, Date exp)
            throws Exception {
        SigningKey signingKey = new SigningKey(new SecretBuilder()
                .secretKey(keyPair.getPrivate())
                .publicKey(keyPair.getPublic())
                .expiresAt(Instant.MAX)
                .stableId("some-id"));
        if (exp == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 30);
            exp = calendar.getTime();
        }
        JwtClaimsSetBuilder builder = new JwtClaimsSetBuilder();
        if (sub != null) {
            builder.sub(sub);
        }
        if (iss != null) {
            builder.iss(iss);
        }

        String signedJwtStr = new SignedJwtBuilderImpl(new SecretRSASigningHandler(signingKey))
                .headers().alg(JwsAlgorithm.RS256)
                .kid(kid)
                .done()
                .claims(builder
                        .claim("challenge", challenge)
                        .exp(exp)
                        .build())
                .asJwt().build();

        return new JwtReconstruction().reconstructJwt(signedJwtStr, SignedJwt.class);
    }
}