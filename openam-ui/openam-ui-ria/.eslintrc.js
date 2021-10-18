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
 * Copyright 2015-2018 ForgeRock AS.
 */

module.exports = {
    env: {
        amd: true,
        browser: true,
        es6: true,
        mocha: true,
        node: true
    },
    extends: [
        "@forgerock/eslint-config",
        "@forgerock/eslint-config/filenames",
        "@forgerock/eslint-config/jsx-a11y",
        "@forgerock/eslint-config/promise",
        "@forgerock/eslint-config/react"
    ],
    globals: {
        __webpack_public_path__: true
    },
    parser: "babel-eslint",
    parserOptions: {
        ecmaVersion: 2015,
        sourceType: "module",
        ecmaFeatures: {
            experimentalObjectRestSpread: true,
            jsx: true
        }
    },
    rules: {
        // This is to ignore the usages of deprecated React lifecycle methods
        "camelcase": ["error", { "allow": ["^UNSAFE_"] }],
        "promise/always-return": "off",
        "promise/catch-or-return": "off",
        "promise/no-callback-in-promise": "off",
        "promise/no-nesting": "off",
        "filenames/match-exported": ["error", null, "\\.test.generator$"]
    },
    settings: {
        react: {
            version: "16.9.0"
        }
    }
};
