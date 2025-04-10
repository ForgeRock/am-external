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

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for checking an authenticator against the FIDO Alliance provided
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html">FIDO
 * Metadata Service (Version 3)</a>.
 */
public class FidoMetadataV3Service implements MetadataService {

    private final Logger logger = LoggerFactory.getLogger(FidoMetadataV3Service.class);
    private final CertificateFactory certFactory;
    private final List<MetadataEntry> metadataEntries;
    private final TrustAnchorValidator.Factory trustAnchorValidatorFactory;

    /**
     * Constructor for the FIDO Alliance Metadata Service.
     *
     * @param certFactory                 the certification factory to use with this metadata service.
     * @param trustAnchorValidatorFactory the trust anchor validator factory
     * @param metadataEntries             the metadata entries associated with this service
     */
    public FidoMetadataV3Service(CertificateFactory certFactory,
                                 TrustAnchorValidator.Factory trustAnchorValidatorFactory,
                                 List<MetadataEntry> metadataEntries) {
        this.certFactory = certFactory;
        this.trustAnchorValidatorFactory = trustAnchorValidatorFactory;
        this.metadataEntries = metadataEntries;
    }

    /**
     * Checks that the provided attestation certificate chain is valid and returns the {@link AuthenticatorDetails}
     * of the provided authenticator device based on the provided details.
     * <p>
     * This checks that the provided attestation certificate chain is valid against the metadata service in accordance
     * with <a href="https://www.w3.org/TR/webauthn-2/#sctn-registering-a-new-credential">
     *     WebAuthn specification for registering a new credential - Sections 7.1.20 and 7.1.21</a>.
     *
     * @param deviceAaguid device AAGUID to check against the metadata service
     * @param attestCerts  attestation certificates to check against the metadata service
     * @return the associated {@link AuthenticatorDetails} for the device
     * @throws MetadataException if no corresponding metadata entry is found for the provided AAGUID, or if there was
     * an issue processing the provided data
     */
    public AuthenticatorDetails determineAuthenticatorStatus(final Aaguid deviceAaguid,
                                                             final List<X509Certificate> attestCerts)
            throws MetadataException {
        List<MetadataEntry> entries = metadataEntries.stream()
                .filter(metadataEntry -> metadataEntry.aaguid().equals(deviceAaguid))
                .toList();

        // Metadata service has been configured but no metadata entries found for the AAGUID
        if (entries.isEmpty()) {
            logger.warn("No metadata entries found for the AAGUID {}", deviceAaguid);
            throw new MetadataException("No metadata entries found for the AAGUID");
        }

        if (entries.size() > 1) {
            logger.warn("Multiple metadata entries found for the same AAGUID {}", deviceAaguid);
            throw new MetadataException("Multiple metadata entries found for the same AAGUID");
        }

        MetadataEntry metadataEntry = entries.get(0);
        if (metadataEntry.attestationRootCertificates().isEmpty()) {
            throw new MetadataException("No attestation root certificates found associated with the AAGUID");
        }

        Set<TrustAnchor> metadataTrustAnchors = metadataEntry.attestationRootCertificates().stream()
                .map(cert -> new TrustAnchor(cert, null))
                .collect(Collectors.toSet());

        try {
            // We can't directly call the trustAnchorValidator here as we want to ensure the certificate
            // path intersects with the metadata attestation root certificates and not just the trust anchors
            TrustAnchorValidator metadataTrustAnchor = trustAnchorValidatorFactory.create(metadataTrustAnchors, false);
            if (metadataTrustAnchor.isRootCertificate(attestCerts)) {
                return metadataEntry.authenticatorDetails();
            }

            if (metadataTrustAnchor.containsTrustAnchor(attestCerts)) {
                throw new MetadataException("The certificate path contains a trust anchor certificate");
            }

            // § 7.1.21
            CertPath certPath = certFactory.generateCertPath(attestCerts);
            if (metadataTrustAnchor.isTrusted(certPath)) {
                return metadataEntry.authenticatorDetails();
            }
        } catch (CertificateException | CertPathValidatorException | InvalidAlgorithmParameterException e) {
            throw new MetadataException(e);
        }

        return metadataEntry.authenticatorDetails();
    }
}
