/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 * Portions Copyrighted 2011-2026 ForgeRock AS.
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package com.sun.identity.authentication.modules.radius.client;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.ResponseAuthenticatorValidator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;

/**
 * This class implements RFC2865 - Remote Authentication Dial In User Service (RADIUS), June 2000.
 */
public class RadiusConn {
    /**
     * The default timeout.
     */
    public static final int DEFAULT_TIMEOUT = 5;
    /**
     * The client secret.
     */
    private String secret = null;
    /**
     * The data gram socket.
     */
    private DatagramSocket socket = null;
    /**
     * The source of id generation for access request packets.
     */
    private short id = (short) currentTimeMillis();
    /**
     * Source of random bytes.
     */
    private SecureRandom random = null;
    /**
     * The set of primary servers to connect to.
     */
    private Set<RADIUSServer> primaries;
    /**
     * The secondary set of servers to connect to if the primaries are offline.
     */
    private Set<RADIUSServer> secondaries;
    /**
     * The health check interval.
     */
    private int healthCheckInterval = 5;
    /**
     * The status of the various servers.
     */
    private static final Map<RADIUSServer, Boolean> SERVER_STATUS = new HashMap<RADIUSServer, Boolean>();
    /**
     * The Debug logger instance for this class.
     */
    private static Logger debug = LoggerFactory.getLogger(RadiusConn.class);
    /**
     * The runnable task monitor that verifies connectivity to servers.
     */
    private static RADIUSMonitor serverMonitor;
    /**
     * An object used as an access lock.
     */
    private static final Object SERVER_MONITOR_LOCK = new Object();
    /**
     * The validator for the Response Authenticator of received responses.
     */
    private final ResponseAuthenticatorValidator responseAuthenticatorValidator =
            new ResponseAuthenticatorValidator();

