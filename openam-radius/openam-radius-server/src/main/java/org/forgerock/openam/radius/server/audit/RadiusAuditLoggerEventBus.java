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
 * Copyright 2015-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.audit;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;
import org.forgerock.openam.radius.server.events.AcceptedRadiusEvent;
import org.forgerock.openam.radius.server.events.AuthRequestAcceptedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestChallengedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestReceivedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Makes audit logs on behalf of the Radius Server.
 */
public class RadiusAuditLoggerEventBus implements RadiusAuditor {

    private static final Logger LOG = LoggerFactory.getLogger(RadiusAuditLoggerEventBus.class);

    /**
     * Factory from which auditEvents can be created.
     */
    private final AuditEventFactory auditEventFactory;

    /**
     * Class to which audit events should be published.
     */
    private AuditEventPublisher auditEventPublisher;

    /**
     * Constructor.
     *
     * @param eventBus - and event bus that the constructed object will register with in order to be notified of RADIUS
     *            events.
     * @param eventFactory - a factory from which Audit events may be built.
     * @param eventPublisher - the interface through which audit events may be published to the audit handler
     *            sub-system.
     */
    @Inject
    public RadiusAuditLoggerEventBus(@Named("RadiusEventBus") EventBus eventBus, AuditEventFactory eventFactory,
            AuditEventPublisher eventPublisher) {
        LOG.debug("Entering RadiusAuditLogger.RadiusAuditLogger");
        LOG.debug("Registering RadiusAuditLogger with the eventBus, hashCode; {}", eventBus.hashCode());
        eventBus.register(this);
        this.auditEventFactory = eventFactory;
        this.auditEventPublisher = eventPublisher;
        LOG.debug("Leaving RadiusAuditLogger.RadiusAuditLogger");
    }

