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

const { ProvidePlugin } = require("webpack");
const { resolve } = require("path");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const DuplicatePackageCheckerWebpackPlugin = require("duplicate-package-checker-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

const alias = require("./parts/resolve/alias");
const extensions = require("./parts/resolve/extensions");
const loaders = require("../../loaders/loaders");
const modules = require("./parts/resolve/modules");

module.exports = {
    entry: "./src/js/main.js",
    module: {
        rules: [
            loaders("html", "handlebars-template-loader", true),
            loaders(["eot", "svg", "ttf", "woff", "woff2"], {
                loader: "file-loader",
                options: {
                    name: "[name].[hash:10].[ext]",
                    outputPath: "css/"
                }
            }, true),
            loaders("png", {
                loader: "file-loader",
                options: {
                    name: "[name].[hash:10].[ext]",
                    outputPath: "images/"
                }
            })
        ]
    },
    plugins: [
        new CopyWebpackPlugin([
            { from: "src/resources/*.ico", flatten: true },
            { from: "src/resources/*.json", flatten: true },
            { from: "src/resources/images", to: "images" }
        ]),
        new HtmlWebpackPlugin({
            chunks: ["main"],
            chunksSortMode: "none",
            template: "src/resources/index.html"
        }),
        new ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery"
        }),
        new DuplicatePackageCheckerWebpackPlugin()
    ],
    resolve: {
        alias,
        extensions,
        modules
    },
    output: {
        hashDigestLength: 10,
        path: resolve(process.cwd(), "build")
    }
};
