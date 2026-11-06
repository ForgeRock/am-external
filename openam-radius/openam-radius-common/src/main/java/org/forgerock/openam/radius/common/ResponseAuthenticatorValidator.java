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
 * Copyright 2026 ForgeRock AS.
 */
package org.forgerock.openam.radius.common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.forgerock.util.Reject;

/**
 * Validates the Response Authenticator of a RADIUS response as described in section 3 of RFC 2865.
 *
 * <p>The Response Authenticator is the MD5 hash of the response packet with the Request Authenticator of the
 * originating request in place of its own Authenticator field, followed by the shared secret. As the shared secret
 * is required to produce it, a response bearing a valid Response Authenticator can only have been sent by a server
 * in possession of that secret.
 */
public class ResponseAuthenticatorValidator {

    /**
     * Indicates whether the given response carries the Response Authenticator expected for the given request.
     *
     * @param response the response {@link Packet} to validate.
     * @param requestAuthenticator the {@link Authenticator} of the request that produced the response.
     * @param secret the secret shared between this client and the remote server.
     * @return {@code true} if the response bears the expected Response Authenticator.
     */
    public boolean isValid(Packet response, Authenticator requestAuthenticator, String secret) {
        Reject.ifNull(response, "response must not be null");
        Reject.ifNull(requestAuthenticator, "requestAuthenticator must not be null");
        Reject.ifBlank(secret, "secret must not be blank");

        Authenticator responseAuthenticator = response.getAuthenticator();
        if (responseAuthenticator == null) {
            return false;
        }

        byte[] expected = compute(response, requestAuthenticator, secret);
        return MessageDigest.isEqual(expected, responseAuthenticator.getOctets());
    }

    private byte[] compute(Packet response, Authenticator requestAuthenticator, String secret) {
        Authenticator responseAuthenticator = response.getAuthenticator();
        try {
            // The hash is taken over the response as it stood before the server replaced the request's authenticator
            // with its own, so substitute the request authenticator back in for the duration of the calculation.
            response.setAuthenticator(requestAuthenticator);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(response.getOctets());
            md5.update(secret.getBytes("UTF-8"));
            return md5.digest();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to compute RADIUS Response Authenticator", e);
        } finally {
            response.setAuthenticator(responseAuthenticator);
        }
    }
}
