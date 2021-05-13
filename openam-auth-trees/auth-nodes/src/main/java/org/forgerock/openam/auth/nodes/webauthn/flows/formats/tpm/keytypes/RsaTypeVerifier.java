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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmAlg;

/**
 * This class represents a method of, and the result of parsing RSA information contained within the pubArea.
 */
public final class RsaTypeVerifier implements TypeVerifier {

    private static final int DEFAULT_EXPONENT = 65537;

    private final TpmAlg symmetric;
    private final TpmAlg scheme;
    private final byte[] keyBits;
    private final int exponent;

    private RsaTypeVerifier(TpmAlg symmetric, TpmAlg scheme, byte[] keyBits, int exponent) {
        this.symmetric = symmetric;
        this.scheme = scheme;
        this.keyBits = keyBits;
        this.exponent = exponent;
    }

    @Override
    public boolean verify(JWK jwk, byte[] unique) {
        //key must be of type RSA
        if (jwk.getKeyType() != KeyType.RSA) {
            return false;
        }

        RsaJWK rsaJwk = (RsaJWK) jwk;
        RSAPublicKey rsaPublicKey = rsaJwk.toRSAPublicKey();

        //exponent must match
        if (!rsaPublicKey.getPublicExponent().equals(BigInteger.valueOf(exponent))) {
            return false;
        }

        //modulus must match the pub area
        if (!rsaPublicKey.getModulus().equals(new BigInteger(1, unique))) {
            return false;
        }

        return true;
    }

    /**
     * Return the exponent of this type verifier.
     *
     * @return the exponent.
     */
    public int getExponent() {
        return exponent;
    }

    /**
     * Return the key bits of this type verifier.
     *
     * @return an array of bytes representing the key bits.
     */
    public byte[] getKeyBits() {
        return keyBits;
    }

    @Override
    public TpmAlg getScheme() {
        return scheme;
    }

    @Override
    public TpmAlg getSymmetric() {
        return symmetric;
    }

    /**
     * Parses the structure of the parameters field into an object for manipulation and verification.
     *
     * @param parameters The byte array in the specified format.
     * @return An RSA type verifier.
     * @throws IOException if the structure was unable to be parsed
     */
    public static RsaTypeVerifier parseToType(byte[] parameters) throws IOException {
        try (DataInputStream params = new DataInputStream(new ByteArrayInputStream(parameters))) {
            TpmAlg symmetric = TpmAlg.getTpmAlg(params.readShort());
            TpmAlg scheme = TpmAlg.getTpmAlg(params.readShort());
            byte[] keyBits = new byte[2];
            params.read(keyBits, 0, 2);

            int exponent = params.readInt();
            if (exponent == 0) {
                exponent = DEFAULT_EXPONENT;
            }

            return new RsaTypeVerifier(symmetric, scheme, keyBits, exponent);
        }
    }
}
