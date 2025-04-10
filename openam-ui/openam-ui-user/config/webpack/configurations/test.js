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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

const { DefinePlugin } = require("webpack");
const webpackNodeExternals = require("webpack-node-externals");

const alias = require("./common/parts/resolve/alias");
const babelLoader = require("../loaders/babelLoader");
const extensions = require("./common/parts/resolve/extensions");
const loaders = require("../loaders/loaders");
const modules = require("./common/parts/resolve/modules");

const testBabelLoader = babelLoader();
/**
 * TODO: Remove this babel-loader modification and @babel/plugin-transform-modules-commonjs when issue is resolved.
 * @see https://github.com/plasticine/inject-loader/issues/62
 */
testBabelLoader.options.plugins.push("@babel/plugin-transform-modules-commonjs");
testBabelLoader.options.plugins.push("babel-plugin-dynamic-import-node");

module.exports = {
    externals: [webpackNodeExternals()],
    mode: "development",
    module: {
        rules: [
            loaders(["js", "jsx"], [testBabelLoader]),
            loaders(["css", "html", "less", "png"], "null-loader", true)
        ]
    },
    performance: {
        hints: false
    },
    plugins: [
        new DefinePlugin({
            __DEV__: process.env.NODE_ENV !== "production"
        })
    ],
    resolve: {
        alias,
        extensions,
        modules
    },
    target: "node"
};
