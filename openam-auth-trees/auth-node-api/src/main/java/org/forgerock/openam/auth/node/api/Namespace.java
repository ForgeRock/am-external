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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

/**
 * Enum representing the namespace of a node, used by {@link Node.Metadata}. The namespace is used to differentiate
 * two nodes with the same name.
 */
public enum Namespace {

    /**
     * Denotes a service introduced into the core AM product after originally
     * being present in a Marketplace plugin.
     */
    PRODUCT("product"),
    /**
     * The default Namespace for a node.
     */
    NONE("");

    private final String value;

    Namespace(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}