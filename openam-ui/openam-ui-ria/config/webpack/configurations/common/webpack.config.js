/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const { ProvidePlugin, optimize: { CommonsChunkPlugin } } = require("webpack");
const { resolve } = require("path");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const DuplicatePackageCheckerWebpackPlugin = require("duplicate-package-checker-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

const alias = require("./parts/resolve/alias");
const entry = require("./parts/entry");
const extensions = require("./parts/resolve/extensions");
const loaders = require("../../loaders/loaders");
const modules = require("./parts/resolve/modules");

module.exports = {
    entry,
    module: {
        rules: [
            loaders("html", "handlebars-template-loader", true),
            loaders(["eot", "svg", "ttf", "woff", "woff2"], {
                loader: "file-loader",
                options: {
                    outputPath: "css/"
                }
            }, true),
            loaders("png", {
                loader: "file-loader",
                options: {
                    outputPath: "images/"
                }
            })
        ]
    },
    plugins: [
        new CommonsChunkPlugin({
            async: "vendor",
            minChunks ({ context }, count) {
                const isVendor = context && context.includes("node_modules") || context.includes("main/js/libs");
                return count >= 2 && isVendor;
            }
        }),
        new CopyWebpackPlugin([
            { from: "src/main/resources/*.ico", flatten: true },
            { from: "src/main/resources/*.json", flatten: true },
            { from: "src/main/resources/images", to: "images" },
            { from: "src/main/resources/locales", to: "locales" },
            { from: "src/main/resources/themes", to: "themes" }
        ]),
        new HtmlWebpackPlugin({
            chunks: ["main"],
            template: "src/main/resources/index.html"
        }),
        new HtmlWebpackPlugin({
            chunks: ["main-503"],
            filename: "503.html",
            template: "src/main/resources/503.html"
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