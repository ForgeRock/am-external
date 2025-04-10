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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.utils.Time;

/**
 * Responsible for monitoring the metadata blob payload and determining if it has expired.
 */
public class MetadataBlobPayloadMonitor {

    private final Instant nextUpdate;
    private boolean isExpired = false;
    private WatchKey watchKey;
    private Path watchedContext;
    private Instant expiryTime;
    private final String mdsBlobEndpoint;
    private static WatchService watchService;

    /**
     * Constructor.
     * @param nextUpdate the next update time specified in the metadata blob payload
     * @param mdsBlobEndpoint the blob endpoint that the payload was retrieved from
     * @throws MetadataException if an error occurs
     */
    public MetadataBlobPayloadMonitor(Instant nextUpdate, String mdsBlobEndpoint) throws MetadataException {
        this.nextUpdate = nextUpdate;
        this.mdsBlobEndpoint = mdsBlobEndpoint;
        reset();
    }

    /**
     * Resets the expiry time.
     * @throws MetadataException if an error occurs
     */
    public void reset() throws MetadataException {
        if (mdsBlobEndpoint.startsWith("http://") || mdsBlobEndpoint.startsWith("https://")) {
            Instant now = Time.instant();
            // check if for some reason we're already past the next update time and wait an extra day so we don't
            // spam the endpoint
            expiryTime = now.isAfter(nextUpdate) ? now.plus(1, ChronoUnit.DAYS) : nextUpdate;
        } else {
            try {
                Path fullPath = new File(mdsBlobEndpoint).toPath().toAbsolutePath();
                watchedContext = fullPath.getFileName();
                watchKey = fullPath.getParent().register(getWatchService(), ENTRY_CREATE, ENTRY_DELETE,
                        ENTRY_MODIFY, OVERFLOW);
            } catch (IOException e) {
                throw new MetadataException("Failed to register listener", e);
            }
        }
        isExpired = false;
    }

    /**
     * Returns whether the payload has expired.
     * @return {@code true} if the payload has expired
     */
    public boolean isExpired() {
        if (isExpired) {
            return true;
        }

        if (expiryTime != null && Time.instant().isAfter(expiryTime)) {
            isExpired = true;
        }

        if (watchKey != null) {
            if (!watchKey.isValid()) {
                isExpired = true;
            } else {
                if (watchKey.pollEvents().stream().anyMatch(event -> event.context().equals(watchedContext))) {
                    isExpired = true;
                    watchKey.cancel();
                }
            }
        }

        return isExpired;
    }

    private static WatchService getWatchService() throws IOException {
        if (watchService == null) {
            watchService = FileSystems.getDefault().newWatchService();
        }
        return watchService;
    }
}
