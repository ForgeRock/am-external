/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.wsfederation.common;

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;

/**
 * A {@link WSFederationException} type that specifically handles error situations corresponding to WS-Federation Active
 * Requestor Profile.
 */
public class ActiveRequestorException extends WSFederationException {

    private static final Debug DEBUG = Debug.getInstance("libWSFederation");
    private final QName faultCode;
    private final String key;
    private int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    private ActiveRequestorException(QName faultCode, String key, String... args) {
        super(WSFederationConstants.BUNDLE_NAME, key, args);
        this.faultCode = faultCode;
        this.key = key;
    }

    /**
     * Constructs a new exception instance for client side errors.
     *
     * @param key The localization key to use when constructing the SOAP fault.
     * @param args Additional parameters for the localized message.
     * @return An exception instance which represents the error state.
     */
    public static ActiveRequestorException newSenderException(String key, String... args) {
        return new ActiveRequestorException(SOAPConstants.SOAP_SENDER_FAULT, key, args);
    }

    /**
     * Constructs a new exception instance for server side errors.
     *
     * @param key The localization key to use when constructing the SOAP fault.
     * @param args Additional parameters for the localized message.
     * @return An exception instance which represents the error state.
     */
    public static ActiveRequestorException newReceiverException(String key, String... args) {
        return new ActiveRequestorException(SOAPConstants.SOAP_RECEIVER_FAULT, key, args);
    }

    /**
     * Constructs a new exception instance for server side errors based on exception objects.
     *
     * @param throwable The exception that should be encapsulated in the error message.
     * @return An exception instance which represents the error state.
     */
    public static ActiveRequestorException newReceiverException(Throwable throwable) {
        return new ActiveRequestorException(SOAPConstants.SOAP_RECEIVER_FAULT, "unexpectedError",
                throwable.getMessage());
    }

    /**
     * Returns the exception's SOAP Fault representation that can be returned back to the SOAP client as a response.
     *
     * @return The exception's SOAP Fault representation. May return null, if there was a problem while creating the
     * SOAP objects.
     */
    public SOAPMessage getSOAPFault() {
        try {
            MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
            SOAPFault fault = envelope.getBody().addFault(faultCode, getMessage(), Locale.ENGLISH);
            fault.appendFaultSubcode(new QName(WSFederationConstants.WSSE_NAMESPACE, key, "wsse"));
            return soapMessage;
        } catch (SOAPException se) {
            DEBUG.error("An error occurred while creating SOAP fault", se);
            return null;
        }
    }

    /**
     * Returns the HTTP status that should be used when returning SOAP Fault on HTTP response.
     *
     * @return this with status code set
     */
    public ActiveRequestorException setStatusCode(int status) {
        statusCode = status;
        return this;
    }

    /**
     * Returns the HTTP status that should be used when returning SOAP Fault on HTTP response.
     * Unless changed by {@link #setStatusCode(int)}, it returns status code (500).
     *
     * @return The HTTP status that should be returned by the SOAP Fault.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
