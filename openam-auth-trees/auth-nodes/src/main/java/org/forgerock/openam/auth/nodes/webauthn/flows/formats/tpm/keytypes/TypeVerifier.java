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

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import java.io.DataInputStream;
import java.io.IOException;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmAlg;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmtPublicException;

/**
 * Functional interface for verifying a JWK against the provided pubArea's unique field.
 */
public interface TypeVerifier {

    /**
     * Verify the provided JWK and the pubArea unique field.
     *
     * @param publicKey The public key information in JWK format.
     * @param unique PubArea unique field, abstracted behind an interface.
     * @return true if valid, false if not.
     */
    boolean verify(JWK publicKey, TpmtUniqueParameter unique);

    /**
     * Retrieve the scheme algorithm this type verifier instance uses.
     *
     * @return the TpmAlg for the scheme algorithm.
     */
    TpmAlg getScheme();

    /**
     * Retrieve the symmetric algorithm value of this type verifier instance uses.
     *
     * @return the TpmAlg for the symmetric algorithm.
     */
    TpmAlg getSymmetric();

    /**
     * Retrieve the pubArea unique parameter value.
     *
     * @param pubArea the pubArea part of TPM attestation data
     * @return The pubArea unique parameter
     * @throws InvalidTpmtPublicException If an invalid pubArea is identified
     * @throws IOException If an error occurs reading data from the pubArea
     */
    TpmtUniqueParameter getUniqueParameter(DataInputStream pubArea) throws InvalidTpmtPublicException, IOException;
}
