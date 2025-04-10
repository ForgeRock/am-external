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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import java.io.IOException;

import org.forgerock.http.util.Json;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tree hook that sets details on a JSON response.
 */
abstract class DetailsTreeHook implements TreeHook {

    private static final Logger logger = LoggerFactory.getLogger(DetailsTreeHook.class);

    /**
     * Converts a string value to an object if it is a string representation of some valid JSON, otherwise returns
     * the input string.
     * @param value The string value to convert
     * @return The object, or the input string if conversion fails.
     */
    protected Object convertStringToJson(String value) {
        try {
            return Json.readJson(value);
        } catch (IOException e) {
            logger.debug("Failed to parse JSON value: {}", value, e);
            return value;
        }
    }
}