    /**
     * Construct a connection object with a set of primary and seconary servers.
     *
     * @param primaries           the primary servers to connect to.
     * @param secondaries         the secondary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param timeout             the timeout value.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException thrown if unable to create a DatagramSocket.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries, String secret, int timeout,
            int healthCheckInterval) throws SocketException {
        this.secret = secret;
        this.primaries = primaries;
        this.secondaries = secondaries;
        this.healthCheckInterval = healthCheckInterval;
        if (debug.isDebugEnabled()) {
            debug.debug("Primary RADIUS servers: " + primaries);
            debug.debug("Secondary RADIUS servers: " + secondaries);
        }
        socket = new DatagramSocket();
        socket.setSoTimeout(timeout * 1000);
        random = new SecureRandom();
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
     */
    public void authenticate(String name, String password)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
        sendPacket(req);
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
                               ChallengeException ce) throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        StateAttribute state = (StateAttribute)
                ce.getAttributeSet().getAttributeByType(AttributeType.STATE);
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
     */
    private void sendPacket(Packet packet) throws IOException,
            RejectException, ChallengeException {
        Packet res = null;
        RADIUSServer server = null;
        while (res == null) {
            server = getOnlineServer();
            if (debug.isDebugEnabled()) {
                debug.debug("Using " + server + " for contact RADIUS");
            }
            try {
                send(packet, server);
                res = receive(packet);
                if (res instanceof AccessReject) {
                    throw new RejectException((AccessReject) res);
                } else if (res instanceof AccessChallenge) {
                    throw new ChallengeException((AccessChallenge) res);
                }
            } catch (IOException ioe) {
                if (ioe instanceof ConnectException || ioe instanceof SocketTimeoutException) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("Moving server to offline state - " + server);
                    }
                    synchronized (SERVER_STATUS) {
                        SERVER_STATUS.put(server, Boolean.FALSE);
                    }
                    synchronized (SERVER_MONITOR_LOCK) {
                        if (serverMonitor == null || serverMonitor.scheduledExecutionTime() == -1) {
                            serverMonitor = new RADIUSMonitor();
                            SystemTimer.getTimer().schedule(serverMonitor,
                                    new Date(((currentTimeMillis()) / 1000) * 1000));
                        }
                    }
                } else {
                    throw ioe;
                }
            }
        }
    }

    /**
     * Returns a server that has successfully been contacted.
     *
     * @return the server to connect to.
     */
    private RADIUSServer getOnlineServer() {
        synchronized (SERVER_STATUS) {
            for (RADIUSServer server : primaries) {
                Boolean state = SERVER_STATUS.get(server);
                if (state == null) {
                    SERVER_STATUS.put(server, Boolean.TRUE);
                    return server;
                } else if (state) {
                    return server;
                }
            }
            for (RADIUSServer server : secondaries) {
                Boolean state = SERVER_STATUS.get(server);
                if (state == null) {
                    SERVER_STATUS.put(server, Boolean.TRUE);
                    return server;
                } else if (state) {
                    return server;
                }
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
     * Sends the packet.
     *
     * @param packet the packet.
     * @param server the target server.
     * @throws IOException
     */
    private void send(Packet packet, RADIUSServer server)
            throws IOException {
        if (server == null) {
            throw new IOException("No RADIUS server is online.");
        }
        byte[] buffer = new byte[4096];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        dp.setPort(server.getPort());
        dp.setAddress(InetAddress.getByName(server.getHost()));
        byte[] data = packet.getOctets();
        dp.setLength(data.length);
        dp.setData(data);
        socket.send(dp);
        if (debug.isDebugEnabled()) {
            debug.debug("Sent " + packet);
        }
    }

    /**
     * Blocking call that waits until a response packet for the given request is received. Responses that do not
     * correspond to the request are discarded and the call continues to wait, until the socket timeout expires.
     *
     * @param request the request the response is expected to correspond to.
     * @return the received packet.
     * @throws SocketTimeoutException if the socket timeout expires before a corresponding response is received.
     * @throws IOException
     */
    private Packet receive(Packet request)
            throws IOException {
        int soTimeout = socket.getSoTimeout();
        // A socket timeout of 0 means block indefinitely, so there is no deadline to bound the wait against; waiting
        // indefinitely for a response that corresponds to the request is what that configuration asks for.
        boolean bounded = soTimeout > 0;
        long deadline = currentTimeMillis() + soTimeout;
        int discarded = 0;
        try {
            while (true) {
                if (bounded) {
                    long remaining = deadline - currentTimeMillis();
                    if (remaining <= 0) {
                        throw abandonedAfterDiscarding(discarded);
                    }
                    // Re-arm the socket with only what remains of the original timeout. Bounding the loop by the
                    // remaining time rather than by a count of discarded responses means an unsolicited-response
                    // flood can neither extend the total wait beyond the configured timeout nor cause the request
                    // to be abandoned while time still remains for the genuine reply to arrive.
                    socket.setSoTimeout((int) remaining);
                }
                byte[] buffer = new byte[4096];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(dp);
                } catch (SocketTimeoutException e) {
                    // Where responses were discarded, report why rather than letting the socket's own bare timeout
                    // surface with no RADIUS-specific detail for the operator.
                    throw (discarded == 0) ? e : abandonedAfterDiscarding(discarded);
                }
                byte[] data = dp.getData();
                Packet p;
                try {
                    p = PacketFactory.toPacket(data);
                } catch (RuntimeException e) {
                    // A malformed datagram (e.g. a declared length inconsistent with the actual attribute data) can
                    // make PacketFactory throw rather than return null; treat it the same as any other unparseable
                    // response rather than letting it escape the discard loop uncaught. The cause is logged so that
                    // a parsing defect on a well-formed packet remains distinguishable from a hostile datagram.
                    debug.debug("Failed to parse RADIUS response from {}:{}", dp.getAddress(), dp.getPort(), e);
                    p = null;
                }
                if (debug.isDebugEnabled() && p != null) {
                    debug.debug("Received " + p + " size=" + p.getAttributeSet().size());
                }
                String mismatch = (p == null) ? "unparseable or unsupported packet type"
                        : describeMismatch(p, request);
                if (mismatch == null) {
                    return p;
                }
                discarded++;
                debug.debug("Discarding RADIUS response from {}:{} ({}); {} discarded so far", dp.getAddress(),
                        dp.getPort(), mismatch, discarded);
            }
        } finally {
            if (bounded) {
                socket.setSoTimeout(soTimeout);
            }
        }
    }

    /**
     * Reports the abandonment of a request for which every response received was discarded, and provides the
     * exception with which to abandon it.
     *
     * @param discarded the number of responses that were discarded.
     * @return the exception to throw.
     */
    private SocketTimeoutException abandonedAfterDiscarding(int discarded) {
        String reason = "Discarded " + discarded + " RADIUS response(s) that did not correspond to the request; "
                + "none carried the expected identifier and Response Authenticator. Either the shared secret does "
                + "not match the RADIUS server's, or the responses were not produced by it.";
        debug.warn(reason);
        // SocketTimeoutException (an IOException) is returned deliberately, rather than a plain IOException, so that
        // sendPacket(...)'s existing catch (IOException ioe) block recognises it as a retryable failure, marks the
        // server offline, and fails over to the next configured server exactly as it does for a genuine timeout.
        return new SocketTimeoutException(reason);
    }

    /**
     * Indicates why a received response does not correspond to the given request, discarding responses bearing an
     * identifier other than the request's or a Response Authenticator that the shared secret could not have produced.
     *
     * @param response the received response.
     * @param request the request the response is expected to correspond to.
     * @return the reason the response does not correspond to the request, or {@code null} if it does correspond.
     */
    private String describeMismatch(Packet response, Packet request) {
        // Only the low octet of the identifier travels on the wire, so compare the identifiers as the single unsigned
        // octet that RFC 2865 defines.
        if ((response.getIdentifier() & 0xFF) != (request.getIdentifier() & 0xFF)) {
            return "unexpected identifier " + (response.getIdentifier() & 0xFF) + ", expected "
                    + (request.getIdentifier() & 0xFF);
        }
        if (!responseAuthenticatorValidator.isValid(response, request.getAuthenticator(), secret)) {
            return "invalid Response Authenticator, likely due to a mismatched shared secret";
        }
        return null;
    }

    /**
     * Generates an access request packet.
     *
     * @return the access request packet.
     */
    private AccessRequest createAccessRequest() {
        RequestAuthenticator ra = new RequestAuthenticator(random);
        AccessRequest req = new AccessRequest(getIdentifier(), ra);
        return req;
    }

    /**
     * A monitor that periodically checks for currently available remote servers to connect to.
     */
    private class RADIUSMonitor extends GeneralTaskRunnable {
        /**
         * Indicates if monitoring is cancelled.
         */
        private boolean cancelled = false;

        /**
         * Not implemented in this monitor. Always returns false.
         *
         * @param key unused parameter.
         * @return always returns false.
         */
        public boolean addElement(Object key) {
            return false;
        }

        /**
         * Not implemented in this monitor. Always returns false.
         *
         * @param key unused parameter.
         * @return always returns false.
         */
        public boolean removeElement(Object key) {
            return false;
        }

        /**
         * Always returns true.
         *
         * @return always returns true.
         */
        public boolean isEmpty() {
            return true;
        }

        /**
         * Returns the run period of this TaskRunnable.
         *
         * @return A long value to indicate the run period.
         */
        public long getRunPeriod() {
            return cancelled ? 0 : healthCheckInterval * 60000;
        }

        /**
         * Scans through the set of servers testing connectivity and marking the servers accordingly.
         */
        public void run() {
            Map<RADIUSServer, Boolean> tmp;
            if (debug.isDebugEnabled()) {
                debug.debug("Checking server statuses");
            }
            synchronized (SERVER_STATUS) {
                tmp = new LinkedHashMap<RADIUSServer, Boolean>(SERVER_STATUS);
            }
            DatagramSocket testSocket = null;
            int offline = 0;
            for (Map.Entry<RADIUSServer, Boolean> entry : tmp.entrySet()) {
                RADIUSServer server = entry.getKey();
                if (!entry.getValue()) {
                    offline++;
                    try {
                        testSocket = new DatagramSocket();
                        testSocket.setSoTimeout(socket.getSoTimeout());
                        AccessRequest req = createAccessRequest();
                        req.addAttribute(new UserNameAttribute("nonexistent"));
                        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                                secret, "invalidpass"));
                        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
                        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
                        byte[] buffer = new byte[4096];
                        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                        dp.setPort(server.getPort());
                        dp.setAddress(InetAddress.getByName(server.getHost()));
                        byte[] data = req.getOctets();
                        dp.setLength(data.length);
                        dp.setData(data);
                        testSocket.send(dp);
                        byte[] buffer2 = new byte[4096];
                        dp = new DatagramPacket(buffer2, buffer2.length);
                        testSocket.receive(dp);
                        dp.getData();
                        if (debug.isDebugEnabled()) {
                            debug.debug("Moving server to online state - " + server);
                        }
                        synchronized (SERVER_STATUS) {
                            SERVER_STATUS.put(server, Boolean.TRUE);
                        }
                        offline--;
                    } catch (Exception ex) {
                        if (debug.isDebugEnabled()) {
                            debug.debug("Exception occured while checking RADIUS server status: " + ex.getMessage());
                        }
                        //server is still unavailable
                    } finally {
                        if (testSocket != null) {
                            testSocket.disconnect();
                        }
                    }
                }
            }
            if (offline == 0) {
                cancelled = true;
            }
        }
    }
}
