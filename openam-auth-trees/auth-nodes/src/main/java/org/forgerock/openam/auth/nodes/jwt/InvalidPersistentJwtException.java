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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.jwt;

/**
 * Represents an exception that occurs when a Persistent JWT is determined as invalid.
 */
public class InvalidPersistentJwtException extends Exception {

    /**
     * Constructs a InvalidPersistentJwtException with a given message.
     *
     * @param message the localized message.
     */
    InvalidPersistentJwtException(String message) {
        super(message);
    }

    /**
     * Constructs a InvalidPersistentJwtException with a exception.
     *
     * @param e the exception to wrap in a InvalidPersistentJwtException.
     */
    InvalidPersistentJwtException(Exception e) {
        super(e);
    }

    /**
     * Constructs a InvalidPersistentJwtException with a given message and exception.
     *
     * @param message the localized message.
     * @param cause the underlying cause.
     */
    InvalidPersistentJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
