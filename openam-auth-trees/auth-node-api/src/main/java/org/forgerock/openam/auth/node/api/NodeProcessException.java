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
 * Copyright 2017-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import org.forgerock.openam.annotations.SupportedAll;

/**
 * An Exception to indicate that there was a problem processing a {@link Node} that could not be resolved to a
 * {@link Action}.
 *
 */
@SupportedAll
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
