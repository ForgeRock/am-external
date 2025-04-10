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

const TerserWebpackPlugin = require("terser-webpack-plugin");
const webpackMerge = require("webpack-merge");

const babelLoader = require("../loaders/babelLoader");
const common = require("./common/webpack.config");
const cssLoader = require("../loaders/cssLoader");
const eslintLoader = require("../loaders/eslintLoader");
const lessLoader = require("../loaders/lessLoader");
const loaders = require("../loaders/loaders");

module.exports = webpackMerge(common, {
    devtool: "nosources-source-map",
    mode: "production",
    module: {
        rules: [
            loaders(["js", "jsx"], [babelLoader(), eslintLoader()]),
            loaders("css", [
                {
                    loader: "file-loader",
                    options: {
                        name: "[name].[hash:10].[ext]",
                        outputPath: "css/"
                    }
                }, {
                    loader: "extract-loader",
                    options: {
                        publicPath: "../"
                    }
                },
                cssLoader(true)
            ]),
            loaders("less", [
                {
                    loader: "file-loader",
                    options: {
                        name: "[name].[hash:10].css",
                        outputPath: "css/"
                    }
                }, {
                    loader: "extract-loader",
                    options: {
                        publicPath: "../"
                    }
                },
                cssLoader(),
                lessLoader(true)
            ])
        ]
    },
    optimization: {
        minimizer: [
            new TerserWebpackPlugin({
                sourceMap: true
            })
        ]
    },
    output: {
        chunkFilename: "[name].[chunkhash].js",
        filename: "[name].[chunkhash].js"
    },
    stats: "minimal"
});
