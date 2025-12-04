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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.assertj.core.api.Assertions;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayload;
import org.junit.jupiter.api.Test;

public class BlobReaderTest {

    private final BlobReader reader = new BlobReader();
    private Jwt jwt;

    @Test
    void testBlobReaderFmsJson() throws Exception {
        String string = FileUtils.readStream(
                FileUtils.getFromClasspath("forgerock-signed.jwt"));
        jwt = new JwtReconstruction().reconstructJwt(string, SignedJwt.class);
        final MetadataBlobPayload blob = reader.readBlob(jwt);
        assertThat(blob.entries()).hasSize(6);
    }

    @Test
    void testBlobReaderNonFmsJson() {
        jwt = mock(Jwt.class);
        given(jwt.getClaimsSet()).willReturn(mock(JwtClaimsSet.class));
        given(jwt.getClaimsSet().toString()).willReturn(FileUtils.readStream(
                FileUtils.getFromClasspath("not-a-certificate")));
        Assertions.assertThatThrownBy(() -> reader.readBlob(jwt))
                .isInstanceOf(InvalidPayloadException.class);
    }

}
