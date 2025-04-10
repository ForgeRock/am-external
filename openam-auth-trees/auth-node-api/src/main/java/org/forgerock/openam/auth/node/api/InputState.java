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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import org.forgerock.openam.annotations.Supported;

/**
 * Describes a single shared state attribute consumed by a node.
 */
@Supported
public final class InputState {
    /** The attribute name of this state. */
    public final String name;

    /** Whether this state is required. */
    public final boolean required;

    /**
     * Constructs a required InputState. The `required` attribute is set to true.
     *
     * @param name the attribute name of this state
     */
    @Supported
    public InputState(String name) {
        this(name, true);
    }

    /**
     * Construct an InputState.
     *
     * @param name the attribute name of this state
     * @param required whether this state is required for the node to function.
     */
    @Supported
    public InputState(String name, boolean required) {
        this.name = name;
        this.required = required;
    }

    /**
     * Gets the attribute name of this input state.
     *
     * @return the attribute name of this state
     */
    @Supported
    public String name() {
        return name;
    }

    /**
     * Returns a boolean indicating whether this state is required by the consuming node in order to function.
     * If false the node must be able to handle the case where this input is missing.
     * @return whether this state is required for the node to function.
     */
    @Supported
    public boolean required() {
        return required;
    }

    @Override
    public String toString() {
        return "InputState{"
                + "name='" + name + '\''
                + ", required=" + required
                + '}';
    }
}
