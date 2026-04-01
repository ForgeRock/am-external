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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import static org.forgerock.openam.radius.common.PacketType.ACCESS_ACCEPT;
import static org.forgerock.openam.radius.common.PacketType.ACCESS_CHALLENGE;
import static org.forgerock.openam.radius.common.PacketType.ACCESS_REJECT;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.VALUE_LENGTH;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.VALUE_START_POSITION;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.authentication.modules.radius.client.Challenge;
import com.sun.identity.authentication.modules.radius.client.ChallengeException;
import com.sun.identity.authentication.modules.radius.client.RejectException;

/**
 * This class implements RFC2865 - Remote Authentication Dial In User Service (RADIUS), June 2000.
 * This is an adaptation of the original RadiusConn code that is compatible with RADIUS nodes.
 */
public class RadiusNodeConn {
    /**
     * The Debug logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RadiusNodeConn.class);
    /**
     * The client secret.
     */
    private final String secret;
    /**
     * The data gram socket.
     */
    private final DatagramSocket socket;
    /**
     * Source of random bytes.
     */
    private final SecureRandom random;
    /**
     * The set of primary servers to connect to.
     */
    private final Set<RADIUSServer> primaries;
    /**
     * The secondary set of servers to connect to if the primaries are offline.
     */
    private final Set<RADIUSServer> secondaries;
    /**
     * The calculator for Message-Authenticator attribute values, null if not required.
     */
    private final MessageAuthenticatorCalculator messageAuthenticatorCalculator;
    /**
     * The runnable task monitor that verifies connectivity to servers.
     */
    private final RadiusMonitor serverMonitor;
    /**
     * The source of id generation for access request packets.
     */
    private short id = (short) currentTimeMillis();

