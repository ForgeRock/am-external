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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import javax.security.auth.callback.ConfirmationCallback;

/**
 * Augments the standard ConfirmationCallback with a name field. This allows specific ConfirmationCallbacks to be
 * identified and handled appropriately.
 */
public class NamedConfirmationCallback extends ConfirmationCallback {
    private final String name;

    /**
     * {@inheritDoc}.
     * @param name the name of this ConfirmationCallback, used to identify it for special handling.
     */
    public NamedConfirmationCallback(String name, int messageType, String[] options, int defaultOption) {
        super(messageType, options, defaultOption);
        this.name = name;
    }

    /**
     * {@inheritDoc}.
     * @param name the name of this ConfirmationCallback, used to identify it for special handling.
     */
    public NamedConfirmationCallback(String name, int messageType, int optionType, int defaultOption) {
        super(messageType, optionType, defaultOption);
        this.name = name;
    }

    /**
     * {@inheritDoc}.
     * @param name the name of this ConfirmationCallback, used to identify it for special handling.
     */
    public NamedConfirmationCallback(String name, String prompt, int messageType, String[] options, int defaultOption) {
        super(prompt, messageType, options, defaultOption);
        this.name = name;
    }

    /**
     * {@inheritDoc}.
     * @param name the name of this ConfirmationCallback, used to identify it for special handling.
     */
    public NamedConfirmationCallback(String name, String prompt, int messageType, int optionType, int defaultOption) {
        super(prompt, messageType, optionType, defaultOption);
        this.name = name;
    }

    /**
     * Get the name of this ConfirmationCallback.
     * @return the name of this ConfirmationCallback.
     */
    public String getName() {
        return name;
    }
}
