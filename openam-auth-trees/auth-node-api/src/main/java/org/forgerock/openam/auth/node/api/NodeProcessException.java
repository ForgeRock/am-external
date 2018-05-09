/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

/**
 * An Exception to indicate that there was a problem processing a {@link Node} that could not be resolved to a
 * {@link Action}.
 *
 * @supported.all.api
 */
public class NodeProcessException extends Exception {

    /**
     * Construct an exception with a message.
     * @param message The message.
     */
    public NodeProcessException(String message) {
        super(message);
    }

    /**
     * Construct an exception with a message and a cause.
     * @param cause The cause.
     * @param message The message.
     */
    public NodeProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct an exception with a cause.
     * @param cause The cause.
     */
    public NodeProcessException(Throwable cause) {
        super(cause);
    }
}
