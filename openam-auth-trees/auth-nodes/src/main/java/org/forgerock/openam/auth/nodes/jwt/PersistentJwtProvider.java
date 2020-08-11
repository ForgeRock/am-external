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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.jwt;

import java.io.FileNotFoundException;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.encode.Base64;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Provides persistent cookie jwts.
 */
public class PersistentJwtProvider {

    private static final int MINIMUM_SIGNING_KEY_LENGTH = 32;
    private static final String TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY = "tokenIdleTimeSeconds";

    private AMKeyProvider amKeyProvider;
    private AuthKeyFactory authKeyFactory;
    private JwtReconstruction jwtReconstruction;

    /**
     * Constructs a PersistentJwtProvider.
     *
     * @param authKeyFactory the auth key factory.
     * @param jwtReconstruction the jwt builder.
     */
    @Inject
    public PersistentJwtProvider(AuthKeyFactory authKeyFactory, JwtReconstruction jwtReconstruction) {
        this.authKeyFactory = authKeyFactory;
        this.jwtReconstruction = jwtReconstruction;
        this.amKeyProvider = InjectorHolder.getInstance(AMKeyProvider.class);
    }

    /**
     * Returns a valid, decrypted Jwt. A jwt is 'valid' if its signature has been verified and the current time is
     * within the jwt's idle and expiry time.
     *
     * @param jwtString The jwt as a String.
     * @param orgName the org name.
     * @param hmacKey the hmac key.
     * @return a valid, decrypted Jwt.
     * @throws InvalidPersistentJwtException thrown if jwt parsing or validation fails.
     */
    public Jwt getValidDecryptedJwt(String jwtString, String orgName, String hmacKey)
            throws InvalidPersistentJwtException {
        Key privateKey;
        if (jwtString == null) {
            throw new InvalidPersistentJwtException("jwtString is null");
        }
        try {
            privateKey = authKeyFactory.getPrivateAuthKey(amKeyProvider, orgName);
        } catch (SSOException | SMSException | FileNotFoundException e) {
            throw new InvalidPersistentJwtException("error getting keys from store");
        }
        SignedEncryptedJwt jwt;
        try {
            jwt = jwtReconstruction.reconstructJwt(jwtString, SignedEncryptedJwt.class);
        } catch (InvalidJwtException e) {
            throw new InvalidPersistentJwtException("jwt reconstruction error");
        }
        if (jwt == null) {
            throw new InvalidPersistentJwtException("jwt reconstruction error");
        }
        validateSignature(jwt, hmacKey);
        jwt.decrypt(privateKey);
        if (!isWithinIdleAndExpiryTime(jwt)) {
            throw new InvalidPersistentJwtException("jwt is not within expiry or idle time");
        }
        return jwt;
    }

    private void validateSignature(SignedEncryptedJwt jwt, String hmacKey) throws InvalidPersistentJwtException {
        final byte[] signingKey = Base64.decode(hmacKey);
        if (signingKey == null || signingKey.length < MINIMUM_SIGNING_KEY_LENGTH) {
            throw new InvalidPersistentJwtException("Signing key must be at least 256-bits base64 encoded");
        }
        if (!jwt.verify(new HmacSigningHandler(signingKey))) {
            throw new InvalidPersistentJwtException("failed to verify jwt signature");
        }
    }

    private boolean isWithinIdleAndExpiryTime(Jwt jwt) {
        JwtClaimsSet jwtClaimsSet = jwt.getClaimsSet();
        Long expiryTime = jwtClaimsSet.getExpirationTime().getTime();
        Long tokenIdleTimeMillis = TimeUnit.SECONDS.toMillis(
                jwtClaimsSet.getClaim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Number.class).longValue());
        Long tokenIdleTime = new Date(tokenIdleTimeMillis).getTime();
        long nowTime = new Date(Time.currentTimeMillis()).getTime();
        return expiryTime > nowTime && tokenIdleTime > nowTime;
    }
}


