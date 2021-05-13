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

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;

/**
 * Represents https://www.w3.org/TR/webauthn/#attestation-object.
 */
public class AttestationObject {

    /** https://www.w3.org/TR/webauthn/#attestation-formats. **/
    public final AttestationVerifier attestationVerifier;
    /** https://www.w3.org/TR/webauthn/#authenticator-data. **/
    public final AuthData authData;
    /** https://www.w3.org/TR/webauthn/#attestation-statement. **/
    public final AttestationStatement attestationStatement;

    /**
     * The attestation object constructor.
     *
     * @param attestationVerifier the format of the attestation.
     * @param authData the auth data.
     * @param attestationStatement the attestation statement.
     */
    public AttestationObject(AttestationVerifier attestationVerifier, AuthData authData,
                             AttestationStatement attestationStatement) {
        this.attestationVerifier = attestationVerifier;
        this.authData = authData;
        this.attestationStatement = attestationStatement;
    }
}
