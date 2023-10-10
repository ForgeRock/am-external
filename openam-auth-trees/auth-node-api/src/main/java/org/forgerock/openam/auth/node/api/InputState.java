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
 * Copyright 2019-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

/** Describes one shared state datum consumed by a node. */
public class InputState {
    /** The attribute name of this state. */
    public final String name;

    /** Whether this state is required. */
    public final boolean required;

    /**
     * Construct a required InputState.
     *
     * @param name the attribute name of this state
     */
    public InputState(String name) {
        this(name, true);
    }

    /**
     * Construct a potentially required InputState.
     *
     * @param name the attribute name of this state
     * @param required whether this state is required
     */
    public InputState(String name, boolean required) {
        this.name = name;
        this.required = required;
    }

    @Override
    public String toString() {
        return "InputState{"
                + "name='" + name + '\''
                + ", required=" + required
                + '}';
    }
}
