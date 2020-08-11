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
 * Copyright 2017-2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/applications/agents/common/isValidUrlWithPortAndPath
 */

import URI from "urijs";

/**
 * Validates that the given URL contains a protocol, hostname, port and path.
 * @param {string} urlToValidate The URL to validate
 * @returns {boolean} Returns true if the URL is a valid
 */
const isValidUrlWithPortAndPath = (urlToValidate) => {
    if (!urlToValidate) { return true; }
    try {
        const url = URI.parse(urlToValidate);
        // Expected format is protocol://host:port/deploymentUri
        return !!(url.protocol && url.hostname && url.port && url.path !== "/");
    } catch (error) {
        return false;
    }
};

export default isValidUrlWithPortAndPath;
