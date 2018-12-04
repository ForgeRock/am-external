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
 * Copyright 2018 ForgeRock AS.
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