    /**
     * Construct a connection object with a set of primary and secondary servers.
     *
     * @param primaries                      the primary servers to connect to.
     * @param secondaries                    the secondary servers to connect to.
     * @param secret                         the secret shared between this client and the remote servers.
     * @param timeout                        the timeout value.
     * @param messageAuthenticatorCalculator the calculator for Message-Authenticator attribute values,
     *                                       or null if not required.
     * @param monitor                        the radius monitor for this connection.
     * @throws SocketException thrown if unable to create a DatagramSocket.
     */
    public RadiusNodeConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries, String secret, int timeout,
            MessageAuthenticatorCalculator messageAuthenticatorCalculator, RadiusMonitor monitor)
            throws SocketException {
        this.secret = secret;
        this.primaries = primaries;
        this.secondaries = secondaries;
        this.messageAuthenticatorCalculator = messageAuthenticatorCalculator;
        logger.debug("Primary RADIUS servers: {}", primaries);
        logger.debug("Secondary RADIUS servers: {}", secondaries);
        socket = new DatagramSocket();
        socket.setSoTimeout(timeout * 1000);
        random = new SecureRandom();
        this.serverMonitor = monitor;
    }

    /**
     * Disconnects the underlying datagram socket.
     *
     * @throws IOException if an io exception occurs.
     */
    public void disconnect() throws IOException {
        socket.disconnect();
    }

    /**
     * Authenticates the username and password against the remote servers.
     *
     * @param name     the username.
     * @param password the password.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     * @return the attribute set from the response.
     */
    public AttributeSet authenticate(String name, String password)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
        return sendPacket(req);
    }

    /**
     * Sends an access-request to the server in response to a challenge request.
     *
     * @param name     the username.
     * @param password the password.
     * @param ce       the challenge exception providing access to the original challenge response.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public void replyChallenge(String name, String password,
            Challenge ce) throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        StateAttribute state = ce.getStateAttribute();
        if (state == null) {
            throw new IOException("State not found in challenge");
        }
        AccessRequest req = createAccessRequest();
        req.addAttribute(state); // needed in challenge
        if (name != null) {
            req.addAttribute(new UserNameAttribute(name));
        }
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));

        sendPacket(req);
    }

    /**
     * Finds an available server and then sends a packet to that servers.
     *
     * @param packet the packet.
     * @throws IOException        if there is a problem.
     * @throws RejectException    if there is a problem.
     * @throws ChallengeException if there is a problem.
     * @return the attribute set from the response.
     */
    private AttributeSet sendPacket(Packet packet) throws IOException,
            RejectException, ChallengeException {
        Packet res = null;
        RADIUSServer server;
        while (res == null) {
            server = getOnlineServer();
            if (logger.isDebugEnabled()) {
                logger.debug("Using {} for contact RADIUS", server);
            }
            try {
                send(packet, server, socket);
                res = receive(packet.getAuthenticator());
                if (res instanceof AccessReject) {
                    throw new RejectException((AccessReject) res);
                } else if (res instanceof AccessChallenge) {
                    throw new ChallengeException((AccessChallenge) res);
                }
            } catch (IOException ioe) {
                if (ioe instanceof ConnectException || ioe instanceof SocketTimeoutException) {
                    logger.debug("Moving server to offline state - {}", server);
                    serverMonitor.markServerDown(server);
                } else {
                    throw ioe;
                }
            }
        }
        return res.getAttributeSet();
    }

    /**
     * Returns a server that has successfully been contacted.
     *
     * @return the server to connect to.
     */
    private RADIUSServer getOnlineServer() {
        for (RADIUSServer server : primaries) {
            if (!serverMonitor.isServerDown(server)) {
                return server;
            }
        }
        for (RADIUSServer server : secondaries) {
            if (!serverMonitor.isServerDown(server)) {
                return server;
            }
        }
        return null;
    }

    /**
     * Generates a new access request packet identifier.
     *
     * @return the new identifier.
     */
    private short getIdentifier() {
        return id++;
    }

    /**
     * Sends an AccessRequest packet.
     *
     * @param packet the packet.
     * @param server the target server.
     */
    private void send(Packet packet, RADIUSServer server, DatagramSocket socket) throws IOException {
        if (server == null) {
            throw new IOException("No RADIUS server is online.");
        }
        if (messageAuthenticatorCalculator != null) {
            messageAuthenticatorCalculator.addMessageAuthenticator(packet, secret);
        }
        byte[] buffer = new byte[4096];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        dp.setPort(server.getPort());
        dp.setAddress(InetAddress.getByName(server.getHost()));
        byte[] data = packet.getOctets();
        dp.setLength(data.length);
        dp.setData(data);
        socket.send(dp);
        if (logger.isDebugEnabled()) {
            logger.debug("Sent {}", packet);
        }
    }

    /**
     * Blocking call that waits until a valid response packet is received or socket timeout occurs.
     * Continues listening for valid packets within the socket timeout window, silently discarding
     * invalid Message-Authenticator packets as per RFC 2869 section 5.14.
     *
     * @param requestAuthenticator the request authenticator.
     * @return the received packet, or null if only invalid packets were received before timeout.
     */
    private Packet receive(Authenticator requestAuthenticator)
            throws IOException {
        while (true) {
            byte[] buffer = new byte[4096];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            socket.receive(dp);
            byte[] data = dp.getData();
            Packet p = PacketFactory.toPacket(data);
            if (logger.isDebugEnabled()) {
                logger.debug("Received {} size={}", p, p.getAttributeSet().size());
            }
            if (!isValidMessageAuthenticator(p, requestAuthenticator)) {
                // RFC 2869 section 5.14: Silently discard packets with invalid Message-Authenticator
                // and continue listening for valid packets
                logger.debug("Received packet with invalid Message-Authenticator, silently discarding and"
                        + " continuing to listen");
                continue;
            }
            return p;
        }
    }

    /**
     * Generates an access request packet.
     *
     * @return the access request packet.
     */
    private AccessRequest createAccessRequest() {
        RequestAuthenticator ra = new RequestAuthenticator(random);
        return new AccessRequest(getIdentifier(), ra);
    }

    /**
     * Validates the Message-Authenticator attribute in the response packet.
     *
     * @param response             the response packet
     * @param requestAuthenticator the request authenticator
     * @return true if the Message-Authenticator is valid or not required, false otherwise
     */
    private boolean isValidMessageAuthenticator(Packet response, Authenticator requestAuthenticator) {
        if (messageAuthenticatorCalculator == null) {
            return true;
        }
        if (!Arrays.asList(ACCESS_ACCEPT, ACCESS_REJECT, ACCESS_CHALLENGE).contains(response.getType())) {
            logger.debug("Unexpected packet type: {}", response.getType());
            return false;
        }

        // Save the original response authenticator
        Authenticator responseAuthenticator = response.getAuthenticator();

        try {
            // Temporarily replace with request authenticator for validation
            response.setAuthenticator(requestAuthenticator);
            ByteBuffer buffer = ByteBuffer.wrap(response.getOctets());
            Optional<ByteBuffer> calculatedValue = messageAuthenticatorCalculator.computeFromPayloadIfPresent(buffer,
                    secret);

            if (calculatedValue.isEmpty()) {
                logger.debug("Message-Authenticator is required but not present in response packet");
                return false;
            }

            return MessageDigest.isEqual(calculatedValue.get().array(),
                    Arrays.copyOfRange(buffer.array(), VALUE_START_POSITION, VALUE_START_POSITION + VALUE_LENGTH));
        } finally {
            // Restore the original response authenticator
            response.setAuthenticator(responseAuthenticator);
        }
    }
}
