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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TrustAnchorUtilitiesTest {

    @Mock
    VerificationKey mockVerificationKey;
    @Mock
    X509Certificate mockCertificate;

    TrustAnchorUtilities utilities;

    @BeforeTest
    public void theSetup() {
        initMocks(this);
        utilities = new TrustAnchorUtilities();
    }

    @Test
    public void testTrustAnchorCreation() {
        //given
        List<VerificationKey> keys = new ArrayList<>();
        keys.add(mockVerificationKey);
        Promise<Stream<VerificationKey>, NeverThrowsException> secretSource = newResultPromise(keys.stream());

        given(mockVerificationKey.getCertificate(X509Certificate.class)).willReturn(Optional.of(mockCertificate));

        //when
        Set<TrustAnchor> result = utilities.trustAnchorsFromSecrets(secretSource);

        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next().getTrustedCert()).isEqualTo(mockCertificate);
    }

}
