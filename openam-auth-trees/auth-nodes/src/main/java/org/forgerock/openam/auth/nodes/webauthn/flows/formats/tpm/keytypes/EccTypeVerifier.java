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

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmAlg;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmEccCurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a method of, and the result of parsing ECC information contained within the pubArea.
 */
public final class EccTypeVerifier implements TypeVerifier {

    private final Logger logger = LoggerFactory.getLogger(EccTypeVerifier.class);

    private final TpmAlg symmetric;
    private final TpmAlg scheme;
    private final TpmEccCurve curveId;
    private final TpmAlg kdf;

    private EccTypeVerifier(TpmAlg symmetric, TpmAlg scheme, TpmEccCurve curveId, TpmAlg kdf) {
        this.symmetric = symmetric;
        this.scheme = scheme;
        this.curveId = curveId;
        this.kdf = kdf;
    }

    @Override
    public boolean verify(JWK publicKey, byte[] unique) {

        if (curveId.getSupportedEllipticCurve() == null) {
            logger.error("Attempted to use an unsupported curveId: {}", curveId);
            return false;
        }

        if (publicKey.getKeyType() != KeyType.EC) {
            logger.error("Key type must be EC, but was {}", publicKey.getKeyType());
            return false;
        }

        if (kdf != TpmAlg.TPM_ALG_NULL) {
            logger.error("Key derivative function must be NULL, but was {}", kdf);
            return false;
        }

        EcJWK ecJwk = EcJWK.parse(publicKey.toJsonValue());

        if (curveId.getSupportedEllipticCurve() != ecJwk.getEllipticCurve()) {
            logger.error("CurveId was unsupported: {}. Expected to match: {}", curveId.getSupportedEllipticCurve(),
                    ecJwk.getEllipticCurve());
            return false;
        }

        //todo: compare coordinates to unique

        return true;
    }

    /**
     * Retrieve the curve Id.
     *
     * @return the curve id.
     */
    public TpmEccCurve getCurveId() {
        return curveId;
    }

    /**
     * Retrieve the key derivative function.
     *
     * @return the key derivative function.
     */
    public TpmAlg getKdf() {
        return kdf;
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
     * @return An ECC type verifier.
     * @throws IOException if the structure was unable to be parsed
     */
    public static EccTypeVerifier parseToType(byte[] parameters) throws IOException {
        try (DataInputStream params = new DataInputStream(new ByteArrayInputStream(parameters))) {
            TpmAlg symmetric = TpmAlg.getTpmAlg(params.readShort());
            TpmAlg scheme = TpmAlg.getTpmAlg(params.readShort());
            TpmEccCurve curveId = TpmEccCurve.getTpmEccCurve(params.readShort());
            TpmAlg kdf = TpmAlg.getTpmAlg(params.readShort());
            return new EccTypeVerifier(symmetric, scheme, curveId, kdf);
        }
    }
}
