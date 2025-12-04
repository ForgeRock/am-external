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
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayload;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayloadEntry;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayloadMonitor;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.x509.CertificateUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FidoMetadataV3Processor} will attempt to resolve the location of the {@link Jwt} and
 * present the contents of it to the user, based on the
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html">FIDO
 * Metadata Service V3 Specification</a>.
 */
class FidoMetadataV3Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FidoMetadataV3Processor.class);

    private final MetadataBlobPayloadDownloader blobPayloadDownloader;
    private final String mdsBlobEndpoint;
    private final TrustAnchorValidator trustAnchorValidator;
    private final ExponentialBackoff exponentialBackoff;

    private MetadataBlobPayloadMonitor payloadMonitor;
    private List<MetadataEntry> cachedMetadataEntries;
    /**
     * Constructor.
     * @param blobPayloadDownloader the blob payload downloader
     * @param mdsBlobEndpoint       the details as to where to find the blob
     * @param trustAnchorValidator  used to validate the blob certificates
     */
    FidoMetadataV3Processor(MetadataBlobPayloadDownloader blobPayloadDownloader, String mdsBlobEndpoint,
            TrustAnchorValidator trustAnchorValidator) {
        this.blobPayloadDownloader = blobPayloadDownloader;
        this.mdsBlobEndpoint = mdsBlobEndpoint;
        this.trustAnchorValidator = trustAnchorValidator;
        this.exponentialBackoff = new ExponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(1));
    }

    /**
     * After resolving the locations of the provided parameters, attempt
     * to resolve the {@link SignedJwt} and perform certificate verification against the
     * resolved certificates that make up the {@link CertPath}, returning a list of metadata entries.
     *
     * @return a list of {@link MetadataEntry} that were successfully processed
     * @throws MetadataException if there is any error in processing, then the cause of
     *                           the error will be established to as specific an error as possible to assist
     *                           the user with debugging the issue
     */
    List<MetadataEntry> process() throws MetadataException {
        if (!shouldRefreshCache()) {
            return cachedMetadataEntries;
        }

        if (!exponentialBackoff.isReady()) {
            throw new MetadataException("The FIDO Metadata Service is currently unavailable");
        }

        try {
            final MetadataBlobPayload payload = blobPayloadDownloader.downloadMetadataPayload(mdsBlobEndpoint,
                    trustAnchorValidator);

            // Note Atos CardOS FIDO2 certificates have an issue in that the unique serial number is not unique.
            // This is an issue with the FIDO metadata set and not the implementation.

            // Filter for FIDO2 protocol family and FIDO_CERTIFIED authenticators and map to MetadataEntry items
            cachedMetadataEntries = payload.entries().stream()
                    .filter(entry -> entry.metadataStatement().protocolFamily().equals("fido2"))
                    .map(this::createMetadataEntry)
                    .toList();

            payloadMonitor = payload.monitor(mdsBlobEndpoint);
            exponentialBackoff.reset();
            return cachedMetadataEntries;
        } catch (MetadataException e) {
            exponentialBackoff.error();
            throw e;
        }
    }

    private boolean shouldRefreshCache() {
        return payloadMonitor == null || payloadMonitor.isExpired() || CollectionUtils.isEmpty(cachedMetadataEntries);
    }

    private MetadataEntry createMetadataEntry(MetadataBlobPayloadEntry entry) {
        int numCertificates = entry.metadataStatement().attestationRootCertificates().size();
        AtomicInteger i = new AtomicInteger(1);
        List<X509Certificate> certificates = entry.metadataStatement().attestationRootCertificates().stream()
                .map(certificatePath -> {
                    try {
                        X509Certificate certificate = CertificateUtils.readCertificate(new ByteArrayInputStream(
                                Base64.getDecoder().decode(sanitiseCertificate(certificatePath))));
                        LOGGER.info(format("[OK]        [{1}/{2}]  {0}",
                                entry.metadataStatement().description(), i.getAndIncrement(), numCertificates));
                        return certificate;
                    } catch (Exception e) {
                        LOGGER.warn(format("[FAILED]    [{1}/{2}]  {0}  {3}",
                                entry.metadataStatement().description(), i.getAndIncrement(), numCertificates,
                                e.getMessage()));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return new MetadataEntry(new Aaguid(entry.aaguid()), certificates,
                new AuthenticatorDetails(entry.statusReports()));
    }

    /**
     * Basic sanitisation is performed as there is a flaw in the current FIDO metadata set, in which
     * OneSpan DIGIPASS FX1 BIO certificate contains an illegal tab character.
     *
     * @param certificatePath the certificate to sanitise
     * @return the sanitised certificate
     */
    private static String sanitiseCertificate(String certificatePath) {
        return certificatePath.replaceFirst("\\t", "");
    }
}
