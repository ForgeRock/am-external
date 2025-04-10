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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.monitoring;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.forgerock.openam.radius.server.events.AuthRequestAcceptedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestReceivedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestRejectedEvent;
import org.forgerock.openam.radius.server.events.PacketProcessedEvent;
import org.forgerock.openam.radius.server.events.PacketReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Collates information for JMX reporting and monitoring.
 */
public class RadiusServerEventRegistrar implements RadiusServerEventRegistrator, RadiusServerEventMonitorMXBean {

    // private static Logger logger = LoggerFactory.getLogger(RadiusServerConstants.RADIUS_SERVER_LOGGER);
    private static final Logger LOG = LoggerFactory.getLogger(RadiusServerEventRegistrar.class);

    private final AtomicLong noOfPacketsReceived = new AtomicLong();
    private final AtomicLong noOFPacketsAccepted = new AtomicLong();
    private final AtomicLong noOfPacketsProcessed = new AtomicLong();
    private final AtomicLong noOfAuthRequestsAccepted = new AtomicLong();
    private final AtomicLong noOfAuthRequestsRejected = new AtomicLong();

    /**
     * Constructor. Registers this class with the radius events bus, such that it will be a subscriber to radius events.
     * So that the radius packet and request statistics can be reported to JMX clients such as visualvm this class
     * registers itself with the platforms MBean server. If this bean is already registered with the server, then it
     * will be unregistered and then re-registered.
     *
     * @param eventBus - the event bus that this class will register as a subscriber with.
     */
    @Inject
    public RadiusServerEventRegistrar(@Named("RadiusEventBus") EventBus eventBus) {
        LOG.debug("Entering RadiusServerEventRegistrar.RadiusServerEventRegistrar");
        LOG.debug("RadiusServerEventRegistrar - Registering with EventBus hashCode; " + eventBus.hashCode());
        eventBus.register(this);

        ObjectName name = null;
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final String procName = ManagementFactory.getRuntimeMXBean().getName();
            name = new ObjectName("OpenAM" + ":type=RadiusServer");
            if (server.isRegistered(name)) {
                LOG.debug("MBean with name " + name + " is already registered. Unregistering.");
                server.unregisterMBean(name);
            }

            server.registerMBean(this, name);
            LOG.debug("Registered MBean with name '{}'", name.toString());

        } catch (final Exception e) {
            LOG.error("Unable to register MBean", e);
        }

