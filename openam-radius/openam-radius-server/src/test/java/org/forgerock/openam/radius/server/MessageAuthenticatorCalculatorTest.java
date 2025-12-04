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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Stream;

import javax.crypto.Mac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link MessageAuthenticatorCalculator}.
 */
@ExtendWith(MockitoExtension.class)
class MessageAuthenticatorCalculatorTest {

    private static final ByteBuffer PAYLOAD_IN_BYTES = ByteBuffer.wrap(new byte[]{1, 113, 0, 74, 80, 5, -8, 62, 91, 25,
            -84, 71, 38, 73, -22, 108, -32, -74, -48, -112, 80, 18, -9, -17, -36, 74, 45, 76, -22, 30, 57, 102, -58, 65,
            106, 100, 85, 87, 1, 6, 100, 101, 109, 111, 2, 18, -43, 110, 55, -126, 49, -28, -42, -105, 127, -10, -29,
            -60, 34, -120, 112, 42, 4, 6, 127, 0, 0, 1, 5, 6, 0, 0, 0, 10, 0, 0, 0});

    @InjectMocks
    private MessageAuthenticatorCalculator calculator;

    @Test
    void shouldComputeFromPayloadIfPresent() {
        // Given
        ByteBuffer payload = clone(PAYLOAD_IN_BYTES);
        String clientSecret = "password";

        // When
        Optional<ByteBuffer> result = calculator.computeFromPayloadIfPresent(payload, clientSecret);

        // Then
        ByteBuffer expectedMessageAuthenticatorValue = ByteBuffer.wrap(new byte[]{-9, -17, -36, 74, 45, 76, -22, 30,
                57, 102, -58, 65, 106, 100, 85, 87});
        assertThat(result.get()).isEqualTo(expectedMessageAuthenticatorValue);
    }

    @Test
    void computeFromPayloadIfPresentShouldEnsureOriginalPayloadRemainsUnchanged() {
        // Given
        ByteBuffer payload = clone(PAYLOAD_IN_BYTES);

        // When
        calculator.computeFromPayloadIfPresent(payload, "password");

        // Then
        assertThat(payload).isEqualTo(PAYLOAD_IN_BYTES);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("payloadsWithInvalidMessageAuthenticator")
    void shouldNotComputeFromPayloadIfPresentGiven(String testDescription, ByteBuffer payload) {
        // When
        Optional<ByteBuffer> result = calculator.computeFromPayloadIfPresent(payload, "password");

        // Then
        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFailToComputeFromPayloadIfPresentGivenNullRadiusPayload() {
        // Given
        ByteBuffer payload = null;

        // When - Then
        assertThatThrownBy(() -> calculator.computeFromPayloadIfPresent(payload, "password"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("payload must not be null");
    }

    @Test
    void shouldFailToComputeFromPayloadIfPresentGivenInvalidAlgorithm() {
        try (MockedStatic<Mac> mockedMac = Mockito.mockStatic(Mac.class)) {
            // Given
            mockedMac.when(() -> Mac.getInstance("HmacMD5")).thenThrow(NoSuchAlgorithmException.class);

            // When - Then
            assertThatThrownBy(() -> calculator.computeFromPayloadIfPresent(clone(PAYLOAD_IN_BYTES), "password"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Failed to sign RADIUS payload");
        }
    }

    @Test
    void shouldFailToComputeFromPayloadIfPresentGivenInvalidKey() throws InvalidKeyException {
        // Given
        try (MockedStatic<Mac> mockedMac = Mockito.mockStatic(Mac.class)) {
            // Given
            Mac mac = Mockito.mock(Mac.class);
            mockedMac.when(() -> Mac.getInstance("HmacMD5")).thenReturn(mac);
            doThrow(InvalidKeyException.class).when(mac).init(any());

            // When - Then
            assertThatThrownBy(() -> calculator.computeFromPayloadIfPresent(clone(PAYLOAD_IN_BYTES), "password"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Failed to sign RADIUS payload");
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldFailToComputeFromPayloadIfPresentGivenBlankSecret(String secret) {
        // Given
        ByteBuffer payload = ByteBuffer.wrap(new byte[]{18, -43, 110, 55, -126});

        // When - Then
        assertThatThrownBy(() -> calculator.computeFromPayloadIfPresent(payload, secret))
                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class)
                .hasMessage("secret must not be blank");
    }

    private static Stream<Arguments> payloadsWithInvalidMessageAuthenticator() {
        return Stream.of(
                Arguments.of("No Message Authenticator present in payload", ByteBuffer.wrap(new byte[]{1,
                        113, 0, 74, 80, 5, -8, 62, 91, 25, -84, 71, 38, 73, -22, 108, -32, -74, -48, -112, 1, 6, 100,
                        101, 109, 111, 2, 18, -43, 110, 55, -126, 49, -28, -42, -105, 127, -10, -29, -60, 34, -120, 112,
                        42, 4, 6, 127, 0, 0, 1, 5, 6, 0, 0, 0, 10})),
                Arguments.of("Message Authenticator length of 16 (see last byte in array) is less than 18",
                        ByteBuffer.wrap(new byte[]{1, 113, 0, 74, 80, 5, -8, 62, 91, 25, -84, 71, 38, 73, -22, 108, -32,
                                -74, -48, -112, 80, 16})),
                Arguments.of("Message Authenticator length including the preceding bytes does not meet the "
                        + "minimum of 38", ByteBuffer.wrap(new byte[]{1, 113, 0, 74, 80, 5, -8, 62, 91, 25, -84, 71,
                        38, 73, -22, 108, -32, -74, -48, -112, 80, 18})),
                Arguments.of("Total payload length is 20 so the Message Authenticator type position of [20] "
                        + "is out of bounds", ByteBuffer.wrap(new byte[]{1, 113, 0, 74, 80, 5, -8, 62, 91, 25, -84, 71,
                        38, 73, -22, 108, -32, -74, -48, -112}))
        );
    }

    private ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

}
