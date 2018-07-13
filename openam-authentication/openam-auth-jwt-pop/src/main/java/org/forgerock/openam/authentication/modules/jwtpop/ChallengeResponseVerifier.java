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

import static java.security.MessageDigest.isEqual;
import static org.forgerock.json.jose.jwe.JweHeaderKey.EPK;

import java.nio.ByteBuffer;
import java.security.Key;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;
import org.forgerock.util.encode.Base64url;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;

/**
 * Verifies that a challenge response is correct and valid.
 */
final class ChallengeResponseVerifier {
    private static final int SHA256_THUMBPRINT_LEN = 32;

    private static final JwtReconstruction JWT_PARSER = new JwtReconstruction();
    private static final SigningManager SIGNING_MANAGER = new SigningManager();

    private final JWK responseDecryptionKey;
    private final SigningHandler signatureVerificationHandler;
    private final JwsAlgorithm signatureAlgorithm;
    private final JWK signatureVerificationKey;
    private final Challenge challenge;

    ChallengeResponseVerifier(JWK responseDecryptionKey, JWK signatureVerificationKey, Challenge challenge) {
        this.responseDecryptionKey = responseDecryptionKey;
        this.signatureVerificationHandler = verificationHandler(signatureVerificationKey);
        this.signatureAlgorithm = signatureAlgorithm(signatureVerificationKey);
        this.signatureVerificationKey = signatureVerificationKey;
        this.challenge = challenge;
    }

    JwtClaimsSet verify(HttpCallback response) throws AuthLoginException {
        final Date now = Time.newDate();
        if (now.after(challenge.getExpiresAt())) {
            throw new AuthLoginException("challenge has expired");
        }

        final EncryptedJwt responseJwt = parseResponse(response);
        final JwtClaimsSet claims = decryptAndVerifySignature(responseJwt);
        return validateClaims(claims, now);
    }

    private EncryptedJwt parseResponse(final HttpCallback response) throws AuthLoginException {
        if (response.getAuthorization() == null) {
            throw new AuthLoginException("no authorization header in response");
        }

        final String[] authParts = response.getAuthorization().split("\\s+", 2);
        if (authParts.length != 2 || !challenge.getAuthScheme().equalsIgnoreCase(authParts[0].trim())) {
            throw new AuthLoginException("invalid authorization header");
        }

        try {
            return JWT_PARSER.reconstructJwt(authParts[1], EncryptedJwt.class);
        } catch (InvalidJwtException e) {
            throw new AuthLoginException("invalid authorization header", e);
        } catch (ClassCastException e) {
            throw new AuthLoginException("response is not encrypted", e);
        }
    }

    private JwtClaimsSet decryptAndVerifySignature(EncryptedJwt responseJwt) throws AuthLoginException {
        try {
            final Algorithm responseAlgorithm = responseJwt.getHeader().getAlgorithm();
            final Algorithm expectedAlgorithm = JwkUtils.algorithm(responseDecryptionKey,
                    responseDecryptionKey.getKeyType() == KeyType.EC ? JweAlgorithm.ECDH_ES : JweAlgorithm.DIRECT);
            final EncryptionMethod encryptionMethod = ((JweHeader) responseJwt.getHeader()).getEncryptionMethod();

            // If they have specified encryption algorithms, then validate that they are what we expect before
            // decrypting
            if (responseAlgorithm != null && responseAlgorithm != expectedAlgorithm) {
                throw new AuthLoginException("incorrect response key exchange method");
            }

            if (encryptionMethod != null && encryptionMethod != challenge.getResponseEncryptionCipher()) {
                throw new AuthLoginException("incorrect response encryption cipher");
            }

            responseJwt.decrypt(decryptionKey(responseDecryptionKey));

            final byte[] rvn = Base64url.decode(challenge.getChallengeNonce());
            final byte[] rpn = Base64url.decode(responseJwt.getClaimsSet().getClaim(Challenge.PROVER_NONCE_CLAIM,
                    String.class));
            if (rpn.length != Challenge.NONCE_LENGTH) {
                throw new AuthLoginException("invalid rpn claim length");
            }
            if (isEqual(rvn, rpn)) {
                throw new AuthLoginException("rvn and rpn should be different");
            }

            final byte[] clientThumbprint = JwkUtils.thumbprint("SHA-256", signatureVerificationKey);
            final byte[] serverThumbprint = JwkUtils.thumbprint("SHA-256", challenge.getChallengeSignatureKey());

            ByteBuffer signingInput = null;

            if (responseDecryptionKey.getKeyType() == KeyType.EC) {
                // Using elliptic curve crypto, so recalculate and verify that the apu and apv claims are correct
                final byte[] apu = Base64url.decode(responseJwt.getHeader().get("apu").required().asString());
                final byte[] apv = Base64url.decode(responseJwt.getHeader().get("apv").required().asString());

                // Recalculate the expected apu and apv claims and compare to the provided ones. This ensures that all
                // the expected public key material has been used in the key derivation function to bind the derived
                // key cryptographically to the authentication context.

                final EcJWK clientEphemeralKey = responseJwt.getHeader().getParameter(EPK.toString(), EcJWK.class);

                // For the ESnnn algorithms, public key size == signature size
                final int publicKeySize = clientEphemeralKey.getEllipticCurve().getSignatureSize();

                final ByteBuffer expectedApu = ByteBuffer.allocate(SHA256_THUMBPRINT_LEN + publicKeySize +
                        Challenge.NONCE_LENGTH)
                        .put(clientThumbprint)
                        .put(Base64url.decode(clientEphemeralKey.getX()))
                        .put(Base64url.decode(clientEphemeralKey.getY()))
                        .put(rpn);

                final ByteBuffer expectedApv = ByteBuffer.allocate(SHA256_THUMBPRINT_LEN + publicKeySize +
                        Challenge.NONCE_LENGTH)
                        .put(serverThumbprint)
                        .put(Base64url.decode(((EcJWK) responseDecryptionKey).getX()))
                        .put(Base64url.decode(((EcJWK) responseDecryptionKey).getY()))
                        .put(rvn);

                // We probably do not need constant-time comparisons here, but it is best to be safe
                if (!isEqual(expectedApu.array(), apu)) {
                    throw new AuthLoginException("apu is incorrect");
                }

                if (!isEqual(expectedApv.array(), apv)) {
                    throw new AuthLoginException("apv is incorrect");
                }

                // Reuse the apu and apv claims as the signing input as they contain all parameters
                signingInput = ByteBuffer.allocate(apu.length + apv.length).put(apu).put(apv);
            }

            if (signingInput == null) {
                // If not using ECDHE then the signing input is the thumbprints of both parties' long-term identity
                // public keys plus both parties' random nonces.
                signingInput = ByteBuffer.allocate((SHA256_THUMBPRINT_LEN * 2) + (Challenge.NONCE_LENGTH * 2))
                        .put(clientThumbprint)
                        .put(rpn)
                        .put(serverThumbprint)
                        .put(rvn);
            }

            // Verify the signature claim
            final String signature = responseJwt.getClaimsSet().getClaim("sig", String.class);
            if (signature == null) {
                throw new AuthLoginException("no signature provided");
            }
            if (!signatureVerificationHandler.verify(signatureAlgorithm, signingInput.array(),
                    Base64url.decode(signature))) {
                throw new AuthLoginException("signature verification failed");
            }

        } catch (JweDecryptionException e) {
            throw new AuthLoginException("response decryption failed");
        } catch (IllegalArgumentException e) {
            throw new AuthLoginException("invalid signature or encryption algorithm");
        } catch (NullPointerException e) {
            throw new AuthLoginException("missing response claims");
        }

        return responseJwt.getClaimsSet();
    }


