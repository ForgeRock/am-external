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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.exceptions;

/**
 * AM requested the user be verified, but the authenticator didn't claim they were.
 */
public class UserNotVerifiedException extends WebAuthnRegistrationException {

    /**
     * Constructor.
     *
     * @param message the message to pass on
     */
    public UserNotVerifiedException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message   the message to pass on
     * @param throwable the cause
     */
    public UserNotVerifiedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