    /* (non-Javadoc)
     * @see org.forgerock.openam.radius.server.audit.RadiusAuditLogger#recordAccessRequest
     * (org.forgerock.openam.radius.server.events.AccessRequestEvent)
     */
    @Override
    @Subscribe
    public void recordAuthRequestReceivedEvent(AuthRequestReceivedEvent authRequestReceivedEvent) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.recordAuthRequestReceivedEvent()");
        makeLogEntry(EventName.AM_ACCESS_ATTEMPT, authRequestReceivedEvent);
        LOG.debug("Leaving RadiusAuditLoggerEventBus.recordAuthRequestReceivedEvent()");
    }

    @Override
    @Subscribe
    public void recordAuthRequestAcceptedEvent(AuthRequestAcceptedEvent authRequestAcceptedEvent) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.recordAuthRequestAcceptedEvent()");
        makeLogEntry(EventName.AM_ACCESS_OUTCOME, authRequestAcceptedEvent);
        LOG.debug("Leaving RadiusAuditLoggerEventBus.recordAuthRequestAcceptedEvent()");
    }

    @Override
    @Subscribe
    public void recordAuthRequestRejectedEvent(AuthRequestRejectedEvent authRequestRejectedEvent) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.recordAuthRequestRejectedEvent()");
        makeLogEntry(EventName.AM_ACCESS_OUTCOME, authRequestRejectedEvent);
        LOG.debug("Leaving RadiusAuditLoggerEventBus.recordAuthRequestRejectedEvent()");
    }

    @Override
    @Subscribe
    public void recordAuthRequestChallengedEvent(AuthRequestChallengedEvent authRequestChallengedEvent) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.recordAuthRequestRejectedEvent()");
        makeLogEntry(EventName.AM_ACCESS_OUTCOME, authRequestChallengedEvent);
        LOG.debug("Leaving RadiusAuditLoggerEventBus.recordAuthRequestRejectedEvent()");
    }

    /**
     * Makes an 'access' audit log entry.
     *
     * @param eventName - the name of the event.
     * @param accessRequestEvent - the access request event.
     */
    public void makeLogEntry(EventName eventName, AcceptedRadiusEvent accessRequestEvent) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.makeLogEntry()");

        // Due to use of multithreading / EventBus, audit events cannot simply be populated from the AuditRequestContext
        AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(accessRequestEvent.getRealm())
                .timestamp(accessRequestEvent.getTimeOfEvent())
                .eventName(eventName)
                .component(Component.RADIUS);

        String uid = accessRequestEvent.getUniversalId();
        if (!Strings.isNullOrEmpty(uid)) {
            builder.userId(uid);
        } else {
            LOG.debug("Not setting authentication to universal Id. None available.");
        }

        setRequestDetails(builder, accessRequestEvent);

        try {
            setClientDetails(builder, accessRequestEvent.getRequestContext());
            RadiusResponse response = accessRequestEvent.getResponse();

            if (response.getResponsePacket() != null) {
                setResponseDetails(builder, response);
            }
        } catch (RadiusAuditLoggingException e) {
            LOG.warn("Failed to set client details on access audit event. Reason; {}", e.getMessage());
        }

        this.auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, builder.toEvent());
        LOG.debug("Leaving RadiusAuditLoggerEventBus.makeLogEntry()");
    }


    private void setRequestDetails(AMAccessAuditEventBuilder builder, AcceptedRadiusEvent accessRequestEvent) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.setRequestDetails()");

        RadiusRequest request = accessRequestEvent.getRequest();
        if (request != null) {
            Packet packet = request.getRequestPacket();
            if (packet != null) {
                PacketType packetType = packet.getType();
                Short packetId = packet.getIdentifier();
                if (packetType != null && packetId != null) {
                    String operationName = packetType.toString();
                    JsonValue requestId = json(object(
                            field("radiusId", packetId)));
                    builder.request("RADIUS", operationName, requestId);
                }
            }
        }

        LOG.debug("Leaving RadiusAuditLoggerEventBus.setRequestDetails()");
    }

    /**
     * Sets the client details via the access event builder.
     *
     * @param builder - the AccessAuditEventBuilder to which the client details should be added.
     * @param radiusRequestContext
     * @throws RadiusAuditLoggingException
     */
    private void setClientDetails(AMAccessAuditEventBuilder builder, RadiusRequestContext radiusRequestContext)
            throws RadiusAuditLoggingException {
        String clientIPAddress = null;
        InetSocketAddress source = radiusRequestContext.getSource();
        if (source == null) {
            throw new RadiusAuditLoggingException("Could not obtain the source address from the request context.");
        } else {
            int port = source.getPort();
            InetAddress address = source.getAddress();
            if (address == null) {
                throw new RadiusAuditLoggingException("Could not obtain the address from the InetSocketAddress.");

            } else {
                clientIPAddress = address.toString();
                if (Strings.isNullOrEmpty(clientIPAddress)) {
                    throw new RadiusAuditLoggingException("String representation of client's ip address is blank.");
                } else {
                    builder.client(clientIPAddress, port);
                }
            }
        }
    }

    /**
     * Sets the response details of the builder, using the details provided in the <code>RadiusResponse</code>.
     *
     * @param builder
     * @param response
     */
    private void setResponseDetails(AMAccessAuditEventBuilder builder, RadiusResponse response) {
        LOG.debug("Entering RadiusAuditLoggerEventBus.setResponseDetails()");
        ResponseStatus responseStatus = null;
        PacketType packetType = response.getResponsePacket().getType();

        if ((packetType == PacketType.ACCESS_ACCEPT)
                || (packetType == PacketType.ACCESS_CHALLENGE)) {
            responseStatus = ResponseStatus.SUCCESSFUL;
        } else if (packetType == PacketType.ACCESS_REJECT) {
            responseStatus = ResponseStatus.FAILED;
        } else {
            LOG.warn("Unexpected packet type in RadiusAuditLoggerEventBus.setResponseDetails()");
        }

        builder.response(responseStatus,
                packetType.toString(),
                response.getTimeToServiceRequestInMilliSeconds(),
                TimeUnit.MILLISECONDS);

        LOG.debug("Leaving RadiusAuditLoggerEventBus.setResponseDetails()");
    }
}
