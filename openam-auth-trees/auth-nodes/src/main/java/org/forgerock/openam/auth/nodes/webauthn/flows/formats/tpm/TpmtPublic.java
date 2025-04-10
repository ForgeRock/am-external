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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmtPublicException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes.EccTypeVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes.RsaTypeVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes.TpmtUniqueParameter;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes.TypeVerifier;

/**
 * This class represents a method of, and the result of parsing the webAuthn-passed 'pubArea' field into an
 * object containing information about the algorithms used within the attestation process.
 */
final class TpmtPublic {

    // _LNG is short for "length", as in the number of bytes this item takes up
    static final int RSA_PARAMETER_LNG = 10;
    static final int ECC_PARAMETER_LNG = 8;

    final TpmAlg type;
    final TpmAlg nameAlg;
    final int objectAttrs;
    final byte[] authPolicy;
    final TypeVerifier typeVerifier;
    final TpmtUniqueParameter unique;

    private TpmtPublic(TpmAlg type, TpmAlg nameAlg, int objectAttrs, byte[] authPolicy, TypeVerifier typeVerifier,
            TpmtUniqueParameter unique) {
        this.type = type;
        this.nameAlg = nameAlg;
        this.objectAttrs = objectAttrs;
        this.authPolicy = authPolicy;
        this.typeVerifier = typeVerifier;
        this.unique = unique;
    }

    static TpmtPublic toTpmtPublic(byte[] publicArea) throws InvalidTpmtPublicException {
        try (DataInputStream pubArea = new DataInputStream(new ByteArrayInputStream(publicArea))) {
            TpmAlg type = TpmAlg.getTpmAlg(pubArea.readShort());
            TpmAlg name = TpmAlg.getTpmAlg(pubArea.readShort());
            int objectAttrs = pubArea.readInt();

            int authPolicyLength = pubArea.readShort();
            byte[] authPolicy = new byte[authPolicyLength];
            pubArea.read(authPolicy, 0, authPolicyLength);

            TypeVerifier parameters;
            if (type == TpmAlg.TPM_ALG_RSA) {
                byte[] rsaBytes = new byte[RSA_PARAMETER_LNG];
                pubArea.read(rsaBytes, 0, RSA_PARAMETER_LNG);
                parameters = RsaTypeVerifier.parseToType(rsaBytes);
            } else if (type == TpmAlg.TPM_ALG_ECC) {
                byte[] eccBytes = new byte[ECC_PARAMETER_LNG];
                pubArea.read(eccBytes, 0, ECC_PARAMETER_LNG);
                parameters = EccTypeVerifier.parseToType(eccBytes);
            } else {
                throw new InvalidTpmtPublicException("Type alg was not one of RSA or ECC");
            }

            TpmtUniqueParameter unique = parameters.getUniqueParameter(pubArea);

            return new TpmtPublic(type, name, objectAttrs, authPolicy, parameters, unique);
        } catch (IOException ioe) {
            throw new InvalidTpmtPublicException("Unable to parse TpmtPublic by spec.");
        }
    }

}
