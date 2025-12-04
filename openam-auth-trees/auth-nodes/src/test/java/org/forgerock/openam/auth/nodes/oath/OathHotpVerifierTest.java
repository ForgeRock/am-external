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

package org.forgerock.openam.auth.nodes.oath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import org.forgerock.openam.core.rest.devices.RecoveryCodeStorage;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OathHotpVerifierTest {

    @Mock
    private static RecoveryCodeStorage recoveryCodeStorage;
    private static MockedStatic<RecoveryCodeStorage> recoveryCodeStorageMockedStatic;
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

    @BeforeAll
    static void setUp() {
        recoveryCodeStorageMockedStatic = mockStatic(RecoveryCodeStorage.class);
        recoveryCodeStorageMockedStatic.when(RecoveryCodeStorage::getSystemStorage).thenReturn(recoveryCodeStorage);
    }

    @AfterAll
    static void tearDown() {
        recoveryCodeStorageMockedStatic.close();
    }

    @Test
    void verifyFirstValidHotpThenSucceed() throws Exception {
        // Given
        settings.setSharedSecret("abcd");

        // When
        hotpVerifier.verify("564491");

        // Then
        assertThat(settings.getCounter()).isEqualTo(1);
    }

    @Test
    void verifySecondValidHotpThenSucceed() throws Exception {
        // Given
        settings.setSharedSecret("abcd");

        // When
        hotpVerifier.verify("016912");

        // Then
        assertThat(settings.getCounter()).isEqualTo(2);
    }

    @Test
    void verifyThirdValidHotpThenSucceed() throws Exception {
        // Given
        settings.setSharedSecret("abcd");
        settings.setCounter(2);

        // When
        hotpVerifier.verify("734574");

        // Then
        assertThat(settings.getCounter()).isEqualTo(3);
    }

    @Test
    void verifySameValidHotpThenFail() {
        // Given
        settings.setSharedSecret("abcd");
        settings.setCounter(1);

        // When / Then
        assertThatThrownBy(() -> hotpVerifier.verify("564491"))
                .isInstanceOf(OathVerificationException.class);
    }

    @Test
    void verifyWhenInvalidHotpThenFail() {
        // Given
        settings.setSharedSecret("abcd");
        // When / Then
        assertThatThrownBy(() -> hotpVerifier.verify("000000"))
                .isInstanceOf(OathVerificationException.class);
    }

    @Test
    void verifyWhenExceedMaximumWindowSizeThenFail() {
        // Given
        settings.setSharedSecret("abcd");
        settings.setCounter(0);

        // When / Then
        assertThatThrownBy(() -> hotpVerifier.verify("301128"))
                .isInstanceOf(OathVerificationException.class);
    }
}
