/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/applications/agents/common/appendToUrl
 */

import URI from "URI";

/**
 * Appends a path and optional query to a url, normalising any double or missing forward slashes
 * @param {String} url The url which is to be modified
 * @param {String} appendage The additional path (and optional query) which is to be added to the url
 * @returns {String} Returns the normalised url
 */
const appendToUrl = (url, appendage) => {
    const newURI = new URI(url);
    const appendageURI = new URI(appendage);
    const newPath = URI.joinPaths(newURI.path(), appendageURI.path());

    newURI.path(newPath);
    newURI.query(appendageURI.query());

    return newURI.toString();
};

export default appendToUrl;
