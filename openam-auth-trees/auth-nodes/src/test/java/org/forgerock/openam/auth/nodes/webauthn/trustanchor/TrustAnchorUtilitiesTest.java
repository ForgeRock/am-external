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
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.forgerock.secrets.keys.CertificateVerificationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TrustAnchorUtilitiesTest {

    @Mock
    CertificateVerificationKey mockVerificationKey;
    @Mock
    X509Certificate mockCertificate;

    TrustAnchorUtilities utilities;

    @BeforeEach
    void theSetup() {
        utilities = new TrustAnchorUtilities();
    }

    @Test
    void testTrustAnchorCreation() throws Exception {
        //given
        List<CertificateVerificationKey> keys = new ArrayList<>();
        keys.add(mockVerificationKey);
        List<CertificateVerificationKey> secretSource = newResultPromise(keys.stream()).get().toList();

        given(mockVerificationKey.getCertificate(X509Certificate.class)).willReturn(Optional.of(mockCertificate));

        //when
        Set<TrustAnchor> result = utilities.trustAnchorsFromSecrets(secretSource);

        //then
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getTrustedCert()).isEqualTo(mockCertificate);
    }

}
