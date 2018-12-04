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
package org.forgerock.openam.auth.nodes.webauthn;

/**
 * Data container class for data returned by the client script.
 */
final public class ClientScriptResponse {

    private String clientData;
    private String credentialId;
    private byte[] authenticatorData;
    private byte[] signature;
    private byte[] attestationData;

    /**
     * get the client data.
     * @return the client data.
     */
    public String getClientData() {
        return clientData;
    }

    /**
     * Set the client data.
     * @param clientData the client data.
     */
    public void setClientData(String clientData) {
        this.clientData = clientData;
    }

    /**
     * Get the credential id.
     * @return the credential id.
     */
    public String getCredentialId() {
        return credentialId;
    }

    /**
     * Set the credential id.
     * @param credentialId the credential id.
     */
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    /**
     * Get the authenticator data.
     * @return the authenticator data.
     */
    public byte[] getAuthenticatorData() {
        return authenticatorData;
    }

    /**
     * Set the authenticator data.
     * @param authenticatorData the authenticator data.
     */
    public void setAuthenticatorData(byte[] authenticatorData) {
        this.authenticatorData = authenticatorData;
    }

    /**
     * Get the signature.
     * @return the signature.
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Set the signature.
     * @param signature the signature.
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Get the attestation data.
     * @return the attestation data.
     */
    public byte[] getAttestationData() {
        return attestationData;
    }

    /**
     * Set the attestation data.
     * @param attestationData the attestation data.
     */
    public void setAttestationData(byte[] attestationData) {
        this.attestationData = attestationData;
    }
}
