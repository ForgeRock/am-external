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

const UglifyJSPlugin = require("uglifyjs-webpack-plugin");
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
                        name: "[name].[ext]",
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
                lessLoader(true)
            ])
        ]
    },
    optimization: {
        minimizer: [
            new UglifyJSPlugin({
                exclude: /config\/ThemeConfiguration/,
                sourceMap: true,
                uglifyOptions: {
                    compress: {
                        arrows: false,
                        booleans: false,
                        collapse_vars: false,
                        comparisons: false,
                        computed_props: false,
                        hoist_funs: false,
                        hoist_props: false,
                        hoist_vars: false,
                        if_return: false,
                        inline: false,
                        join_vars: false,
                        keep_infinity: true,
                        loops: false,
                        negate_iife: false,
                        properties: false,
                        reduce_funcs: false,
                        reduce_vars: false,
                        sequences: false,
                        side_effects: false,
                        switches: false,
                        top_retain: false,
                        toplevel: false,
                        typeofs: false,
                        unused: false,
    
                        /**
                         * Switch off all types of compression except those needed to convince
                         * react-devtools that we're using a production build
                         */
                        conditionals: true,
                        dead_code: true,
                        evaluate: true
                    }
                }
            })
        ]
    },
    output: {
        chunkFilename: "[id].[chunkhash].js",
        filename: ({ chunk }) => {
            return ["main-authorize", "main-device"].includes(chunk.name)
                ? "[name].js"
                : "[name].[chunkhash].js";
        }
    },
    stats: "minimal"
});
