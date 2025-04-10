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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayload;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayloadEntry;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayloadMonitor;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.statement.MetadataStatement;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

public class FidoMetadataV3ProcessorTest {
    @Mock
    private TrustAnchorValidator trustAnchorValidator;
    @Mock
    private MetadataBlobPayloadDownloader metadataBlobPayloadDownloader;
    @Mock
    private MetadataBlobPayload metadataBlobPayload;
    @Mock
    private MetadataBlobPayloadMonitor metadataBlobPayloadMonitor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MetadataBlobPayloadEntry metadataBlobPayloadEntry;

    private String aaguid;

    @BeforeEach
    void setup() throws Exception {
        openMocks(this);

        TimeTravelUtil.useFastForwardClock();
        doReturn(List.of(metadataBlobPayloadEntry, metadataBlobPayloadEntry)).when(metadataBlobPayload).entries();
        MetadataStatement metadataStatement = metadataBlobPayloadEntry.metadataStatement();
        doReturn("fido2").when(metadataStatement).protocolFamily();
        aaguid = UUID.randomUUID().toString();
        doReturn(aaguid).when(metadataBlobPayloadEntry).aaguid();
        doReturn(Collections.emptyList()).when(metadataBlobPayloadEntry).statusReports();
        doReturn(metadataBlobPayloadMonitor).when(metadataBlobPayload).monitor(any());
    }

    @AfterEach
    void tearDown() {
        TimeTravelUtil.resetClock();
    }

    @Test
    void shouldNegativeCache() throws Exception {
        FidoMetadataV3Processor processor = new FidoMetadataV3Processor(metadataBlobPayloadDownloader,
                "https://mds.localtest.me", trustAnchorValidator);

        doThrow(new MetadataException("boom!")).when(metadataBlobPayloadDownloader)
                .downloadMetadataPayload(any(), any());

        assertThrows(MetadataException.class, processor::process, "boom!");
        // Assert not ready for use
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");

        verify(metadataBlobPayloadDownloader, times(1)).downloadMetadataPayload(any(), any());
    }

    @Test
    void shouldNegativeCacheAndRelease() throws Exception {
        FidoMetadataV3Processor processor = new FidoMetadataV3Processor(metadataBlobPayloadDownloader,
                "https://mds.localtest.me", trustAnchorValidator);

        doThrow(new MetadataException("boom!")).when(metadataBlobPayloadDownloader)
                .downloadMetadataPayload(any(), any());

        assertThrows(MetadataException.class, processor::process, "boom!");
        // Assert not ready for use
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");

        // Time travel and assert now ready for use but still invalid
        TimeTravelUtil.fastForward(1, TimeUnit.SECONDS);
        assertThrows(MetadataException.class, processor::process, "boom!");
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");

        // Time travel and assert still not ready as retry time has increased
        TimeTravelUtil.fastForward(1, TimeUnit.SECONDS);
        assertThrows(MetadataException.class, processor::process,
                "The FIDO Metadata Service is currently unavailable");

        // Time travel and assert now ready for use but still invalid
        TimeTravelUtil.fastForward(1, TimeUnit.SECONDS);
        assertThrows(MetadataException.class, processor::process, "boom!");

        verify(metadataBlobPayloadDownloader, times(3)).downloadMetadataPayload(any(), any());
    }

    @Test
    void shouldPositiveCache() throws MetadataException {
        FidoMetadataV3Processor processor = new FidoMetadataV3Processor(metadataBlobPayloadDownloader,
                "https://mds.localtest.me", trustAnchorValidator);

        when(metadataBlobPayloadDownloader.downloadMetadataPayload(any(), any())).thenReturn(metadataBlobPayload);

        List<MetadataEntry> entries1 = processor.process();
        List<MetadataEntry> entries2 = processor.process();

        assertThat(entries1.size()).isEqualTo(2);
        assertThat(entries1.get(0).aaguid().toString()).isEqualTo(aaguid);
        assertThat(entries1).isEqualTo(entries2);
        assertThat(entries1).isSameAs(entries2);
        verify(metadataBlobPayloadDownloader, times(1)).downloadMetadataPayload(any(), any());
    }

    @Test
    void shouldPositiveCacheReset() throws MetadataException {
        FidoMetadataV3Processor downloader = new FidoMetadataV3Processor(metadataBlobPayloadDownloader,
                "https://mds.localtest.me", trustAnchorValidator);
        doReturn(false).when(metadataBlobPayloadMonitor).isExpired();

        when(metadataBlobPayloadDownloader.downloadMetadataPayload(any(), any())).thenReturn(metadataBlobPayload);

        // Test caching results in the data only being downloaded once
        downloader.process();
        downloader.process();
        verify(metadataBlobPayloadDownloader, times(1)).downloadMetadataPayload(any(), any());

        // Test that out of data results in attempts are made to download the latest data
        doReturn(true).when(metadataBlobPayloadMonitor).isExpired();
        downloader.process();
        downloader.process();
        downloader.process();
        verify(metadataBlobPayloadDownloader, times(4)).downloadMetadataPayload(any(), any());

        // Test that the cache still works after it has been reset
        doReturn(false).when(metadataBlobPayloadMonitor).isExpired();
        downloader.process();
        downloader.process();
        verify(metadataBlobPayloadDownloader, times(4)).downloadMetadataPayload(any(), any());

    }
}