        LOG.debug("Leaving RadiusServiceEntryRegistrar()");
    }

    ///////////////////
    // Packets Received

    /**
     * Once an object of this class has registered with the eventBus (passed into the constructor) the
     * <code>EventBus</code> will call this method when any <code>PacketReceivedEvent</code> objects are posted.
     *
     * @param receivedEvent - the event that was posted to the <code>EventBus</code>
     */
    @Subscribe
    public void packetReceived(PacketReceivedEvent receivedEvent) {
        LOG.debug("RadiusServerEventRegistrar.packetReceived() called by EventBus");
        packetReceived();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerStateUpdator#packetReceived()
     */
    @Override
    public long packetReceived() {
        long total = noOfPacketsReceived.incrementAndGet();
        LOG.debug("RadiusServerEventRegistrar.packetReceived() - total now " + total);
        return total;
    }

    /* (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerMonitoring#getNumberOfPacketsRecieved()
     */
    @Override
    public long getNumberOfPacketsRecieved() {
        long total = noOfPacketsReceived.get();
        LOG.debug("RadiusServerEventRegistrar.getNumberOfPacketsRecieved() returning " + total);
        return total;
    }

    ////////////////////
    // Packets accepted.

    /**
     * Once an object of this class has registered with the eventBus (passed into the constructor) the
     * <code>EventBus</code> will call this method when any <code>AuthRequestReceivedEvent</code> objects are posted.
     *
     * @param receivedRadiusEvent - the event that was posted to the <code>EventBus</code>
     */
    @Subscribe
    public void packetAccepted(AuthRequestReceivedEvent receivedRadiusEvent) {
        LOG.debug("RadiusServerEventRegistrar.packetAccepted() called by EventBus");
        this.packetAccepted();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerStateUpdator#packetAccepted()
     */
    @Override
    public long packetAccepted() {
        long total = noOFPacketsAccepted.incrementAndGet();
        LOG.debug("RadiusServerEventRegistrar.packetAccepted() - total now " + total);
        return total;
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerMonitoring#getNumberOfAcceptedPackets()
     */
    @Override
    public long getNumberOfAcceptedPackets() {
        long total = noOFPacketsAccepted.get();
        LOG.debug("RadiusServerEventRegistrar.getNumberOfAcceptedPackets() returning " + total);
        return total;
    }

    /////////////////////
    // Packets processed.

    /**
     * Once an object of this class has registered with the eventBus (passed into the constructor) the
     * <code>EventBus</code> will call this method when any <code>PacketProcessedEvent</code> objects are posted.
     *
     * @param packetProcessedEvent - the event that was posted to the <code>EventBus</code>
     */
    @Subscribe
    public void packetProcessed(PacketProcessedEvent packetProcessedEvent) {
        LOG.debug("RadiusServerEventRegistrar.packetProcessed() called by EventBus");
        packetProcessed();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator#packetProcessed()
     */
    @Override
    public long packetProcessed() {
        long total = noOfPacketsProcessed.incrementAndGet();
        LOG.debug("RadiusServerEventRegistrar.packetProcessed() - total now " + total);
        return total;
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventMonitorMXBean#getNumberOfPacketsProcessed()
     */
    @Override
    public long getNumberOfPacketsProcessed() {
        long total = noOfPacketsProcessed.get();
        LOG.debug("RadiusServerEventRegistrar.getNumberOfPacketsProcessed() returning " + total);
        return total;
    }

    /////////////////////
    // Requests Accepted.

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator#authRequestAccepted()
     */

    /**
     * Once an object of this class has registered with the eventBus (passed into the constructor) the
     * <code>EventBus</code> will call this method when any <code>AuthRequestAcceptedEvent</code> objects are posted.
     *
     * @param acceptedEvent - the event that was posted to the <code>EventBus</code>
     */
    @Subscribe
    public void authRequestAccepted(AuthRequestAcceptedEvent acceptedEvent) {
        LOG.debug("RadiusServerEventRegistrar.authRequestAccepted() called by EventBus");
        authRequestAccepted();
    }

    @Override
    public long authRequestAccepted() {
        long total = noOfAuthRequestsAccepted.incrementAndGet();
        LOG.debug("ID: " + this.hashCode()
                + "RadiusServerEventRegistrar.authRequestAccepted() - incrementing. Total accepted " + total);
        return total;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.forgerock.openam.radius.server.monitoring.RadiusServerEventMonitorMXBean#getNumberOfAuthRequestsAccepted()
     */
    @Override
    public long getNumberOfAuthRequestsAccepted() {
        long total = noOfAuthRequestsAccepted.get();
        LOG.debug("RadiusServerEventRegistrar.getNumberOfAuthRequestsAccepted() - returning " + total);
        return total;
    }

    /////////////////////
    // Requests Rejected.
    /**
     * Once an object of this class has registered with the eventBus (passed into the constructor) the
     * <code>EventBus</code> will call this method when any <code>AuthRequestRejectedEvent</code> objects are posted.
     *
     * @param authRequestRejectedEvent - the event that was posted to the <code>EventBus</code>
     */
    @Subscribe
    public void authRequestRejected(AuthRequestRejectedEvent authRequestRejectedEvent) {
        LOG.debug("Entering RadiusServerEventRegistrar.authRequestRejected()");
        authRequestRejected();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator#authRequestRejected()
     */
    @Override
    public long authRequestRejected() {
        return noOfAuthRequestsRejected.incrementAndGet();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.forgerock.openam.radius.server.monitoring.RadiusServerEventMonitorMXBean#getNumberOfAuthRequestsRejected()
     */
    @Override
    public long getNumberOfAuthRequestsRejected() {
        return noOfAuthRequestsRejected.get();
    }
}
