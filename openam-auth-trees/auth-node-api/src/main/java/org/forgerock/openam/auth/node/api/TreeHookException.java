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

package org.forgerock.openam.auth.node.api;

import org.forgerock.openam.annotations.Supported;

/**
 * An exception to indicate there was a problem executing a tree hook.
 */
@Supported
public final class TreeHookException extends Exception {
    /**
     * Default constructor that constructs a blank TreeHookException.
     */
    @Supported
    public TreeHookException() {
        super();
    }

    /**
     * Constructs the TreeHookException taking the information from a given
     * throwable object.
     *
     * @param e Throwable we want to wrap into a TreeHookException.
     */
    @Supported
    public TreeHookException(Throwable e) {
        super(e);
    }

    /**
     * Constructs the TreeHookException with a message.
     *
     * @param message The message.
     */
    @Supported
    public TreeHookException(String message) {
        super(message);
    }

    /**
     * Constructs the TreeHookException with a message and a wrapped throwable.
     *
     * @param message The message
     * @param cause   Throwable we want to wrap into a TreeHookException.
     */
    @Supported
    public TreeHookException(String message, Throwable cause) {
        super(message, cause);
    }
}
