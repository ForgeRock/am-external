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
 * Copyright 2017 ForgeRock AS.
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
