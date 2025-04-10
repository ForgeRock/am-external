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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import java.security.cert.X509Certificate;
import java.util.List;

import org.forgerock.openam.auth.nodes.webauthn.Aaguid;

/**
 * An interface for metadata services used to assess the validity of certificates.
 */
public interface MetadataService {
    /**
     * Determine the status of an authenticator based on the AAGUID and attestation certificates.
     *
     * @param deviceAaguid the AAGUID of the device to check against the metadata service
     * @param attestCerts  the attestation certificates to check
     * @return the associated {@link AuthenticatorDetails} for the device
     * @throws MetadataException if the device fails to be verified, or a corresponding metadata entry is not found
     */
    AuthenticatorDetails determineAuthenticatorStatus(Aaguid deviceAaguid, List<X509Certificate> attestCerts)
            throws MetadataException;
}
