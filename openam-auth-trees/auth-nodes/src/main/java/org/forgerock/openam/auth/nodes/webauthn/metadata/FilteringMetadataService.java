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
import org.forgerock.openam.auth.nodes.webauthn.FidoCertificationLevel;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * A metadata service that filters out authenticators that do not meet a specified FIDO certification level.
 * <p>
 * Calls to a {@link MetadataService} delegate to determine the authenticator details.
 */
public class FilteringMetadataService implements MetadataService {
    private final MetadataService delegate;
    private final FidoCertificationLevel fidoCertificationLevel;

    /**
     * Constructs a new FilteringMetadataService.
     *
     * @param delegate               the metadata service delegate
     * @param fidoCertificationLevel the FIDO certification level to filter by
     */
    @VisibleForTesting
    public FilteringMetadataService(MetadataService delegate, FidoCertificationLevel fidoCertificationLevel) {
        this.delegate = delegate;
        this.fidoCertificationLevel = fidoCertificationLevel;
    }

    @Override
    public AuthenticatorDetails determineAuthenticatorStatus(Aaguid deviceAaguid, List<X509Certificate> attestCerts)
            throws MetadataException {
        AuthenticatorDetails details = delegate.determineAuthenticatorStatus(deviceAaguid, attestCerts);
        if (details != null && !details.getMaxCertificationStatus().isSatisfiedBy(fidoCertificationLevel.getStatus())) {
            throw new MetadataException("Authenticator does not meet required FIDO certification level");
        }
        return details;
    }
}
