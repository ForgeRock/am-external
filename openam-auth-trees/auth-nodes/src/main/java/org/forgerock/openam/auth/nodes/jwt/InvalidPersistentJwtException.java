/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
}
