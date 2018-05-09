/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/applications/agents/common/isValidUrlWithPortAndPath
 */

import URI from "URI";

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
