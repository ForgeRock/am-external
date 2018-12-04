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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.exceptions;

/**
 * Parent of exceptions triggered during registration.
 */
public abstract class WebAuthnRegistrationException extends Exception {

    /**
     * Construct a new webauthn registration exception with message.
     *
     * @param message the message
     */
    WebAuthnRegistrationException(String message) {
        super(message);
    }

    /**
     * Construct a new webauthn registration exception with message and cause.
     *
     * @param message the message
     * @param throwable the cause
     */
    WebAuthnRegistrationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
