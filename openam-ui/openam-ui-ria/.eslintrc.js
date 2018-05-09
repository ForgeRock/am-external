/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
module.exports = {
    env: {
        amd: true,
        browser: true,
        node: true
    },
    extends: [
        "forgerock",
        "forgerock/filenames",
        "forgerock/jsx-a11y",
        "forgerock/promise",
        "forgerock/react"
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
        "promise/always-return": "off",
        "promise/catch-or-return": "off",
        "promise/no-callback-in-promise": "off",
        "promise/no-nesting": "off"
    },
    settings: {
        react: {
            version: "16.0.0"
        }
    }
};
