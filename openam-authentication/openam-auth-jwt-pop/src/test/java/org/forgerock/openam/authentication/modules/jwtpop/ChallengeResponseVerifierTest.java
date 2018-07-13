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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.authentication.modules.jwtpop.JwkUtils.thumbprint;

import java.nio.ByteBuffer;
import java.security.Key;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;

public class ChallengeResponseVerifierTest {
    private static final byte[] RPN = new byte[16];
    private static final byte[] RVN = new byte[16];
    static {
        Arrays.fill(RVN, (byte) 42);
        Arrays.fill(RPN, (byte) 1);
    }

    private OctJWK responseDecryptionKey;
    private Key responseEncryptionKey;
    private EcJWK confirmationKey;
    private EcJWK serverEphemeralKey;
    private EcJWK clientEphemeralKey;
    private Challenge challenge;
    private HttpCallback callback;

    private ChallengeResponseVerifier verifier;

    @BeforeMethod
    public void setup() throws Exception {
        responseDecryptionKey = OctJWK.parse(json(object(
                field("kty", "oct"),
                field("use", "enc"),
                field("alg", "dir"),
                field("k", Base64url.encode(new byte[16])))));
        responseEncryptionKey = new SecretKeySpec(Base64url.decode(responseDecryptionKey.getKey()), "AES");

        confirmationKey = EcJWK.parse(json(object(
                field("kty", "EC"),
                field("crv", "P-256"),
                field("use", "sig"),
                field("alg", "ES256"),
                field("x", "57iUXcWTc-suFhA3fzImc_L4ImheHXgmiKAuOs7pUms"),
                field("y", "lrABXqEjgPLlaUBXerhourCX2W9Uev3lQ49HSvetckE"),
                field("d", "TrUGyccuS30iEXdqrWBPsY6eorpL9PDJQIPRJdQxS-8")
        )));

        serverEphemeralKey = EcJWK.parse(json(object(
                field("kty", "EC"),
                field("crv", "P-256"),
                field("use", "enc"),
                field("alg", "ECDH-ES"),
                field("x", "X6WnHBPmiwKXV30NNIuPxp6US9h9u4QCRfYwBTXyxyY"),
                field("y", "neEP8cx3xmReK7AOqCeNbX0_uRBvLyLyGzvP5ltBxOA"),
                field("d", "WYnYDDg9yptSygmDslhJSaRYDUN6rY3O-8N2ak5g_Oc")
        )));

        clientEphemeralKey = EcJWK.parse(json(object(
                field("kty", "EC"),
                field("crv", "P-256"),
                field("use", "enc"),
                field("alg", "ECDH-ES"),
                field("x", "XeKjycYftnHYSMj7u0ISz3fjnp_dVueJV6C8DeKoLSI"),
                field("y", "fiWt-cL_tSjbOHVcZZAoa0NsT0KtFpvOu7I4DN0jzYU"),
                field("d", "UIvOKW4DlyPH-M-Z0XKLv80t0ZtxF8X9R79oJ_L5bSw")
        )));

        EcJWK challengeSigningKey = confirmationKey; // For simplicity

        challenge = Challenge.builder()
                .responseEncryptionKey(responseDecryptionKey)
                .orgDn("some realm")
                .audience("some client")
                .signingKey(challengeSigningKey)
                .expiresAt(new Date(Long.MAX_VALUE))
                .issuedAt(new Date(0))
                .nonce(Base64url.encode(RVN))
                .build();
        callback = challenge.toCallback("some realm");

        verifier = new ChallengeResponseVerifier(responseDecryptionKey, confirmationKey, challenge);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNonSignatureConfirmationKeys() throws Exception {
        // Given
        EcJWK cnf = new EcJWK(confirmationKey.toECPublicKey(), KeyUse.ENC, null);

        // When
        new ChallengeResponseVerifier(responseDecryptionKey, cnf, challenge);

        // Then - exception
    }

    @Test(expectedExceptions = AuthLoginException.class, expectedExceptionsMessageRegExp = "challenge has expired")
    public void shouldRejectIfChallengeHasExpired() throws Exception {
        // Given
        challenge = Challenge.builder()
                .responseEncryptionKey(responseDecryptionKey)
                .orgDn("some realm")
                .audience("some client")
                .signingKey(confirmationKey) // To satisfy builder, not actually used
                .expiresAt(new Date(0L))
                .issuedAt(new Date())
                .build();
        verifier = new ChallengeResponseVerifier(responseDecryptionKey, confirmationKey, challenge);
        callback.setAuthorization("some authorization response");

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "invalid authorization header")
    public void shouldRejectInvalidAuthSchemeInResponse() throws Exception {
        // Given
        callback.setAuthorization("invalid response");

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class)
    public void shouldRejectInvalidJwtInResponse() throws Exception {
        callback.setAuthorization("JWT-PoP not-a-jwt");
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "response is not encrypted.*")
    public void shouldRejectNonEncryptedResponse() throws Exception {
        // Given
        final String responseJwt = buildResponse(null, confirmationKey, new Date(Long.MAX_VALUE), new Date(),
                challenge.getAudience(), challenge.getOrganisationDn(), challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class, expectedExceptionsMessageRegExp = "response decryption failed")
    public void shouldRejectIncorrectlyEncryptedResponse() throws Exception {
        // Given
        final String responseJwt = buildResponse(new SecretKeySpec("incorrect key123".getBytes(), "AES"),
                confirmationKey, challenge.getExpiresAt(), new Date(), challenge.getAudience(),
                challenge.getOrganisationDn(), challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class)
    public void shouldRejectIncorrectlySignedResponse() throws Exception {
        // Given
        // Attempt to trick AM into accepting a JWT signed using the public key but with a symmetric algorithm,
        // that would not prove possession of the private key
        final JWK signingKey = OctJWK.parse(json(object(
                field("kty", "oct"),
                field("k", confirmationKey.getX()),
                field("alg", "HS256")
        )));
        final String responseJwt = buildResponse(responseEncryptionKey, signingKey, challenge.getExpiresAt(),
                 new Date(), challenge.getAudience(), challenge.getOrganisationDn(), challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "signature verification failed")
    public void shouldRejectInvalidSignature() throws Exception {
        // Given
        confirmationKey = EcJWK.parse(json(object(
                field("kty", "EC"),
                field("crv", "P-256"),
                field("use", "sig"),
                field("alg", "ES256"),
                field("x", "57iUXcWTc-suFhA3fzImc_L4ImheHXgmiKAuOs7pUms"),
                field("y", "lrABXqEjgPLlaUBXerhourCX2W9Uev3lQ49HSvetckE"),
                field("d", Base64url.encode(new byte[32]))
        )));

        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, challenge.getExpiresAt(),
                new Date(), challenge.getAudience(), challenge.getOrganisationDn(), challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "response was issued before challenge")
    public void shouldRejectResponsesIssuedBeforeChallenge() throws Exception {
        // Given
        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, challenge.getExpiresAt(),
                new Date(Long.MIN_VALUE), challenge.getAudience(), challenge.getOrganisationDn(),
                challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "response issued in the future")
    public void shouldRejectResponsesIssuedInTheFuture() throws Exception {
        // Given
        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, challenge.getExpiresAt(),
                new Date(Long.MAX_VALUE), challenge.getAudience(), challenge.getOrganisationDn(),
                challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "response has expired")
    public void shouldRejectResponseThatHasExpired() throws Exception {
        // Given
        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, new Date(Long.MIN_VALUE),
                new Date(), challenge.getAudience(), challenge.getOrganisationDn(), challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "incorrect issuer")
    public void shouldRejectResponseNotIssuedBySubject() throws Exception {
        // Given
        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, new Date(Long.MAX_VALUE),
                new Date(), "incorrect subject", challenge.getOrganisationDn(), challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "incorrect audience")
    public void shouldRejectResponseNotMeantForUs() throws Exception {
        // Given
        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, new Date(Long.MAX_VALUE),
                new Date(), challenge.getAudience(), "incorrect audience", challenge.getChallengeNonce());
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "signature verification failed")
    public void shouldRejectIncorrectChallengeResponse() throws Exception {
        // Given
        final String responseJwt = buildResponse(responseEncryptionKey, confirmationKey, new Date(Long.MAX_VALUE),
                new Date(), challenge.getAudience(), challenge.getOrganisationDn(),
                Base64url.encode("this is not the correct challenge nonce".getBytes()));
        callback.setAuthorization("JWT-PoP " + responseJwt);

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "apu is incorrect")
    public void shouldRejectIfRpnDoesNotMatchEncryptionValue() throws Exception {
        // Given
        final byte[] wrongRpn = new byte[16];
        callback.setAuthorization("JWT-PoP " + buildEcdheResponse("apu", "apv", Base64url.encode(wrongRpn)));

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "apu is incorrect")
    public void shouldRejectIncorrectlyFormedApuClaims() throws Exception {
        // Given
        final String apu = Utils.base64urlEncode("apu");
        final byte[] apv = thumbprint("SHA-256", serverEphemeralKey, challenge.getChallengeSignatureKey());
        callback.setAuthorization("JWT-PoP " + buildEcdheResponse(apu, Base64url.encode(apv), Base64url.encode(RPN)));

        // When
        verifier.verify(callback);
    }

    @Test(expectedExceptions = AuthLoginException.class,
            expectedExceptionsMessageRegExp = "apv is incorrect")
    public void shouldRejectIncorrectlyFormedApvClaims() throws Exception {
        // Given
        final byte[] apu = binaryConcat(thumbprint("SHA-256", confirmationKey),
                Base64url.decode(clientEphemeralKey.getX()), Base64url.decode(clientEphemeralKey.getY()), RPN);
        final String apv = "apv";
        callback.setAuthorization("JWT-PoP " + buildEcdheResponse(Base64url.encode(apu), apv, Base64url.encode(RPN)));

        // When
        verifier.verify(callback);
    }

    @Test
    public void shouldAcceptCorrectlyEncryptedAndSignedResponse() throws Exception {
        // Given
        final byte[] apu = binaryConcat(thumbprint("SHA-256", confirmationKey),
                Base64url.decode(clientEphemeralKey.getX()), Base64url.decode(clientEphemeralKey.getY()), RPN);
        final byte[] apv = binaryConcat(thumbprint("SHA-256", challenge.getChallengeSignatureKey()),
                Base64url.decode(serverEphemeralKey.getX()), Base64url.decode(serverEphemeralKey.getY()), RVN);
        callback.setAuthorization("JWT-PoP " + buildEcdheResponse(Base64url.encode(apu), Base64url.encode(apv),
                Base64url.encode(RPN)));

        // When
        final JwtClaimsSet claims = verifier.verify(callback);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getIssuer()).isEqualTo(challenge.getAudience());
    }

    private String buildEcdheResponse(String apu, String apv, String rpn) throws Exception {
        final JwtBuilderFactory jbf = new JwtBuilderFactory();
        final SigningManager signingManager = new SigningManager();

        final byte[] apuBytes = Base64url.decode(apu);
        final byte[] apvBytes = Base64url.decode(apv);

        final SigningHandler signingHandler = signingManager.newSigningHandler(confirmationKey);
        final ByteBuffer signingInput = ByteBuffer.allocate(apuBytes.length + apvBytes.length)
                .put(apuBytes).put(apvBytes);
        final byte[] signature = signingHandler.sign(JwsAlgorithm.ES256, signingInput.array());

        verifier = new ChallengeResponseVerifier(serverEphemeralKey, confirmationKey, challenge);

        final JwtClaimsSet claims = jbf.claims()
                .iat(challenge.getIssuedAt())
                .exp(challenge.getExpiresAt())
                .aud(Collections.singletonList(challenge.getOrganisationDn()))
                .iss(challenge.getAudience())
                .claim(Challenge.PROVER_NONCE_CLAIM, rpn)
                .claim(Challenge.VERIFIER_NONCE_CLAIM, challenge.getChallengeNonce())
                .claim("sig", Base64url.encode(signature))
                .build();

        return jbf.jwe(serverEphemeralKey.toECPublicKey())
                .headers()
                    .alg(JweAlgorithm.ECDH_ES)
                    .enc(EncryptionMethod.A128GCM)
                    .epk(clientEphemeralKey)
                    .apu(apu)
                    .apv(apv)
                .done()
                .claims(claims)
                .build();
    }

    private String buildResponse(Key encryptionKey, JWK signingKey, Date expiry, Date issuedAt,
                                 String issuer, String audience, String nonce) throws Exception {
        final JwtBuilderFactory jbf = new JwtBuilderFactory();
        final SigningManager signingManager = new SigningManager();

        final SigningHandler signingHandler = signingManager.newSigningHandler(signingKey);
        final ByteBuffer signingInput = ByteBuffer.allocate(80 + Base64url.decode(nonce).length)
                .put(JwkUtils.thumbprint("SHA-256", signingKey))
                .put(RPN)
                .put(JwkUtils.thumbprint("SHA-256", challenge.getChallengeSignatureKey()))
                .put(Base64url.decode(nonce));
        final byte[] signature = signingHandler.sign((JwsAlgorithm) signingKey.getJwaAlgorithm(), signingInput.array());

        final JwtClaimsSet claims = jbf.claims()
                .claim(Challenge.VERIFIER_NONCE_CLAIM, nonce)
                .claim(Challenge.PROVER_NONCE_CLAIM, Base64url.encode(RPN))
                .claim("sig", Base64url.encode(signature))
                .iat(issuedAt)
                .exp(expiry)
                .aud(Collections.singletonList(audience))
                .iss(issuer)
                .build();

        if (encryptionKey != null) {
            return jbf.jwe(encryptionKey)
                    .headers()
                        .alg(JweAlgorithm.DIRECT)
                        .enc(EncryptionMethod.A128GCM)
                    .done()
                    .claims(claims)
                    .build();
        } else {
            return jbf.jws(new NOPSigningHandler())
                    .headers()
                        .alg(JwsAlgorithm.NONE)
                    .done()
                    .claims(claims)
                    .build();
        }
    }

    private byte[] binaryConcat(byte[]... args) {
        final int size = Arrays.stream(args).mapToInt(x -> x.length).sum();
        final ByteBuffer combined = ByteBuffer.allocate(size);
        Arrays.stream(args).forEach(combined::put);
        return combined.array();
    }
}