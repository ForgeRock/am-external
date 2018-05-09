/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const test = require("../webpack/configurations/test");

module.exports = function (config) {
    config.set({
        basePath: "../..",
        browsers: ["PhantomJS"],
        files: [
            "node_modules/babel-polyfill/dist/polyfill.js",
            "src/test/js/test-main.js"
        ],
        frameworks: ["mocha"],
        preprocessors: {
            "src/test/js/test-main.js": ["webpack"]
        },
        webpack: test,
        webpackMiddleware: {
            stats: "errors-only"
        }
    });
};