    private JwtClaimsSet validateClaims(final JwtClaimsSet claims, final Date now) throws AuthLoginException {
        if (claims.getIssuedAtTime() != null) {
            if (claims.getIssuedAtTime().before(challenge.getIssuedAt())) {
                throw new AuthLoginException("response was issued before challenge");
            }
            if (claims.getIssuedAtTime().after(now)) {
                throw new AuthLoginException("response issued in the future");
            }
        }

        if (claims.getExpirationTime() != null && now.after(claims.getExpirationTime())) {
            throw new AuthLoginException("response has expired");
        }

        if (!challenge.getAudience().equals(claims.getIssuer())) {
            throw new AuthLoginException("incorrect issuer");
        }

        if (!claims.getAudience().contains(challenge.getOrganisationDn())) {
            throw new AuthLoginException("incorrect audience");
        }

        return claims;
    }

    private static Key decryptionKey(JWK jwk) {
        Reject.ifTrue(jwk.getUse() != null && jwk.getUse() != KeyUse.ENC, "decryption key is not for encryption!");
        switch (jwk.getKeyType()) {
            case OCT:
                return new SecretKeySpec(Base64url.decode(((OctJWK) jwk).getKey()), "AES");
            case RSA:
                return ((RsaJWK) jwk).toRSAPrivateKey();
            case EC:
                return ((EcJWK) jwk).toECPrivateKey();
            default:
                throw new IllegalArgumentException("unknown decryption key type: " + jwk.getKeyType());
        }
    }

    private static SigningHandler verificationHandler(JWK jwk) {
        Reject.ifTrue(jwk.getUse() != null && jwk.getUse() != KeyUse.SIG, "verification key is not for signatures!");
        return SIGNING_MANAGER.newVerificationHandler(jwk);
    }

    private static JwsAlgorithm signatureAlgorithm(JWK jwk) {
        if (jwk.getAlgorithm() != null) {
            return JwsAlgorithm.parseAlgorithm(jwk.getAlgorithm());
        }
        switch (jwk.getKeyType()) {
            case OCT:
                final int keySize = Base64url.decode(((OctJWK) jwk).getKey()).length;
                if (keySize <= 32) {
                    return JwsAlgorithm.HS256;
                } else if (keySize <= 48) {
                    return JwsAlgorithm.HS384;
                } else {
                    return JwsAlgorithm.HS512;
                }
            case RSA:
                return JwsAlgorithm.RS256;
            case EC:
                final SupportedEllipticCurve ellipticCurve = SupportedEllipticCurve.forName(((EcJWK) jwk).getCurve());
                return ellipticCurve.getJwsAlgorithm();
            default:
                throw new IllegalArgumentException("unknown signature verification key type: " + jwk.getKeyType());
        }
    }
}
