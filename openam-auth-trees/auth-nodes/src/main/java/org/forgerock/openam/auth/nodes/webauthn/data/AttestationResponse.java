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

package org.forgerock.openam.auth.nodes.webauthn.data;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;

/**
 * Wrapper for both the attestation object and its validation type.
 */
public class AttestationResponse {

    private final AttestationObject attestationObject;
    private final AttestationType attestationType;

    /**
     * Construct a new AttestationResponse.
     *
     * @param attestationObject The attestation object that was used during attestation.
     * @param attestationType The type of attestation achieved by this attestation.
     */
    public AttestationResponse(AttestationObject attestationObject, AttestationType attestationType) {
        this.attestationObject = attestationObject;
        this.attestationType = attestationType;
    }

    /**
     * Get the underlying attestation object.
     *
     * @return The attestation object.
     */
    public AttestationObject getAttestationObject() {
        return attestationObject;
    }

    /**
     * Get the attestation type which has been validated by the appropriate verifier.
     *
     * @return the attestation type.
     */
    public AttestationType getAttestationType() {
        return attestationType;
    }
}
