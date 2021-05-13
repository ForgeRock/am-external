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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.openam.annotations.EvolvingAll;

/**
 * Suspended text output callback extends {@link TextOutputCallback} to allow a custom message to be displayed to the
 * user whilst informing the client that the current auth flow has been suspended. Note, this callback only makes sense
 * in the context of being used in line with suspending the current tree, by means of {@link
 * Action#suspend(SuspensionHandler)}.
 *
 * @since 7.0.0
 */
@EvolvingAll
public final class SuspendedTextOutputCallback extends TextOutputCallback {

    /**
     * Construct a new instance with a message type and message to be displayed.
     *
     * @param messageType the message type ({@code INFORMATION}, {@code WARNING} or {@code ERROR})
     * @param message the message to be displayed
     * @throws IllegalArgumentException if {@code messageType} is not either {@code INFORMATION}, {@code WARNING} or
     * {@code ERROR}, if {@code message} is null, or if {@code message} has a length of 0
     */
    public SuspendedTextOutputCallback(int messageType, String message) {
        super(messageType, message);
    }

    /**
     * Creates a new {@link SuspendedTextOutputCallback} instance along with an information based message.
     *
     * @param message the information based message
     * @return new {@link SuspendedTextOutputCallback} instance
     */
    public static SuspendedTextOutputCallback info(String message) {
        return new SuspendedTextOutputCallback(TextOutputCallback.INFORMATION, message);
    }

    /**
     * Creates a new {@link SuspendedTextOutputCallback} instance along with an warning based message.
     *
     * @param message the warning based message
     * @return new {@link SuspendedTextOutputCallback} instance
     */
    public static SuspendedTextOutputCallback warn(String message) {
        return new SuspendedTextOutputCallback(TextOutputCallback.WARNING, message);
    }

    /**
     * Creates a new {@link SuspendedTextOutputCallback} instance along with an error based message.
     *
     * @param message the error based message
     * @return new {@link SuspendedTextOutputCallback} instance
     */
    public static SuspendedTextOutputCallback error(String message) {
        return new SuspendedTextOutputCallback(TextOutputCallback.ERROR, message);
    }

}
