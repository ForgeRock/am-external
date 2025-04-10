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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils.getFromClasspath;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.eq;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.junit.jupiter.api.Test;

import java.net.URL;

public class BlobCertificateResolverTest {

    private static final URL A_TEST_URL = ResourceResolver.getUrl("http://google.co.uk").orElse(null);

    private BlobCertificateResolver resolver;
    private ResourceResolver mockResourceResolver;

    public void setup() {
        mockResourceResolver = mock(ResourceResolver.class);
        resolver = new BlobCertificateResolver(mockResourceResolver);
    }

    @Test
    void testBlobCertificateResolverReadsUrl() throws Exception {
        setup();
        given(mockResourceResolver.resolve(eq(A_TEST_URL)))
                .willReturn(getFromClasspath("FidoMetadataDownloader/forgerock/forgerock-root.pem"));
        assertThat(resolver.resolveCertificate(A_TEST_URL)).isNotEmpty();
    }

    @Test
    void testBlobCertificateResolverValidUrlInvalidCert() throws Exception {
        setup();
        given(mockResourceResolver.resolve(eq(A_TEST_URL)))
                .willReturn(getFromClasspath("not-a-certificate"));
        assertThatThrownBy(() -> resolver.resolveCertificate(A_TEST_URL))
                .isInstanceOf(CertificateResolutionException.class);
    }
}
