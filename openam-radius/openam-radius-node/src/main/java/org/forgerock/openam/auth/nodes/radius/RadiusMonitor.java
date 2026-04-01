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

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.TimerPool;

/**
 * A monitor that periodically checks for currently available remote RADIUS servers.
 * Each monitor is associated with a specific connection and its set of primary and secondary servers.
 */
public class RadiusMonitor extends GeneralTaskRunnable {
    private static final Logger logger = LoggerFactory.getLogger(RadiusMonitor.class);

    /**
     * Map of down servers keyed by RADIUS server.
     */
    private final Set<RADIUSServer> unavailableServers = ConcurrentHashMap.newKeySet();
    /**
     * Source of random bytes for health checks.
     */
    private final SecureRandom random = new SecureRandom();
    /**
     * The source of id generation for access request packets.
     */
    private short id = (short) currentTimeMillis();
    private String secret;
    private int socketTimeout;
    private int healthCheckInterval;
    private MessageAuthenticatorCalculator messageAuthenticatorCalculator;

    private final TimerPool timerPool;
    private final Object scheduleLock = new Object();
    private volatile boolean cancelled = false;

    /**
     * Creates a new RADIUS monitor.
     *
     * @param secret                         the shared secret
     * @param socketTimeout                  the socket timeout in seconds
     * @param healthCheckInterval            the health check interval in minutes
     * @param messageAuthenticatorCalculator the message authenticator calculator, or null if not required
     * @param timerPool                      the timer pool for scheduling health checks
     */
    public RadiusMonitor(String secret,
                         int socketTimeout, int healthCheckInterval,
                         MessageAuthenticatorCalculator messageAuthenticatorCalculator,
                         TimerPool timerPool) {
        this.secret = secret;
        this.socketTimeout = socketTimeout;
        this.healthCheckInterval = healthCheckInterval;
        this.messageAuthenticatorCalculator = messageAuthenticatorCalculator;
        this.timerPool = timerPool;
    }

    /**
     * Updates the monitor settings.
     *
     * @param secret                         the shared secret
     * @param socketTimeout                  the socket timeout in seconds
     * @param healthCheckInterval            the health check interval in minutes
     * @param messageAuthenticatorCalculator the message authenticator calculator, or null if not required
     */
    public void updateSettings(String secret, int socketTimeout, int healthCheckInterval,
            MessageAuthenticatorCalculator messageAuthenticatorCalculator) {
        this.secret = secret;
        this.socketTimeout = socketTimeout;
        this.healthCheckInterval = healthCheckInterval;
        this.messageAuthenticatorCalculator = messageAuthenticatorCalculator;
        this.cancelled = false;
    }

    /**
     * Marks a server as down and starts monitoring it.
     *
     * @param server the RADIUS server
     */
    public void markServerDown(RADIUSServer server) {
        unavailableServers.add(server);
        cancelled = false;
        scheduleIfNeeded();
    }

    private void scheduleIfNeeded() {
        if (timerPool == null) {
            return;
        }
        synchronized (scheduleLock) {
            if (isScheduled()) {
                return;
            }
            try {
                long now = currentTimeMillis();
                timerPool.schedule(this, new Date((now / 1000) * 1000));
            } catch (RuntimeException e) {
                logger.warn("Failed to schedule RADIUS monitor", e);
            }
        }
    }

    /**
     * Checks if a server is currently marked as down.
     *
     * @param server the RADIUS server
     * @return true if the server is down, false otherwise
     */
    public boolean isServerDown(RADIUSServer server) {
        return unavailableServers.contains(server);
    }

    /**
     * Cancels this monitor, stopping future health checks.
     */
    @Override
    public void cancel() {
        cancelled = true;
        super.cancel();
    }

    /**
     * Not implemented in this monitor. Always returns false.
     *
     * @param key unused parameter.
     * @return always returns false.
     */
    @Override
    public boolean addElement(Object key) {
        return false;
    }

    /**
     * Not implemented in this monitor. Always returns false.
     *
     * @param key unused parameter.
     * @return always returns false.
     */
    @Override
    public boolean removeElement(Object key) {
        return false;
    }

    /**
     * Always returns true.
     *
     * @return always returns true.
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * Returns the run period of this TaskRunnable.
     *
     * @return A long value to indicate the run period in milliseconds, or 0 if cancelled.
     */
    @Override
    public long getRunPeriod() {
        return cancelled ? 0 : healthCheckInterval * 60000L;
    }

    /**
     * Scans through the set of servers testing connectivity and marking the servers accordingly.
     */
    @Override
    public void run() {
        if (cancelled) {
            return;
        }

        logger.debug("Checking server statuses");

        for (RADIUSServer server :unavailableServers) {
            try (DatagramSocket testSocket = new DatagramSocket()) {
                testSocket.setSoTimeout(socketTimeout * 1000);
                AccessRequest req = createAccessRequest();
                req.addAttribute(new UserNameAttribute("nonexistent"));
                req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                        secret, "invalidPass"));
                req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
                req.addAttribute(new NASPortAttribute(testSocket.getLocalPort()));
                // Send the request
                byte[] buffer = new byte[4096];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                dp.setPort(server.getPort());
                dp.setAddress(InetAddress.getByName(server.getHost()));
                if (messageAuthenticatorCalculator != null) {
                    messageAuthenticatorCalculator.addMessageAuthenticator(req, secret);
                }
                byte[] data = req.getOctets();
                dp.setLength(data.length);
                dp.setData(data);
                testSocket.send(dp);

                // Wait for any response (we don't care about the content)
                byte[] buffer2 = new byte[4096];
                dp = new DatagramPacket(buffer2, buffer2.length);
                testSocket.receive(dp);
                dp.getData();

                logger.debug("Moving server to online state - {}", server);
                unavailableServers.remove(server);

            } catch (Exception e) {
                logger.debug("Exception occurred while checking RADIUS server status: {}", e.getMessage());
            }
        }

        // If all servers for this monitor are online, cancel the monitor
        if (unavailableServers.isEmpty()) {
            cancelled = true;
        }
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
     * Generates an access request packet.
     *
     * @return the access request packet.
     */
    private AccessRequest createAccessRequest() {
        RequestAuthenticator ra = new RequestAuthenticator(random);
        return new AccessRequest(getIdentifier(), ra);
    }
}

