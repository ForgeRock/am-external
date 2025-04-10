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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions copyright 2015-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server;

import static org.forgerock.openam.audit.context.AuditRequestContext.setAuditRequestContext;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.VALUE_LENGTH;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.VALUE_START_POSITION;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.server.events.AuthRequestAcceptedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestChallengedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestRejectedEvent;
import org.forgerock.openam.radius.server.events.PacketDroppedSilentlyEvent;
import org.forgerock.openam.radius.server.events.PacketProcessedEvent;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;
import org.forgerock.services.TransactionId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * Handles valid (ie: from approved clients) incoming radius access-request packets passing responsibility for
 * generating a response to the client's declared handler class. The handler results are returned to the request thread
 * via a promise. This allows a catastrophic failure in one off the handler threads to affect a shutdown of the listener
 * thread and the executors. It also allows for retrying of requests that fail for temporary reasons (e.g. network
 * connection issues) although this is not yet implemented.
 */
public class RadiusRequestHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RadiusRequestHandler.class);

    /**
     * Buffer containing the on-the-wire bytes of the request prior to parsing.
     */
    private final ByteBuffer buffer;

    /**
     * The ResponseContext object providing access to client handlerConfig, receiving channel, and remote user identity.
     */
    private final RadiusRequestContext requestContext;

    /**
     * The event bus is used by handlers and by this class to notify listeners of events occurring with the lifetime of
     * a radius request.
     */
    private final EventBus eventBus;

    /**
     * factory that will attempt to construct the access request handler class.
     */
    private final AccessRequestHandlerFactory accessRequestHandlerFactory;

    private final MessageAuthenticatorCalculator messageAuthenticatorCalculator;

    /**
     * Constructs a request handler.
     *
     * @param accessRequestHandlerFactory - a factory object that will construct access request handlers used to handle
     * the radius requests.
     * @param reqCtx a <code>RadiusRequestContext</code> object. Must be non-null.
     * @param buffer an {@code ByteBuffer} containing the bytes received by a radius handler.
     * @param eventBus used to notify interested parties of events occurring during the processing of radius requests.
     * @param messageAuthenticatorCalculator used to calculate the Message-Authenticator attribute value.
     */
    public RadiusRequestHandler(AccessRequestHandlerFactory accessRequestHandlerFactory,
            final RadiusRequestContext reqCtx, final ByteBuffer buffer,
            final EventBus eventBus, MessageAuthenticatorCalculator messageAuthenticatorCalculator) {
        LOG.debug("Entering RadiusRequestHandler.RadiusRequestHandler()");
        this.requestContext = reqCtx;
        this.buffer = buffer;
        this.eventBus = eventBus;
        this.accessRequestHandlerFactory = accessRequestHandlerFactory;
        this.messageAuthenticatorCalculator = messageAuthenticatorCalculator;
        LOG.debug("Leaving RadiusRequestHandler.RadiusRequestHandler()");
    }

    /**
     * Returns the name of the client from which the packet was received.
     *
     * @return the name of the client from which the packet was received.
     */
    public String getClientName() {
        return requestContext.getClientName();
    }

    @Override
    public void run() {
        try {
            String requestId = UUID.randomUUID().toString();
            // This sets the request context so that when the OpenAM auth chains etc use the AuditRequestContext they
            // will use the same transaction id. This means log entries across the audit logs can be tied up.
            setAuditRequestContext(new AuditRequestContext(new TransactionId(requestId)));

            if (requestContext.getClientConfig().isMessageAuthenticatorRequired()
                    && buffer != null && PacketType.ACCESS_REQUEST == PacketType.getPacketType(buffer.duplicate().get())
                    && !containsValidMessageAuthenticator(buffer)) {
                LOG.debug("Message-Authenticator is missing or invalid. Dropping the request");
                return;
            }

            LOG.debug("Entering RadiusRequestHandler.run();");
            final Packet requestPacket = getValidPacket(buffer);
            if (requestPacket == null) {
                LOG.debug("Leaving RadiusRequestHandler.run(); no requestPacket");
                return;
            }

            // grab the items from the request that we'll need in the RadiusResponseHandler at send time
            requestContext.setRequestId(requestPacket.getIdentifier());
            requestContext.setRequestAuthenticator(requestPacket.getAuthenticator());

            final AccessRequest accessRequest = createAccessRequest(requestPacket);
            if (accessRequest == null) {
                LOG.debug("Leaving RadiusRequestHandler.run(); Packet received was not an AccessRequest packet.");
                return;
            }

            // Instantiate an instance of the AccessRequestHandler class specified in the configuration for this
            // client.
            final AccessRequestHandler accessRequestHandler = accessRequestHandlerFactory
                    .getAccessRequestHandler(requestContext);
            if (accessRequestHandler == null) {
                LOG.debug("Leaving RadiusRequestHandler.run(); Could not obtain Access Request Handler.");
                return;
            }

            final RadiusRequest request = new RadiusRequest(accessRequest, requestId);
            final RadiusResponse response = new RadiusResponse();

            try {
                // The handler will form the response.
                accessRequestHandler.handle(request, response, requestContext);
                postHandledEvent(request, response, requestContext);
                // Send the response to the client.
                Packet responsePacket = response.getResponsePacket();
                requestContext.send(responsePacket);
                eventBus.post(new PacketProcessedEvent());
            } catch (final RadiusProcessingException rre) {
                // So the processing of the request failed.
                handleResponseException(rre, requestContext);
            }

        } catch (final Exception t) {
            String sb = "Exception occurred while handling radius request for RADIUS client '"
                    + getClientName() + "'. Rejecting access.";
            LOG.error(sb, t);

            this.sendAccessReject(requestContext);
        }
    }

    private boolean containsValidMessageAuthenticator(ByteBuffer buffer) {
        Optional<ByteBuffer> calculatedValue = messageAuthenticatorCalculator.computeFromPayloadIfPresent(buffer,
                requestContext.getClientSecret());

        return calculatedValue.isPresent() && MessageDigest.isEqual(calculatedValue.get().array(),
                Arrays.copyOfRange(buffer.array(), VALUE_START_POSITION, VALUE_START_POSITION + VALUE_LENGTH));
    }

    private void postHandledEvent(RadiusRequest request, RadiusResponse response, RadiusRequestContext requestContext) {
        LOG.debug("Entering RadiusRequestHandler.postHandledEvent()");

        // Calculate and set the time to service the response.
        response.setTimeToServiceRequestInMilliSeconds(
                DateTime.now().getMillis() - request.getStartTimestampInMillis());

        Packet responsePacket = response.getResponsePacket();
        if (responsePacket != null) {
            switch (responsePacket.getType()) {
            case ACCESS_ACCEPT:
                eventBus.post(new AuthRequestAcceptedEvent(request, response, requestContext));
                break;
            case ACCESS_CHALLENGE:
                eventBus.post(new AuthRequestChallengedEvent(request, response, requestContext));
                break;
            case ACCESS_REJECT:
                eventBus.post(new AuthRequestRejectedEvent(request, response, requestContext));
                break;
            case ACCOUNTING_RESPONSE:
                break;
            default:
                LOG.warn("Unexpected type of responsePacket: {}", responsePacket.getType().toString());
                break;
            }
        }
        LOG.debug("Leaving RadiusRequestHandler.postHandledEvent()");
    }

    /**
     * Cast the request packet into an access request packet. If this is not possible a log entry is made and null is
     * returned.
     *
     * @param requestPacket - the request packet received from the client.
     * @return the <code>AccessRequest</code> object, or null if one could not be derived from requestPacket.
     */
    private AccessRequest createAccessRequest(Packet requestPacket) {
        try {
            return (AccessRequest) requestPacket;
        } catch (final ClassCastException c) {
            // should never happen
            String sb = "Received packet of type ACCESS_REQUEST from RADIUS client '"
                    + getClientName() + "' but unable to cast to AccessRequest. Rejecting access.";
            LOG.error(sb, c);
            try {
                requestContext.send(new AccessReject());
            } catch (final RadiusProcessingException e) {
                LOG.warn("Failed to send AccessReject() response to client.");
            }
            return null;
        }
    }

    /**
     * Returns a <code>Packet</code> object that represents the incoming radius request.
     *
     * @param buffer2 - buffer containing the bytes to create the packet from.
     * @return the radius request packet, or null if the packet could not be created.
     */
    private Packet getValidPacket(ByteBuffer buffer2) {
        LOG.debug("Entering RadiusRequestHandler.getValidPacket()");
        // parse into a packet object
        Packet requestPacket = null;

        try {
            requestPacket = PacketFactory.toPacket(buffer2);

            // log packet if client handlerConfig indicates
            if (requestContext.getClientConfig().isLogPackets()) {
                requestContext.logPacketContent(requestPacket, "\nPacket from " + getClientName() + ":");
            }

            // verify packet type
            if (requestPacket.getType() != PacketType.ACCESS_REQUEST) {
                LOG.error("Received non Access-Request packet from RADIUS client '" + getClientName() + "'. Dropping.");
            }
        } catch (final Exception e) {
            LOG.warn("Unable to parse packet received from RADIUS client '" + getClientName() + "'. Dropping.", e);
        }
        LOG.debug("Leaving RadiusRequestHandler.getValidPacket()");
        return requestPacket;
    }

    /**
     * Sets the handler's exception. If the exception is only temporary we can have a go at sending an access reject
     * response, if the exception is only a temporary failure then this method will try to send
     *
     * @param rre
     */
    private void handleResponseException(RadiusProcessingException rre, RadiusRequestContext reqCtx) {
        LOG.error("Failed to process a radius request for RADIUS client '" + reqCtx.getClientName() + "'.");

        if (rre.getNature() == RadiusProcessingExceptionNature.TEMPORARY_FAILURE) {
            sendAccessReject(reqCtx);
        }
        final RadiusProcessingExceptionNature nature = rre.getNature();
        switch (nature) {
        case CATASTROPHIC:
            // currently, the only catastrophic event is thrown in RadiusRequestContext.injectResponseAuthenticator().
            // But the listener checks for md5 and UTF-8 encoding so that we should never run into the exceptions that
            // trigger a catastrophic event. So for completeness log it. If we ever see these then we can pass the
            // listener into this class and call its terminate() method in this case.
            LOG.error("Catastrophic error processing a RADIUS request.", rre);
            eventBus.post(new PacketDroppedSilentlyEvent());
            break;
        case INVALID_RESPONSE:
            LOG.error("Failed to handle request. This request will be ignored.", rre);
            eventBus.post(new PacketDroppedSilentlyEvent());
            break;
        case TEMPORARY_FAILURE:
            final String errStr = "Failed to handle request. This request could be retried, but that is"
                    + " currently not implemented.";
            LOG.error(errStr, rre);
            break;
        default:
            break;
        }
    }

    /**
     * Attempts to send an AccessReject message to the client. Failed attempts will be logged.
     *
     * @param reqCtx - the RadiusRequestContext that will be used to send the AccessReject packet.
     */
    private void sendAccessReject(RadiusRequestContext reqCtx) {
        try {
            reqCtx.send(new AccessReject());
            LOG.debug("Rejected access request.");
        } catch (final Exception e1) {
            LOG.warn("Failed to send AccessReject() response to client.");
        }
    }
}
