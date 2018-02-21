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
 * Copyright 2015-2017 ForgeRock AS.
 */
module.exports = function (config) {
    config.set({
        basePath: ".",
        frameworks: ["mocha", "requirejs"],
        files: [
            { pattern: "target/test-classes/test-main.js" },
            { pattern: "target/test-classes/**/*.js", included: false },
            { pattern: "target/compiled/**/*.js", included: false },
            { pattern: "target/dependencies/libs/**/*.js", included: false },
            { pattern: "target/XUI/libs/**/*.js", included: false },
            { pattern: "node_modules/chai/chai.js", included: false },
            { pattern: "node_modules/sinon/pkg/*.js", included: false },
            { pattern: "node_modules/sinon-chai/lib/sinon-chai.js", included: false },
            { pattern: "node_modules/squirejs/src/Squire.js", included: false }
        ],
        exclude: [],
        preprocessors: {
            "target/test-classes/**/*.js": ["babel"]
        },
        babelPreprocessor: {
            options: {
                ignore: ["libs/"],
                presets: ["es2015"]
            }
        },
        reporters: ["notify", "mocha"],
        mochaReporter: {
            output: "autowatch",
            showDiff: true
        },
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: true,
        browsers: ["PhantomJS"],
        singleRun: false
    });
};
