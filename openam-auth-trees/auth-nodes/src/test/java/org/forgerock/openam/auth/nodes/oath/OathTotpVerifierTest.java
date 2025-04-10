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

package org.forgerock.openam.auth.nodes.oath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.core.rest.devices.RecoveryCodeStorage;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

public class OathTotpVerifierTest {

    @Mock
    private static RecoveryCodeStorage recoveryCodeStorage;
    private static MockedStatic<RecoveryCodeStorage> recoveryCodeStorageMockedStatic;
    private final OathDeviceSettings settings = new OathDeviceSettings();
    private final OffsetDateTime now = OffsetDateTime.of(
            2010,
            11,
            10,
            07,
            59,
            55,
            0,
            ZoneOffset.UTC
    );
    private OathTokenVerifierNode.Config config = new OathTokenVerifierNode.Config() {
        @Override
        public HashAlgorithm totpHashAlgorithm() {
            return HashAlgorithm.HMAC_SHA1;
        }
    };
    private OathTotpVerifier totpVerifier = new OathTotpVerifier(config, settings, now.toEpochSecond());

    @BeforeAll
    static void setUp() {
        recoveryCodeStorageMockedStatic = mockStatic(RecoveryCodeStorage.class);
        recoveryCodeStorageMockedStatic.when(RecoveryCodeStorage::getSystemStorage).thenReturn(recoveryCodeStorage);
    }

    @AfterAll
    static void tearDown() {
        recoveryCodeStorageMockedStatic.close();
    }

    @BeforeEach
    void setup() {
        settings.setSharedSecret("abcd");
    }

    @Test
    void verifyWhenFirstValidTotpInWindowThenSucceed() throws Exception {
        // Given
        settings.setLastLogin(now.minusSeconds(120).toEpochSecond(), TimeUnit.SECONDS);

        // When
        totpVerifier.verify("325835");

        // Then
        assertThat(settings.getClockDriftSeconds()).isEqualTo(0);
        assertThat(settings.getLastLogin()).isEqualTo(now.withSecond(30).toEpochSecond());
    }

    @Test
    void verifyWhenSecondValidTotpInWindowThenSucceed() {
        // Given
        settings.setLastLogin(now.minusSeconds(1).toEpochSecond(), TimeUnit.SECONDS);

        // When
        assertThatThrownBy(() -> totpVerifier.verify("325835"))
                // Then
                .isInstanceOf(OathVerificationException.class)
                .hasMessageStartingWith("Login failed attempting to use the same OTP in same Time Step: ");
    }

    @Test
    void verifyWhenClockHasDriftedThenSuccessAndStoreDrift() throws Exception {
        // Given
        settings.setLastLogin(now.minusSeconds(31).toEpochSecond(), TimeUnit.SECONDS);

        // When
        totpVerifier.verify("607352");

        // Then
        assertThat(settings.getClockDriftSeconds()).isEqualTo(30);
        assertThat(settings.getLastLogin()).isEqualTo(now.plusSeconds(5).toEpochSecond());
    }

    @Test
    void verifyWhenInvalidTotpThenFail() {
        // Given
        settings.setLastLogin(now.minusSeconds(31).toEpochSecond(), TimeUnit.SECONDS);

        // When / Then
        assertThatThrownBy(() -> totpVerifier.verify("000000"))
                .isInstanceOf(OathVerificationException.class);
    }

    @Test
    void verifyWhenValidTotpUsingSHA256AlgorithmThenSucceed() throws Exception {
        // Given
        settings.setLastLogin(now.minusSeconds(120).toEpochSecond(), TimeUnit.SECONDS);
        config = new OathTokenVerifierNode.Config() {
            @Override
            public HashAlgorithm totpHashAlgorithm() {
                return HashAlgorithm.HMAC_SHA256;
            }
        };
        totpVerifier = new OathTotpVerifier(config, settings, now.toEpochSecond());

        // When
        totpVerifier.verify("134207");

        // Then
        assertThat(settings.getClockDriftSeconds()).isEqualTo(0);
        assertThat(settings.getLastLogin()).isEqualTo(now.withSecond(30).toEpochSecond());
    }

    @Test
    void verifyWhenValidTotpUsingSHA512AlgorithmThenSucceed() throws Exception {
        // Given
        settings.setLastLogin(now.minusSeconds(120).toEpochSecond(), TimeUnit.SECONDS);
        config = new OathTokenVerifierNode.Config() {
            @Override
            public HashAlgorithm totpHashAlgorithm() {
                return HashAlgorithm.HMAC_SHA512;
            }
        };
        totpVerifier = new OathTotpVerifier(config, settings, now.toEpochSecond());

        // When
        totpVerifier.verify("154266");

        // Then
        assertThat(settings.getClockDriftSeconds()).isEqualTo(0);
        assertThat(settings.getLastLogin()).isEqualTo(now.withSecond(30).toEpochSecond());
    }
}
