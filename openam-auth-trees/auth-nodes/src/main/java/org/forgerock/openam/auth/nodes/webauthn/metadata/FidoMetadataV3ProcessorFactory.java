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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;

/**
 * Factory class for obtaining an instance of {@link FidoMetadataV3Processor}.
 */
class FidoMetadataV3ProcessorFactory {

    private final Map<DownloaderCacheKey, FidoMetadataV3Processor> downloaderCache = new ConcurrentHashMap<>();
    private final MetadataBlobPayloadDownloader metadataBlobPayloadDownloader;

    /**
     * Constructor.
     * @param metadataBlobPayloadDownloader the metadata blob payload downloader
     */
    @Inject
    FidoMetadataV3ProcessorFactory(MetadataBlobPayloadDownloader metadataBlobPayloadDownloader) {
        this.metadataBlobPayloadDownloader = metadataBlobPayloadDownloader;
    }

    /**
     * Create a new instance of the {@link FidoMetadataV3Processor} if one does not already exist for the given
     * metadata service URI and trust anchor validator.
     * @param mdsBlobEndpoint      the details as to where to find the blob
     * @param trustAnchorValidator used to validate the blob certificates
     * @return a new instance of the {@link FidoMetadataV3Processor}
     */
    FidoMetadataV3Processor create(String mdsBlobEndpoint, TrustAnchorValidator trustAnchorValidator) {
        DownloaderCacheKey key = new DownloaderCacheKey(mdsBlobEndpoint, trustAnchorValidator);
        return downloaderCache.computeIfAbsent(key, ignored ->
                new FidoMetadataV3Processor(metadataBlobPayloadDownloader, mdsBlobEndpoint, trustAnchorValidator));
    }

    private record DownloaderCacheKey(String metadataServiceUri, TrustAnchorValidator trustAnchorValidator) {
    }
}
