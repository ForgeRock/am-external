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
 * Copyright 2026 ForgeRock AS.
 */
package com.sun.identity.authentication.modules.radius.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.ResponseAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link RadiusConn}.
 * Verifies construction, authentication, and challenge reply logic.
 */
@ExtendWith(MockitoExtension.class)
class RadiusConnTest {

    public static final String USERNAME = "testUser";
    public static final String PASSWORD = "testPass";
    private static final String TEST_SECRET = "secret";
    private static final IdentifierSource CORRESPONDING_IDENTIFIER = request -> request.getIdentifier();
    private static final IdentifierSource ANOTHER_IDENTIFIER = request -> (short) (request.getIdentifier() + 1);
    private static final int SOCKET_TIMEOUT_MS = 5000;
    /**
     * The message of the exception that escapes a login for which every response was discarded. The discard path's
     * own {@link java.net.SocketTimeoutException} does not reach the caller: it is recognised as retryable, the
     * server is marked offline, and with no further server to try the retry loop fails with this instead. Asserting
     * it distinguishes a response that was discarded from any other {@link IOException} the login could raise.
     */
    private static final String NO_SERVER_ONLINE = "No RADIUS server is online.";

    private Packet sentRequest;

    @Mock
    private RADIUSServer radiusServer;

    @Test
    void shouldDisconnectSocket() throws IOException {
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = Mockito.mockConstruction(DatagramSocket.class)) {
            // Given
            RadiusConn conn = createRadiusConn();

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
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock);
             })) {
            RadiusConn conn = createRadiusConn();

            // When
            conn.authenticate(USERNAME, PASSWORD);

            // Then
            verifyAcceptResponse(mockedDatagramPacket, mockedDatagramSocket);
        }
    }

    @Test
    void shouldFailToAuthenticateGivenAccessRejectedResponse() throws IOException {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_REJECT, mock);
             })) {
            RadiusConn conn = createRadiusConn();

            // When - Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(RejectException.class);
        }
    }

    @Test
    void shouldFailToAuthenticateGivenAccessChallengeResponse() throws IOException {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_CHALLENGE, mock);
             })) {
            RadiusConn conn = createRadiusConn();

            // When - Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(ChallengeException.class);
        }
    }

    @Test
    void shouldReplyChallengeGivenState() throws IOException, ChallengeException, RejectException, NoSuchAlgorithmException {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock);
             })) {
            RadiusConn conn = createRadiusConn();
            ChallengeException challenge = challengeException(new StateAttribute("stateAttributeTest"));

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
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock);
             })) {
            RadiusConn conn = createRadiusConn();
            ChallengeException challenge = challengeException(null);

            // When - Then
            assertThatThrownBy(() -> conn.replyChallenge(USERNAME, PASSWORD, challenge)).isInstanceOf(IOException.class)
                    .hasMessage("State not found in challenge");
        }
    }

    @Test
    void shouldNotAuthenticateGivenResponseNotProducedWithTheSharedSecret() throws IOException {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, "notTheSharedSecret",
                         CORRESPONDING_IDENTIFIER);
             })) {
            RadiusConn conn = createRadiusConn();

            // When - Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(IOException.class)
                    .hasMessage(NO_SERVER_ONLINE);
        }
    }

    @Test
    void shouldNotAuthenticateGivenResponseBearingAnotherRequestsIdentifier() throws IOException {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class, (mock, context) -> {
                 mockSendAndReceiveDatagramPackets(PacketType.ACCESS_ACCEPT, mock, TEST_SECRET, ANOTHER_IDENTIFIER);
             })) {
            RadiusConn conn = createRadiusConn();

            // When - Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(IOException.class)
                    .hasMessage(NO_SERVER_ONLINE);
        }
    }

    @Test
    void shouldNotAuthenticateGivenUnparseableResponsePacket() throws Exception {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) -> Mockito.doReturn(unparseablePacketOctets()).when(mock).getData())) {
            RadiusConn conn = createRadiusConn();

            // When - Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(IOException.class)
                    .hasMessage(NO_SERVER_ONLINE);
        }
    }

    @Test
    void shouldNotAuthenticateGivenMalformedResponsePacket() throws Exception {
        // Given
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) -> Mockito.doReturn(malformedPacketOctets()).when(mock).getData())) {
            RadiusConn conn = createRadiusConn();

            // When - Then
            assertThatThrownBy(() -> conn.authenticate(USERNAME, PASSWORD)).isInstanceOf(IOException.class)
                    .hasMessage(NO_SERVER_ONLINE);
        }
    }

    @Test
    void shouldAuthenticateGivenValidResponseAfterDiscardedResponses() throws Exception {
        // Given
        int discardedBeforeValid = 5;
        AtomicInteger reads = new AtomicInteger();
        try (MockedConstruction<DatagramSocket> mockedDatagramSocket = mockDatagramSocket(discardedBeforeValid + 1);
             MockedConstruction<DatagramPacket> mockedDatagramPacket = Mockito.mockConstruction(DatagramPacket.class,
                     (mock, context) -> mockDiscardedThenValidResponses(mock, reads, discardedBeforeValid))) {
            RadiusConn conn = createRadiusConn();

            // When
            conn.authenticate(USERNAME, PASSWORD);

            // Then
            verify(mockedDatagramSocket.constructed().get(0), Mockito.times(discardedBeforeValid + 1))
                    .receive(Mockito.any(DatagramPacket.class));
        }
    }

    /**
     * Answers reads with the given number of responses bearing an identifier the request does not carry, followed by
     * one that does correspond to the request. The count is deliberately higher than any fixed discard budget: while
     * the socket timeout still has time left for the genuine reply, responses that cannot be attributed to the
     * request must not cause the request to be abandoned, as an attacker able to reach the client's port could
     * otherwise deny service by racing a handful of arbitrary datagrams ahead of the server's answer.
     */
    private void mockDiscardedThenValidResponses(DatagramPacket mock, AtomicInteger reads, int discardedBeforeValid) {
        Mockito.doAnswer(invocation -> {
            sentRequest = PacketFactory.toPacket((byte[]) invocation.getArgument(0));
            return null;
        }).when(mock).setData(Mockito.any(byte[].class));
        Mockito.doAnswer(invocation -> {
            AccessAccept response = new AccessAccept();
            IdentifierSource identifier = reads.getAndIncrement() < discardedBeforeValid
                    ? ANOTHER_IDENTIFIER : CORRESPONDING_IDENTIFIER;
            response.setIdentifier(identifier.identifierFor(sentRequest));
            return signedOctets(response, sentRequest.getAuthenticator(), TEST_SECRET);
        }).when(mock).getData();
    }

    /**
     * Returns the on-the-wire octets of a well-formed but unhandled RADIUS packet (Accounting-Request, code 4),
     * for which {@link PacketFactory#toPacket(byte[])} returns {@code null}.
     */
    private byte[] unparseablePacketOctets() {
        byte[] data = new byte[20];
        data[0] = (byte) PacketType.ACCOUNTING_REQUEST.getTypeCode();
        data[1] = 1;
        data[2] = 0;
        data[3] = 20;
        return data;
    }

    /**
     * Returns the on-the-wire octets of a recognised packet type declaring a length below the minimum RADIUS
     * header size (20), for which {@link PacketFactory#toPacket(byte[])} throws {@link java.nio.BufferUnderflowException}
     * rather than returning {@code null}.
     */
    private byte[] malformedPacketOctets() {
        byte[] data = new byte[20];
        data[0] = (byte) PacketType.ACCESS_ACCEPT.getTypeCode();
        data[1] = 1;
        data[2] = 0;
        data[3] = 10;
        return data;
    }

    private void mockSendAndReceiveDatagramPackets(PacketType receivePacketType, DatagramPacket mock) {
        mockSendAndReceiveDatagramPackets(receivePacketType, mock, TEST_SECRET, CORRESPONDING_IDENTIFIER);
    }

    private void mockSendAndReceiveDatagramPackets(PacketType receivePacketType, DatagramPacket mock,
            String responseSecret, IdentifierSource responseIdentifier) {
        // Capture the request as actually sent, so the response can be built to correspond to it.
        Mockito.doAnswer(invocation -> {
            sentRequest = PacketFactory.toPacket((byte[]) invocation.getArgument(0));
            return null;
        }).when(mock).setData(Mockito.any(byte[].class));
        // Answer reads with a response corresponding to the request that was sent, signed with the given secret as a
        // RADIUS server would do.
        Mockito.doAnswer(invocation -> {
            Packet packet = switch (receivePacketType) {
                case ACCESS_ACCEPT -> new AccessAccept();
                case ACCESS_REJECT -> new AccessReject();
                case ACCESS_CHALLENGE -> new AccessChallenge();
                default -> throw new IllegalStateException();
            };
            packet.setIdentifier(responseIdentifier.identifierFor(sentRequest));
            return signedOctets(packet, sentRequest.getAuthenticator(), responseSecret);
        }).when(mock).getData();
    }

    /**
     * Supplies the identifier a mocked response should bear, given the request that was actually sent. Deriving it
     * from the request rather than hardcoding a value keeps the "another request's identifier" case deterministic:
     * the client's own identifiers are seeded from the clock, so any fixed value collides with the request roughly
     * once in every 256 runs.
     */
    private interface IdentifierSource {
        short identifierFor(Packet request);
    }

    /**
     * Returns the on-the-wire octets of the given response bearing the Response Authenticator that a RADIUS server
     * holding the given secret would produce, as described in section 3 of RFC 2865.
     */
    private byte[] signedOctets(Packet response, Authenticator requestAuthenticator, String secret) {
        try {
            response.setAuthenticator(requestAuthenticator);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(response.getOctets());
            md5.update(secret.getBytes("UTF-8"));
            response.setAuthenticator(new ResponseAuthenticator(md5.digest()));
            return response.getOctets();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Mocks the construction of the client's socket so that it reports a bounded timeout and delivers the given
     * number of responses before falling silent, as a RADIUS server answering a request once does. The timeout is
     * what bounds the client's wait, so a socket reporting one is needed for any test that receives at all.
     */
    private MockedConstruction<DatagramSocket> mockDatagramSocket(int responsesDelivered) {
        AtomicInteger received = new AtomicInteger();
        return Mockito.mockConstruction(DatagramSocket.class, (mock, context) -> {
            Mockito.doReturn(SOCKET_TIMEOUT_MS).when(mock).getSoTimeout();
            Mockito.doAnswer(invocation -> {
                if (received.getAndIncrement() >= responsesDelivered) {
                    throw new SocketTimeoutException("Receive timed out");
                }
                return null;
            }).when(mock).receive(Mockito.any(DatagramPacket.class));
        });
    }

    /**
     * Wraps an Access-Challenge in the exception through which this branch's {@link RadiusConn#replyChallenge} takes
     * the original challenge, optionally bearing the given State attribute.
     */
    private ChallengeException challengeException(StateAttribute state) {
        AccessChallenge challenge = new AccessChallenge();
        challenge.addAttribute(new ReplyMessageAttribute("replyMessage"));
        if (state != null) {
            challenge.addAttribute(state);
        }
        return new ChallengeException(challenge);
    }

    private RadiusConn createRadiusConn() throws SocketException {
        // RadiusConn tracks server health in a static map keyed on RADIUSServer, which implements value equality, so
        // an unstubbed mock is used to keep each test's server a distinct key and its health state its own.
        Set<RADIUSServer> primaries = Set.of(radiusServer);
        Set<RADIUSServer> secondaries = Collections.emptySet();
        return new RadiusConn(primaries, secondaries, TEST_SECRET, 5, 10);
    }

    private void verifyAcceptResponse(MockedConstruction<DatagramPacket> mockedDatagramPacket, MockedConstruction<DatagramSocket> mockedDatagramSocket) throws IOException {
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

    private void verifySendRequestViaSocket(List<DatagramSocket> sockets, DatagramPacket sendRequest) throws IOException {
        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(sockets.get(0)).send(packetCaptor.capture());
        DatagramPacket packet = packetCaptor.getValue();
        assertThat(packet).isEqualTo(sendRequest);
    }
}
