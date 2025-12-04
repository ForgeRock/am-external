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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn;

/**
 * Container for the data returned during a webauthn device authentication from the user agent.
 */
public class ClientAuthenticationScriptResponse extends ClientScriptResponse {

    private final String userHandle;
    private final byte[] authenticatorData;
    private final byte[] signature;

    /**
     * Constructor for authentication script data.
     *
     * @param clientData the client data.
     * @param authenticatorData the raw bytes of the authenticatorData.
     * @param credentialId the credentialId;
     * @param signature the raw bytes of the signature.
     * @param userHandle the userHandle or null.
     */
    public ClientAuthenticationScriptResponse(String clientData, byte[] authenticatorData, String credentialId,
                                              byte[] signature, String userHandle) {
        super(clientData, credentialId);

        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.userHandle = userHandle;
    }

    /**
     * Get the user handle, or null if no handle was requested/supplied.
     *
     * @return the user handle, or null.
     */
    public String getUserHandle() {
        return userHandle;
    }

    /**
     * Get the raw bytes of the authenticator data.
     *
     * @return the authenticator data for this authentication.
     */
    public byte[] getAuthenticatorData() {
        return authenticatorData;
    }

    /**
     * Get the raw bytes of the signature.
     *
     * @return the signature of the authentication.
     */
    public byte[] getSignature() {
        return signature;
    }

}
