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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

/**
 * An exception class associated with handling and processing metadata.
 */
public class MetadataException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     * @param error the error message
     */
    public MetadataException(String error) {
        super(error);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * @param error the error message
     * @param e the cause of the error
     */
    public MetadataException(String error, Throwable e) {
        super(error, e);
    }

    /**
     * Constructs a new exception with the specified cause.
     * @param cause the cause of the error
     */
    public MetadataException(Throwable cause) {
        super(cause);
    }
}
