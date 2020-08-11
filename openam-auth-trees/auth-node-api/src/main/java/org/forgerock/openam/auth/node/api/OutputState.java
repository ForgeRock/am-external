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

import java.util.HashMap;
import java.util.Map;

/** Describes one shared state datum produced by a node. */
public class OutputState {
    /** The attribute name of this state. */
    public final String name;

    /** A map of outcomes for which this state is provided and whether it is always provided. */
    public final Map<String, Boolean> outcomes;

    /**
     * Construct an OutputState with a name and default outcomes.  The default wildcard for outcome indicates that the
     * output will be provided for all outcomes.
     *
     * @param name the name of the output state
     */
    public OutputState(String name) {
        this.name = name;
        outcomes = new HashMap<>();
        outcomes.put("*", true);
    }

    /**
     * Construct an OutputState with a name and map of outcomes.
     *
     * @param name the name of the output state
     * @param outcomes the outcomes for this state
     */
    public OutputState(String name, Map<String, Boolean> outcomes) {
        this.name = name;
        this.outcomes = new HashMap<>();
        this.outcomes.putAll(outcomes);
    }
}
