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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.forgerock.openam.annotations.Supported;


/**
 * Describes one shared state attribute produced by a node.
 */
@Supported
public final class OutputState {
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
    @Supported
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
    @Supported
    public OutputState(String name, Map<String, Boolean> outcomes) {
        this.name = name;
        this.outcomes = new HashMap<>();
        this.outcomes.putAll(outcomes);
    }

    /**
     * Get the attribute (property) name of this state.
     *
     * @return The attribute (property) name of this state
     */
    @Supported
    public String name() {
        return name;
    }

    /**
     * Gets the map of node outcome names and whether the output property is guaranteed to be provided for that outcome.
     * <p> A wildcard * may be used to represent all outcomes.
     *
     * @return The map of node outcomes
     */
    @Supported
    public Map<String, Boolean> outcomes() {
        return outcomes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OutputState that = (OutputState) o;
        return Objects.equals(name, that.name) && Objects.equals(outcomes, that.outcomes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, outcomes);
    }
}
