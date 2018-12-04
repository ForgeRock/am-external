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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;

/**
 * Defines the interface for an Attestation format. Each format handles verification and singing it's own way.
 */
public interface AttestationVerifier {

    /**
     * Verify the attestation using specific steps defined by the attestation format.
     *
     * @param attestationObject the attestation object.
     * @param clientDataHash the hash of the client data.
     * @return true if the attestation is valid.
     */
    VerificationResponse verify(AttestationObject attestationObject, byte[] clientDataHash);
}
