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
package org.forgerock.openam.auth.nodes.webauthn.data;

/**
 * Represents https://www.w3.org/TR/webauthn/#authenticator-data.
 */
public class AuthData {

    /** SHA-256 hash of RP ID. **/
    public final byte[] rpIdHash;

    /** flags for intent. **/
    public final AttestationFlags attestationFlags;

    /** https://www.w3.org/TR/webauthn/#signature-counter. **/
    public final int signCount;

    /** https://www.w3.org/TR/webauthn/#attested-credential-data. **/
    public final AttestedCredentialData attestedCredentialData;

    /** the raw, undecoded, authenticator data. */
    public final byte[] rawAuthenticatorData;

    /**
     * The constructor.
     * @param rpIdHash the rpidhash.
     * @param attestationFlags the attestation flags.
     * @param signCount the sign count.
     * @param attestedCredentialData the credential data.
     * @param rawAuthenticatorData the auth data.
     */
    public AuthData(byte[] rpIdHash, AttestationFlags attestationFlags, int signCount,
                    AttestedCredentialData attestedCredentialData, byte[] rawAuthenticatorData) {
        this.rpIdHash = rpIdHash;
        this.attestationFlags = attestationFlags;
        this.signCount = signCount;
        this.attestedCredentialData = attestedCredentialData;
        this.rawAuthenticatorData = rawAuthenticatorData;
    }
}
