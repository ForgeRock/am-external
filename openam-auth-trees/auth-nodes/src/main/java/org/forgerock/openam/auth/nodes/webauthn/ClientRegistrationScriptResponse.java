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
 * Container for the data returned during a webauthn device registration from the user agent.
 */
public class ClientRegistrationScriptResponse extends ClientScriptResponse {

    private final byte[] attestationData;
    private final String deviceName;

    /**
     * Constructor for registration script data.
     *
     * @param clientData the clientData from the device
     * @param attestationData the attestationData from the device
     * @param credentialId the identifier of this credential
     * @param deviceName the device name for the device
     */
    public ClientRegistrationScriptResponse(String clientData, byte[] attestationData,
                                            String credentialId, String deviceName) {
        super(clientData, credentialId);

        this.attestationData = attestationData;
        this.deviceName = deviceName;
    }

    /**
     * Retrieve the raw bytes of the attestation data.
     *
     * @return the attestation data for this registration.
     */
    public byte[] getAttestationData() {
        return attestationData;
    }

    /**
     * Retrieve the device name.
     * @return the device name.
     */
    public String getDeviceName() {
        return deviceName;
    }
}
