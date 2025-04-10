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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils.copyClassPathDataToTempFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobReader;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobVerifier;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatus;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayload;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayloadEntry;
import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.junit.jupiter.api.Test;

public class MetadataBlobPayloadDownloaderTest {

    public static final String FIDOMDS3_BLOB_LOCATION = "FidoMetadataDownloader/fidoconformance/fido-mds3-blob.jwt";
    public static final String REVOKED_CERTIFICATE_BLOB_LOCATION =
            "FidoMetadataDownloader/fidoconformance/revoked-certificate-blob.jwt";

    @Test
    void downloadData() throws Exception {
        String blobPath = copyClassPathDataToTempFile(FIDOMDS3_BLOB_LOCATION).getAbsolutePath();

        // Test a selection of known entries from the metadata service
        Aaguid certifiedLevel1Aaguid = new Aaguid("4d41190c-7beb-4a84-8018-adf265a6352d");
        Aaguid certifiedLevel2Aaguid = new Aaguid("87dbc5a1-4c94-4dc8-8a47-97d800fd1f3c");
        Aaguid uncertifiedAaguid = new Aaguid("53414d53-554e-4700-0000-000000000000");
        Aaguid revokedAaguid = new Aaguid("ba86dc56-635f-4141-aef6-00227b1b9af6");

        //given
        MetadataBlobPayloadDownloader downloader = new MetadataBlobPayloadDownloader(getBlobVerifier(true),
                new ResourceResolver(), new BlobReader(), new JwtReconstruction());
        MetadataBlobPayload payload =  downloader.downloadMetadataPayload(blobPath, mock(TrustAnchorValidator.class));

        //gives
        List<MetadataBlobPayloadEntry> metadataEntries = payload.entries();
        assertEquals(222, metadataEntries.size());

        assertEquals(AuthenticatorStatus.FIDO_CERTIFIED_L1,
                getAuthenticatorStatus(metadataEntries, certifiedLevel1Aaguid));

        assertEquals(AuthenticatorStatus.FIDO_CERTIFIED_L2,
                getAuthenticatorStatus(metadataEntries, certifiedLevel2Aaguid));

        assertEquals(AuthenticatorStatus.NOT_FIDO_CERTIFIED,
                getAuthenticatorStatus(metadataEntries, uncertifiedAaguid));

        assertEquals(AuthenticatorStatus.REVOKED, getAuthenticatorStatus(metadataEntries, revokedAaguid));
    }

    private AuthenticatorStatus getAuthenticatorStatus(List<MetadataBlobPayloadEntry> metadataEntries, Aaguid aaguid) {
        return metadataEntries.stream().filter(e ->
                e.aaguid() != null && e.aaguid().equals(aaguid.toString())
        ).findFirst().get().statusReports().iterator().next().status();
    }

    @Test
    void downloadBlobWhichFailsVerification() throws MetadataException {
        String blobPath = copyClassPathDataToTempFile(REVOKED_CERTIFICATE_BLOB_LOCATION).getAbsolutePath();

        //given
        MetadataBlobPayloadDownloader downloader = new MetadataBlobPayloadDownloader(getBlobVerifier(false),
                new ResourceResolver(), new BlobReader(), new JwtReconstruction());
        assertThatThrownBy(() -> downloader.downloadMetadataPayload(blobPath, mock(TrustAnchorValidator.class)))
                .isInstanceOf(MetadataException.class)
                .hasMessageContaining("Failed to verify the validity of the MDS BLOB");
    }

    @Test
    void downloadDataInvalidBlobPath() throws MetadataException {
        String blobPath = "invalidFile.jwt";

        //given
        MetadataBlobPayloadDownloader downloader = new MetadataBlobPayloadDownloader(getBlobVerifier(true),
                new ResourceResolver(), new BlobReader(), new JwtReconstruction());
        assertThatThrownBy(() -> downloader.downloadMetadataPayload(blobPath, mock(TrustAnchorValidator.class)))
                .isInstanceOf(MetadataException.class)
                .hasMessageContaining("Location did not exist: invalidFile.jwt");
    }

    private BlobVerifier getBlobVerifier(boolean isValid)
            throws MetadataException {
        BlobVerifier blobVerifier = mock(BlobVerifier.class);
        doReturn(isValid).when(blobVerifier).verify(any(), any(), any());
        return blobVerifier;
    }
}
