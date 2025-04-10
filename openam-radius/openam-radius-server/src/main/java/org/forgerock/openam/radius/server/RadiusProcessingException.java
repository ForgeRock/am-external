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

package org.forgerock.openam.radius.server;

/**
 * An exception thrown when a response to a RADIUS message could not be sent.
 */
public class RadiusProcessingException extends Exception {

    private final RadiusProcessingExceptionNature nature;

    /**
     * Serial ID makes the exception serializable.
     */
    private static final long serialVersionUID = 1949778158880857742L;

    /**
     * Constructor.
     *
     * @param nature - an enum value indicating whether an attempt should be made to re-send the Radius Response or if
     *            the error is more fundamental and further attempts to send the request will be met with failure.
     * @param message - the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public RadiusProcessingException(RadiusProcessingExceptionNature nature, String message) {
        super(message);
        this.nature = nature;
    }

    /**
     * Constructor.
     *
     * @param nature - an enum value indicating whether an attempt should be made to re-send the Radius Response or if
     *            the error is more fundamental and further attempts to send the request will be met with failure.
     * @param message - the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     * @param cause - the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value
     *            is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RadiusProcessingException(RadiusProcessingExceptionNature nature, String message, Throwable cause) {
        super(message, cause);
        this.nature = nature;
    }

    /**
     * Get the nature of the exception.
     *
     * @return the nature.
     */
    public RadiusProcessingExceptionNature getNature() {
        return nature;
    }
}
