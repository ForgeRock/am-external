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

package org.forgerock.openam.auth.nodes.oath;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.testng.annotations.Test;

public class OathHotpVerifierTest {

    private final OathDeviceSettings settings = new OathDeviceSettings();

    private final OathTokenVerifierNode.Config config = new OathTokenVerifierNode.Config() {
        @Override
        public HashAlgorithm totpHashAlgorithm() {
            return HashAlgorithm.HMAC_SHA1;
        }
        @Override
        public int hotpWindowSize() {
            return 2;
        }
    };

    private final OathHotpVerifier hotpVerifier = new OathHotpVerifier(config, settings);

    @Test
    public void verifyFirstValidHotpThenSucceed() throws Exception {
        // Given
        settings.setSharedSecret("abcd");

        // When
        hotpVerifier.verify("564491");

        // Then
        assertThat(settings.getCounter()).isEqualTo(1);
    }

    @Test
    public void verifySecondValidHotpThenSucceed() throws Exception {
        // Given
        settings.setSharedSecret("abcd");

        // When
        hotpVerifier.verify("016912");

        // Then
        assertThat(settings.getCounter()).isEqualTo(2);
    }

    @Test
    public void verifyThirdValidHotpThenSucceed() throws Exception {
        // Given
        settings.setSharedSecret("abcd");
        settings.setCounter(2);

        // When
        hotpVerifier.verify("734574");

        // Then
        assertThat(settings.getCounter()).isEqualTo(3);
    }

    @Test(expectedExceptions = OathVerificationException.class)
    public void verifySameValidHotpThenFail() throws Exception {
        // Given
        settings.setSharedSecret("abcd");
        settings.setCounter(1);

        // When
        hotpVerifier.verify("564491");

        // Then
        // throw exception
    }

    @Test(expectedExceptions = OathVerificationException.class)
    public void verifyWhenInvalidHotpThenFail() throws OathVerificationException {
        // Given
        settings.setSharedSecret("abcd");

        // When
        hotpVerifier.verify("000000");

        // Then
        // throw exception
    }

    @Test(expectedExceptions = OathVerificationException.class)
    public void verifyWhenExceedMaximumWindowSizeThenFail() throws Exception {
        // Given
        settings.setSharedSecret("abcd");
        settings.setCounter(0);

        // When
        hotpVerifier.verify("301128");

        // Then
        // throw exception
    }
}
