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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

require("dotenv-safe").config();

const fs = require("fs");
const { merge } = require("lodash");
const webpackLog = require("webpack-log");
const webpackMerge = require("webpack-merge");

const babelLoader = require("../loaders/babelLoader");
const common = require("./common/webpack.config");
const cssLoader = require("../loaders/cssLoader");
const eslintLoader = require("../loaders/eslintLoader");
const lessLoader = require("../loaders/lessLoader");
const loaders = require("../loaders/loaders");
const logColourInfo = require("../../support/log/colour/info");

const log = webpackLog({ name: "wds" });

const proxyUrl = `${process.env.AM_HOSTNAME}:${process.env.AM_PORT}${process.env.AM_PATH}`;
log.info(`Proxying to AM at ${logColourInfo(proxyUrl)}`);
const httpsConfig = {};
if (process.env.AM_SSL_CERT_TRUSTED === "false" && process.env.AM_HOSTNAME.startsWith("https")) {
    merge(httpsConfig, {
        devServer: {
            https: {
                key: fs.readFileSync(process.env.AM_SSL_KEY),
                cert: fs.readFileSync(process.env.AM_SSL_CERT),
                ca: fs.readFileSync(process.env.AM_SSL_CA_CERT)
            }
        }
    });
}
module.exports = webpackMerge(common, merge({
    devServer: {
        compress: false,
        disableHostCheck: true,
        host: "0.0.0.0",
        port: 8081,
        overlay: true,
        proxy: {
            [`!${process.env.AM_PATH}/ui-admin`]: {
                changeOrigin: true,
                secure: false,
                target: `${process.env.AM_HOSTNAME}:${process.env.AM_PORT}`
            }
        },
        publicPath: `${process.env.AM_PATH}/ui-admin/`
    },
    devtool: "cheap-module-eval-source-map",
    mode: "development",
    module: {
        rules: [
            loaders(["js", "jsx"], [babelLoader(), eslintLoader({ emitWarning: true })]),
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
    optimization: {
        removeAvailableModules: false
    }
}, httpsConfig));
