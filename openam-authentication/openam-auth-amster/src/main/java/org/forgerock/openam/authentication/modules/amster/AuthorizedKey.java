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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.amster;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link Key} that uses an SSH key.
 */
class AuthorizedKey extends Key {
    private final Logger debug = LoggerFactory.getLogger(AuthorizedKey.class);
    private final SigningHandler signingHandler;
    private final String publicKey;

    /**
     * Construct a new key that can be used to validate a JWT.
     *
     * @param publicKey The encoded string representation of the public key.
     * @param signingHandler The signing handler that represents the key for validating Signed JWTs
     * @param options The OpenSSH-compatible options string.
     * @throws IOException If the options are invalid.
     */
    AuthorizedKey(String publicKey, SigningHandler signingHandler, String options) throws IOException {
        super(readSshKeyOptions(options == null ? "" : options));
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key is required");
        }
        if (signingHandler == null) {
            throw new IllegalArgumentException("Signing handler is required");
        }
        this.signingHandler = signingHandler;
        this.publicKey = publicKey;
    }

    private static Map<String, String> readSshKeyOptions(String optionsString) throws IOException {
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        StringReader source = new StringReader(optionsString);
        StringBuilder key = new StringBuilder();
        boolean escaped = false;
        StringBuilder value = null;
        int i;
        while ((i = source.read()) != -1) {
            char c = (char) i;
            if (value == null) {
                if (c == '=') {
                    value = new StringBuilder();
                    source.read(); // consume quotes
                } else if (c == ',') {
                    key = new StringBuilder();
                } else {
                    key.append(c);
                }
            } else {
                if (c == '\\' && !escaped) {
                    escaped = true;
                } else if (c == '"' && !escaped) {
                    options.put(key.toString(), value.toString());
                    key = new StringBuilder();
                    value = null;
                    source.read(); // consume comma
                } else {
                    value.append(c);
                    escaped = false;
                }
            }
        }
        return options;
    }

    @Override
    public boolean isSignatureValid(SignedJwt jwt) {
        try {
            return publicKey.equals(jwt.getHeader().getKeyId()) && jwt.verify(signingHandler);
        } catch (IllegalArgumentException | JwsSigningException e) {
            debug.debug("Could not verify signature for jwt {} against public key {}", jwt, publicKey);
            return false;
        }
    }

}
