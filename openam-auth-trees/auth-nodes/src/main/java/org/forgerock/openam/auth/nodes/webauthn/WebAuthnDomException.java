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
 * Copyright 2018-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.nodes.webauthn.ClientScriptUtilities.RESPONSE_DELIMITER;

import org.apache.commons.lang.StringUtils;

/**
 * Represents an error returned from the DOM.
 */
public class WebAuthnDomException extends Exception {

    /** Used to indicate we are communicating a DOM Exception back to the server. */
    static final String ERROR_MESSAGE = "ERROR";
    /** Used as a key to transmit DOM exceptions after an error further along the tree. */
    static final String WEB_AUTHENTICATION_DOM_EXCEPTION = "WebAuthenticationDOMException";
    /** Error delimiter. */
    private static final String DESCRIPTION_DELIMITER = ":";

    private final WebAuthnDomExceptionType errorType;

    /**
     * Construct a new exception.
     *
     * @param errorType Original DOM exception that caused the error.
     * @param message The message associated with the DOM Exception.
     */
    WebAuthnDomException(WebAuthnDomExceptionType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * Retrieve the DOM-based error type.
     *
     * @return The DOM error type.
     */
    public WebAuthnDomExceptionType getErrorType() {
        return errorType;
    }

    /**
     * Returns in a format which can be parsed by {@link #parse(String)}.
     *
     * @return String representation of this DOM exception.
     */
    @Override
    public String toString() {
        return ERROR_MESSAGE + RESPONSE_DELIMITER + errorType.toString() + DESCRIPTION_DELIMITER + getMessage();
    }

    /**
     * Construct a new WebAuthnDomException from a known-formatted String.
     *
     * @param description Formatted string containing a DOM exception.
     * @return an object representing this error.
     */
    public static WebAuthnDomException parse(String description) {

        String errorDescription = description.substring((ERROR_MESSAGE + RESPONSE_DELIMITER).length());
        int colonIndex = errorDescription.indexOf(DESCRIPTION_DELIMITER);
        if (colonIndex == -1) {
            return new WebAuthnDomException(WebAuthnDomExceptionType.UnknownError, errorDescription);
        }
        String errorName = errorDescription.substring(0, colonIndex);
        String errorMessage = errorDescription.substring(colonIndex + 1);
        WebAuthnDomExceptionType errorType = WebAuthnDomExceptionType.UnknownError;
        if (StringUtils.isNotBlank(errorName)) {
            errorType = WebAuthnDomExceptionType.valueOf(errorName);
        }
        return new WebAuthnDomException(errorType, errorMessage);
    }

}
