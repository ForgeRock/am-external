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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

/**
 * Data container class for data returned by the client script.
 */
public abstract class ClientScriptResponse {

    private final String clientData;
    private final String credentialId;

    ClientScriptResponse(String clientData, String credentialId) {
        this.clientData = clientData;
        this.credentialId = credentialId;
    }

    /**
     * get the client data.
     *
     * @return the client data.
     */
    public String getClientData() {
        return clientData;
    }

    /**
     * Get the credential id.
     *
     * @return the credential id.
     */
    public String getCredentialId() {
        return credentialId;
    }
}
