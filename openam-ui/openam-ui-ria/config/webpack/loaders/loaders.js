/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const { isArray } = require("lodash");

const loaders = (extensions, loaders, includeNodeModules = false) => {
    extensions = isArray(extensions) ? extensions : [extensions];

    const options = {};

    const loaderKey = isArray(loaders) ? "use" : "loader";
    options[loaderKey] = loaders;

    if (!includeNodeModules) {
        options.exclude = /node_modules/;
    }

    return {
        // Optional test for a the URL query "?v=0.0.0", is to support Font Awesome font requests.
        test: new RegExp(`\\.(${extensions.join("|")})(\\?v=[0-9]\\.[0-9]\\.[0-9])?$`),
        ...options
    };
};

module.exports = loaders;
