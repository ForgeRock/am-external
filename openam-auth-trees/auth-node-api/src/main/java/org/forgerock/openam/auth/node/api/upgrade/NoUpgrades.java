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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api.upgrade;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Evolving;

/**
 * Void implementation of the {@link NodeVersionUpgrader} interface.
 * <p>
 * This implementation does not perform any upgrade and returns the same configuration, if used
 * it will also always mark that there are no further upgrades for the node.
 */
@Evolving
public final class NoUpgrades implements NodeVersionUpgrader {
    @Override
    public JsonValue upgrade(JsonValue config) {
        return config;
    }
}
