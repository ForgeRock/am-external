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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

const { isArray } = require("lodash");

const loaders = (extensions, loaders, includeNodeModules = false) => {
    extensions = isArray(extensions) ? extensions : [extensions];

    const options = {};

    const loaderKey = isArray(loaders) ? "use" : "loader";
    options[loaderKey] = loaders;

        if (!includeNodeModules) {
            options.exclude = {
                and: [/node_modules/],
                not: [
                    /sanitize-html/,
                    /sanitize-html\/node_modules/,
                    /nanoid/
                ]
            };
    }

    return {
        // Optional test for a the URL query "?v=0.0.0", is to support Font Awesome font requests.
        test: new RegExp(`\\.(${extensions.join("|")})(\\?v=[0-9]\\.[0-9]\\.[0-9])?$`),
        ...options
    };
};

module.exports = loaders;
