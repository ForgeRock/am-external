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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api.upgrade;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Evolving;
import org.forgerock.openam.auth.node.api.upgrade.exception.NodeUpgradeException;
import org.forgerock.openam.auth.node.api.upgrade.exception.UnsupportedUpgradeException;

/**
 * This class represents an unsupported upgrade operation for a node.
 */
@Evolving
public class UnsupportedUpgrade implements NodeVersionUpgrader {
    @Override
    public JsonValue upgrade(JsonValue config) throws NodeUpgradeException {
        throw new UnsupportedUpgradeException();
    }
}
