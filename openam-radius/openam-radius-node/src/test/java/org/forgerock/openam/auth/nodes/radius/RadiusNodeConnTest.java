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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.authentication.modules.radius.client.Challenge;
import com.sun.identity.authentication.modules.radius.client.ChallengeException;
import com.sun.identity.authentication.modules.radius.client.RadiusChallenge;
import com.sun.identity.authentication.modules.radius.client.RejectException;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Unit tests for {@link RadiusNodeConn}.
 * Verifies construction, authentication, and challenge reply logic.
 */
@ExtendWith(MockitoExtension.class)
class RadiusNodeConnTest {

    public static final String USERNAME = "testUser";
    public static final String PASSWORD = "testPass";
    private static final String TEST_SECRET = "secret";

    @Mock
    private Authenticator authenticator;
    @Mock
    private MessageAuthenticatorCalculator messageAuthCalculator;
    @Mock
    private RADIUSServer radiusServer;
    @Spy
    RadiusMonitor radiusMonitor = Mockito.spy(new RadiusMonitor(TEST_SECRET, 5, 10, null, null));

    @Test
    void shouldDisconnectSocket() throws IOException {
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class)) {
            // Given
            RadiusNodeConn conn = createRadiusConn();

            // When
            conn.disconnect();

            // Then
            List<DatagramSocket> sockets = mockedDatagramSocket.constructed();
            assertThat(sockets).hasSize(1);
            verify(sockets.get(0)).disconnect();
        }
    }

    @Test
    void shouldAuthenticateGivenAccessAcceptedResponse() throws IOException, ChallengeException, RejectException,
            NoSuchAlgorithmException {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[16]);
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) ->
                             mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, context))) {
            RadiusNodeConn conn = createRadiusConn();

            // When
            conn.authenticate(USERNAME, PASSWORD);

            // Then
            verifyAcceptResponse(mockedDatagramPacket, mockedDatagramSocket);
        }
    }

    @Test
    void shouldFailToAuthenticateGivenAccessRejectedResponse() throws IOException {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[16]);
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_REJECT, mock, context);
             })) {
            RadiusNodeConn conn = createRadiusConn();

            // When / Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(RejectException.class);
        }
    }

    @Test
    void shouldFailToAuthenticateGivenAccessChallengeResponse() throws IOException {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[16]);
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_CHALLENGE, mock, context);
             })) {
            RadiusNodeConn conn = createRadiusConn();

            // When / Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(ChallengeException.class);
        }
    }

    @Test
    void shouldReplyChallengeGivenState() throws Exception {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[16]);
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, context);
             })) {
            RadiusNodeConn conn = createRadiusConn();
            Challenge challenge = new RadiusChallenge(
                    new StateAttribute("stateAttributeTest"), "replyMessage", "response"
            );

            // When
            conn.replyChallenge(USERNAME, PASSWORD, challenge);

            // Then
            verifyAcceptResponse(mockedDatagramPacket, mockedDatagramSocket);
        }
    }

    @Test
    void shouldFailToReplyChallengeGivenStateAttributeIsMissing() throws IOException {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) ->
                             mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, context))) {
            RadiusNodeConn conn = createRadiusConn();
            Challenge challenge = new RadiusChallenge(null, "replyMessage", "response");

            // When / Then
            assertThatThrownBy(() -> conn.replyChallenge(USERNAME, PASSWORD, challenge)).isInstanceOf(IOException.class)
                    .hasMessage("State not found in challenge");
        }
    }

    @Test
    void shouldAcceptPacketGivenMessageAuthenticatorNotRequired() throws IOException, NoSuchAlgorithmException,
            ChallengeException, RejectException {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[16]);

        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) ->
                     mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, context))) {

            // MessageAuthenticatorCalculator is null, so validation is skipped
            RadiusNodeConn conn = createRadiusConn();

            // When
            conn.authenticate(USERNAME, PASSWORD);

            // Then - should succeed without Message-Authenticator validation
            List<DatagramSocket> sockets = mockedDatagramSocket.constructed();
            assertThat(sockets).hasSize(1);
            verify(sockets.get(0), times(1)).receive(any(DatagramPacket.class));
        }
    }

    @Test
    void shouldAuthenticateGivenMessageAuthenticatorIsValid() throws IOException, ChallengeException, RejectException,
            NoSuchAlgorithmException {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) ->
                     mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, context, TEST_SECRET))) {
            RadiusNodeConn conn = createRadiusConnWithMessageAuthenticator();
            given(messageAuthCalculator.computeFromPayloadIfPresent(any(), any()))
                    .willReturn(Optional.of(ByteBuffer.wrap(
                            new byte[]{ 111, 42, -118, 103, -96, 70, -113, 33, 65, -37, 69, -51, -8, -103, -18, -5 }))
                    );

            // When
            conn.authenticate(USERNAME, PASSWORD);

            // Then
            verifyAcceptResponse(mockedDatagramPacket, mockedDatagramSocket);
        }
    }

    @Test
    void shouldFailGivenMessageAuthenticatorIsInvalid() throws IOException {
        // Given
        given(authenticator.getOctets()).willReturn(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class,
                (mock, context) -> {
                    // Checking that message-authenticator is validated at least once
                    Mockito.doAnswer(invocation -> null)
                            .doAnswer(invocation -> {
                                // Socket timeout to end the test
                                throw new java.net.SocketTimeoutException("Socket timeout");
                            })
                            .when(mock).receive(any(DatagramPacket.class));
                });
             MockedConstruction<DatagramPacket> mockedDatagramPacket =
                     Mockito.mockConstruction(DatagramPacket.class, (mock, context) ->
                     mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, context, TEST_SECRET))) {
            given(messageAuthCalculator.computeFromPayloadIfPresent(any(), any()))
                    .willReturn(Optional.of(
                            ByteBuffer.wrap(new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 })
                    ));
            RadiusNodeConn conn = createRadiusConnWithMessageAuthenticator();


            // When / Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("No RADIUS server is online");

            // Verify that the Message-Authenticator validation was attempted
            // at least once but returned invalid response
            verify(messageAuthCalculator, times(1)).computeFromPayloadIfPresent(any(), any());
        }
    }

    private void mockSendAndReceiveDatagramPackets(
            PacketType receivePacketType, DatagramPacket mock, MockedConstruction.Context mockContext) {
        mockSendAndReceiveDatagramPackets(receivePacketType, mock, mockContext, null);
    }

    private void mockSendAndReceiveDatagramPackets(
            PacketType receivePacketType, DatagramPacket mock, MockedConstruction.Context mockContext, String secret) {
        if (mockContext.getCount() % 2 == 1) {
            // Odd numbered packets are sent to the RADIUS server
            mockAccessRequest(mock);
        } else {
            // Even numbered packets are received from the RADIUS server
            Packet packet = switch (receivePacketType) {
                case ACCESS_ACCEPT -> new AccessAccept();
                case ACCESS_REJECT -> new AccessReject();
                case ACCESS_CHALLENGE -> new AccessChallenge();
                default -> throw new IllegalStateException();
            };
            packet.setAuthenticator(authenticator);
            if (secret != null) {
                mockAccessMessageAuthenticator(packet, secret);
            }
            var data = packet.getOctets();
            given(mock.getData()).willReturn(data);
        }
    }

    private void mockAccessRequest(DatagramPacket mock) {
        var accessRequest = new AccessRequest();
        accessRequest.setAuthenticator(authenticator);
        var data = accessRequest.getOctets();
        given(mock.getData()).willReturn(data);
    }

    private void mockAccessMessageAuthenticator(Packet packet, String secret) {
        var messageAuthCalculator = new MessageAuthenticatorCalculator();
        messageAuthCalculator.addMessageAuthenticator(packet, secret);
    }

    private RadiusNodeConn createRadiusConn() throws SocketException {
        Set<RADIUSServer> primaries = Set.of(radiusServer);
        Set<RADIUSServer> secondaries = Collections.emptySet();
        return new RadiusNodeConn(primaries, secondaries, TEST_SECRET, 10, null, radiusMonitor);
    }

    private RadiusNodeConn createRadiusConnWithMessageAuthenticator() throws SocketException {
        Set<RADIUSServer> primaries = Set.of(radiusServer);
        Set<RADIUSServer> secondaries = Collections.emptySet();
        return new RadiusNodeConn(primaries, secondaries, TEST_SECRET, 10, messageAuthCalculator, radiusMonitor);
    }

    private void verifyAcceptResponse(
            MockedConstruction<DatagramPacket> mockedDatagramPacket,
            MockedConstruction<DatagramSocket> mockedDatagramSocket) throws IOException {
        List<DatagramPacket> packets = mockedDatagramPacket.constructed();
        assertThat(packets).hasSize(2);
        DatagramPacket sendRequest = packets.get(0);
        DatagramPacket receivedRequest = packets.get(1);

        List<DatagramSocket> sockets = mockedDatagramSocket.constructed();
        assertThat(sockets).hasSize(1);

        verifySendRequestViaSocket(sockets, sendRequest);
        verifyRequestContainsExpectedUserName(sendRequest);
        verifyReceivedPacketIsOfTypeAccessAccept(receivedRequest);
    }

    private void verifyRequestContainsExpectedUserName(DatagramPacket sendRequest) {
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(sendRequest).setData(dataCaptor.capture());
        var sentDataPacket = PacketFactory.toPacket(dataCaptor.getValue());
        Attribute userNameAttribute = sentDataPacket.getAttributeSet().getAttributeByType(AttributeType.USER_NAME);
        assertThat(((UserNameAttribute) userNameAttribute).getName()).isEqualTo(USERNAME);
        Attribute passwordAttribute = sentDataPacket.getAttributeSet().getAttributeByType(AttributeType.USER_PASSWORD);
        assertThat(passwordAttribute).isNotNull();
    }

    private void verifyReceivedPacketIsOfTypeAccessAccept(DatagramPacket receivedRequest) {
        var receivedPacket = PacketFactory.toPacket(receivedRequest.getData());
        assertThat(receivedPacket.getType()).isEqualTo(PacketType.ACCESS_ACCEPT);
    }

    private void verifySendRequestViaSocket(List<DatagramSocket> sockets, DatagramPacket sendRequest)
            throws IOException {
        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(sockets.get(0)).send(packetCaptor.capture());
        DatagramPacket packet = packetCaptor.getValue();
        assertThat(packet).isEqualTo(sendRequest);
    }
}
