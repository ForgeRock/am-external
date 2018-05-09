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

const { URL } = require("url");
const webpackMerge = require("webpack-merge");

const babelLoader = require("../loaders/babelLoader");
const common = require("./common/webpack.config");
const cssLoader = require("../loaders/cssLoader");
const eslintLoader = require("../loaders/eslintLoader");
const lessLoader = require("../loaders/lessLoader");
const loaders = require("../loaders/loaders");
const logColourInfo = require("../support/log/colour/info");

const PROXY_TARGET_PORT = process.env.OPENAM_PORT || 8080;

console.log(`Proxying to AM on port ${logColourInfo(PROXY_TARGET_PORT)}`);
module.exports = webpackMerge(common, {
    devServer: {
        compress: true,
        disableHostCheck: true,
        host: "0.0.0.0",
        overlay: true,
        proxy: {
            "!/openam/XUI": {
                changeOrigin: true,
                router: (request) => {
                    const requestUrl = new URL(`${request.protocol}://${request.get("host")}`);
                    requestUrl.port = PROXY_TARGET_PORT;
                    return requestUrl;
                },
                target: `http://localhost:${PROXY_TARGET_PORT}`
            }
        },
        publicPath: "/openam/XUI/"
    },
    devtool: "cheap-module-eval-source-map",
    module: {
        rules: [
            loaders("js", [babelLoader("js"), eslintLoader({ emitWarning: true })]),
            loaders("jsm", [babelLoader("jsm"), eslintLoader({ emitWarning: true })]),
            loaders("jsx", [babelLoader("jsx"), eslintLoader({ emitWarning: true })]),
            loaders("css", [
                {
                    loader: "file-loader",
                    options: {
                        name: "[name].[ext]",
                        outputPath: "css/"
                    }
                }, {
                    loader: "extract-loader",
                    options: {
                        publicPath: "../"
                    }
                },
                cssLoader()
            ]),
            loaders("less", [
                {
                    loader: "file-loader",
                    options: {
                        name: "[name].css",
                        outputPath: "css/"
                    }
                }, {
                    loader: "extract-loader",
                    options: {
                        publicPath: "../"
                    }
                },
                cssLoader(),
                lessLoader()
            ])
        ]
    },
    output: {
        chunkFilename: "[name].js",
        filename: "[name].js"
    }
});
