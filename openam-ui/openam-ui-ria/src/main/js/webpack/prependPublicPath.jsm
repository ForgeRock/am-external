/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import URI from "urijs";

/**
 * Prepends a public path, that has been set at run-time, to the provided input.
 * @see https://webpack.js.org/guides/public-path/#on-the-fly
 * @param {string} input A path
 * @returns {string} If a <code>__webpack_public_path__</code> is set, returns the input
 * with <code>__webpack_public_path__</code> prepended, otherwise returns <code>input</code>.
 */
const prependPublicPath = (input) => {
    return __webpack_public_path__ === "" // eslint-disable-line camelcase
        ? input
        : URI.joinPaths(__webpack_public_path__, input).toString();
};

export default prependPublicPath;
