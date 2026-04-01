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

import static org.forgerock.openam.radius.common.PacketType.ACCESS_ACCEPT;
import static org.forgerock.openam.radius.common.PacketType.ACCOUNTING_RESPONSE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.channels.DatagramChannel;

import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link RadiusRequestContext}.
 */
@ExtendWith(MockitoExtension.class)
public class RadiusRequestContextTest {

    private static final byte[] RESPONSE_OCTETS = new byte[]{0, -36, 0, 38, 69, 127, 64, 36, 34, -44, 52, 32, 101, 93,
            -120, -105, 114, 125, 43, 6, 80, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @Mock
    private ClientConfig clientConfig;

    @Mock
    private DatagramChannel channel;

    @Mock
    private MessageAuthenticatorCalculator messageAuthenticatorCalculator;

    @InjectMocks
    private RadiusRequestContext radiusRequestContext;

    private Packet packet;

    @BeforeEach
    void setUp() {
        packet = mock(Packet.class);
        given(clientConfig.getSecret()).willReturn("secret");
        given(packet.getOctets()).willReturn(RESPONSE_OCTETS);
    }

    @ParameterizedTest
    @CsvSource({"ACCESS_ACCEPT", "ACCESS_REJECT", "ACCESS_CHALLENGE"})
    void shouldSendResponseWithMessageAuthenticatorGiven(String packetType) throws RadiusProcessingException {
        // Given
        given(clientConfig.isMessageAuthenticatorRequired()).willReturn(true);
        given(packet.getType()).willReturn(PacketType.valueOf(packetType));

        // When
        radiusRequestContext.send(packet);

        // Then
        verify(messageAuthenticatorCalculator).addMessageAuthenticator(eq(packet), eq("secret"));
    }

    @Test
    void shouldSendResponseWithNoMessageAuthenticatorGivenAccountingPacketType() throws RadiusProcessingException {
        // Given
        given(clientConfig.isMessageAuthenticatorRequired()).willReturn(true);
        given(packet.getType()).willReturn(ACCOUNTING_RESPONSE);

        // When
        radiusRequestContext.send(packet);

        // Then
        verify(messageAuthenticatorCalculator, times(0))
                .addMessageAuthenticator(any(Packet.class), any(String.class));
    }

    @Test
    void shouldSendWitNoMessageAuthenticatorGivenMessageAuthenticatorNotRequired() throws RadiusProcessingException {
        // Given
        given(clientConfig.isMessageAuthenticatorRequired()).willReturn(false);
        given(packet.getType()).willReturn(ACCESS_ACCEPT);

        // When
        radiusRequestContext.send(packet);

        // Then
        verify(messageAuthenticatorCalculator, times(0))
                .addMessageAuthenticator(any(Packet.class), any(String.class));
    }
}
