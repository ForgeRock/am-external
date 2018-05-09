/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
