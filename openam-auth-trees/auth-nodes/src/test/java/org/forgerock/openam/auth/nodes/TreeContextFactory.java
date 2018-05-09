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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Helper class to assist with creation of {@link TreeContext} objects for use from unit tests.
 */
final class TreeContextFactory {

    private TreeContextFactory() {
    }

    /**
     * Creates an initial {@link TreeContext} with no shared state.
     *
     * @return The {@link TreeContext}.
     */
    static TreeContext emptyTreeContext() {
        JsonValue emptySharedState = json(object());
        return newTreeContext(emptySharedState);
    }

    /**
     * Creates a {@link TreeContext} with the provided shared state.
     *
     * @param sharedState The shared state to add to the {@link TreeContext}.
     * @return The {@link TreeContext}.
     */
    static TreeContext newTreeContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new ExternalRequestContext.Builder().build(), emptyList());
    }

    /**
     * Creates a {@link TreeContext} with the provided shared state and preferred locales.
     *
     * @param sharedState The shared state to add to the {@link TreeContext}.
     * @param preferredLocales The preferred locales.
     * @return The {@link TreeContext}.
     */
    static TreeContext newTreeContext(JsonValue sharedState, PreferredLocales preferredLocales) {
        return new TreeContext(sharedState, new ExternalRequestContext.Builder().locales(preferredLocales).build(),
                emptyList());
    }

}
