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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;

/**
 * Models the {@literal MetadataBlobPayload} object defined in the FIDO MDS specification.
 * <p>
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#dictdef-metadatablobpayload">
 * 3.1.6 Metadata BLOB Payload dictionary</a>
 * </p>
 */
//@Checkstyle:off JavadocType
public record MetadataBlobPayload(String legalHeader, Integer no, String nextUpdate,
                                  List<MetadataBlobPayloadEntry> entries) {

    /**
     * Returns a {@link MetadataBlobPayloadMonitor} that can be used to monitor whether the payload has expired.
     * @param mdsBlobEndpoint the endpoint the payload was downloaded from
     * @return the monitor
     * @throws MetadataException if an error occurs
     */
    public MetadataBlobPayloadMonitor monitor(String mdsBlobEndpoint) throws MetadataException {
        Instant nextUpdate = DateTimeFormatter.ISO_DATE.parse(this.nextUpdate)
                .query(LocalDate::from).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new MetadataBlobPayloadMonitor(nextUpdate, mdsBlobEndpoint);
    }
}
