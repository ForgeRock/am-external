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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;

/**
 * Represents an error returned from the DOM.
 */
public class WebAuthnDomException extends Exception {
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
        return errorType.toString() + DESCRIPTION_DELIMITER + getMessage();
    }

    /**
     * Construct a new WebAuthnDomException from a known-formatted String.
     *
     * @param errorDescription Formatted string containing a DOM exception.
     * @return an object representing this error.
     */
    public static WebAuthnDomException parse(String errorDescription) {
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        WebAuthnDomException that = (WebAuthnDomException) object;
        return errorType == that.errorType && getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorType, getMessage());
    }
}
