/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.amster;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;

import com.sun.identity.shared.debug.Debug;

/**
 * An implementation of {@link Key} that uses an SSH key.
 */
class AuthorizedKey extends Key {
    private final SigningHandler signingHandler;
    private final String publicKey;

    /**
     * Construct a new key that can be used to validate a JWT.
     *
     * @param publicKey The encoded string representation of the public key.
     * @param signingHandler The signing handler that represents the key for validating Signed JWTs
     * @param options The OpenSSH-compatible options string.
     * @param debug For debugging.
     * @throws IOException If the options are invalid.
     */
    AuthorizedKey(String publicKey, SigningHandler signingHandler, String options, Debug debug) throws IOException {
        super(debug, readSshKeyOptions(options == null ? "" : options));
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
            debug.message("Could not verify signature for jwt {} against public key {}", jwt, publicKey);
            return false;
        }
    }

}
