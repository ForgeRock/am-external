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

import java.security.cert.X509Certificate;

/**
 * Encapsulates the response from a verifying an attestation. A response contains attestation information and
 * and potentially contains error info. See https://www.w3.org/TR/webauthn/#attestation-trust-path for more info.
 */
public class VerificationResponse {

    private AttestationType attestationType;
    private boolean isValid;
    private X509Certificate certificate;

    /**
     * Constructor.
     * @param attestationType type of attestation returned.
     * @param isValid if the attestation is valid.
     * @param certificate the trust path cert - nullable.
     */
    VerificationResponse(AttestationType attestationType, boolean isValid, X509Certificate certificate) {
        this.attestationType = attestationType;
        this.isValid = isValid;
        this.certificate = certificate;
    }

    /**
     * Get the attestation type.
     * @return the attestation type.
     */
    public AttestationType getAttestationType() {
        return attestationType;
    }

    /**
     * Return true if the attestation was verified as valid.
     * @return true if valid.
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Certificates if the trust path was x5c. Can be null.
     * @return the certs.
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * Returns a default failed response.
     * @return a default failed response.
     */
    public static VerificationResponse failure() {
        return new VerificationResponse(AttestationType.NONE, false, null);
    }
}
