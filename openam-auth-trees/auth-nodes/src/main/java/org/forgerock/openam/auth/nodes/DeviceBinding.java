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
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jwk.EllipticCurveJwk;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSetKey;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.jwt.JwtClaimsValidationHandler;
import org.forgerock.openam.jwt.JwtClaimsValidationOptions;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.shared.encode.Base64;

/**
 * Interface to provide default common methods and constants for Device Binding related Nodes.
 */
public interface DeviceBinding {

    /**
     * Logger for logging.
     */
    Logger logger = LoggerFactory.getLogger(DeviceBinding.class);

    /**
     * Secret byte length.
     */
    int SECRET_BYTE_LENGTH = 32;

    /**
     * Challenge claim.
     */
    String CHALLENGE_CLAIM = "challenge";

    /**
     * SKEW Allowance.
     */
    Duration SKEW_ALLOWANCE = Duration.duration(30, TimeUnit.SECONDS);

    /**
     * State variable name for Bounded Device.
     */
    String DEVICE = DeviceBinding.class.getSimpleName() + ".DEVICE";

    /**
     * Retrieve the value from TreeContext.
     *
     * @param node The Authentication Node
     * @param context The TreeContext
     * @param key The key in the Context
     * @return The Value
     * @throws NodeProcessException When there is no value with the provided key
     */
    default String getContextValue(Node node, TreeContext context, String key) throws NodeProcessException {
        JsonValue value = context.getStateFor(node).get(key);
        if (value == null) {
            throw new NodeProcessException(key + " missing from shared state");
        }
        return value.asString();
    }

    /**
     * Create random bytes for challenge.
     *
     * @return Secure random challenge.
     */
    default String createRandomBytes() {
        byte[] secretBytes = new byte[SECRET_BYTE_LENGTH];
        new SecureRandom().nextBytes(secretBytes);
        return Base64.encode(secretBytes);
    }

    /**
     * Return the public key from the provided JWK.
     *
     * @param jwk The JWK.
     * @return The public key
     */
    default PublicKey getPublicKey(JWK jwk) {
        if (jwk instanceof RsaJWK) {
            return ((RsaJWK) jwk).toRSAPublicKey();
        }
        if (jwk instanceof EllipticCurveJwk) {
            return ((EllipticCurveJwk) jwk).toPublicKey();
        }
        throw new IllegalArgumentException("Unsupported JWK algorithm: " + jwk.getKeyType());
    }

    /**
     * Validate the signature of the signedJwt with the provided jwk.
     *
     * @param signedJwt The JWT to be validated
     * @param jwk The JWK for verification
     * @throws NoSuchSecretException When no such secret for the Verification Key
     * @throws SignatureException Invalid signature
     * @throws InvalidKeyException Invalid Key
     */
    default void validateSignature(SignedJwt signedJwt, JWK jwk) throws NoSuchSecretException,
            SignatureException, InvalidKeyException {
        if (jwk == null) {
            throw new InvalidKeyException("JWK is missing in the Header");
        }
        VerificationKey verifier;
        verifier = new VerificationKey(new SecretBuilder()
                .stableId(signedJwt.getHeader().getKeyId())
                .expiresAt(Instant.MAX)
                .publicKey(getPublicKey(jwk)));
        if (!signedJwt.verify(new SigningManager(new SecretsProvider(Time.getClock()))
                .newVerificationHandler(verifier))) {
            throw new SignatureException("Key verification failed");
        }
    }

    /**
     * Validate JWT claim.
     *
     * @param signedJwt The JWT to be validated
     * @param challenge The signing challenge
     * @param issuers the accepted issuers
     */
    default void validateClaim(SignedJwt signedJwt, String challenge, Set<String> issuers) {
        //Validate Claim
        JwtClaimsValidationOptions<InvalidJwtException> validationOptions =
                new JwtClaimsValidationOptions<>(InvalidJwtException::new)
                        .setSkewAllowance(SKEW_ALLOWANCE)
                        .addClaimValidator(JwtClaimsSetKey.ISS.value(), v -> v.isString()
                                && issuers.contains(v.asString()))
                        .addClaimValidator(JwtClaimsSetKey.SUB.value(), JsonValue::isNotNull)
                        .addClaimValidator(CHALLENGE_CLAIM, jsonValue -> {
                            if (!challenge.equals(jsonValue.asString())) {
                                logger.error("Invalid Challenge, challenge not match, expected: {} , got {}",
                                        challenge, jsonValue.asString());
                                return false;
                            }
                            return true;
                        })
                        .setIssuerRequired(false);
        new JwtClaimsValidationHandler<>(validationOptions, signedJwt.getClaimsSet()).validateClaims();
    }

}
