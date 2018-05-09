/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const { DefinePlugin } = require("webpack");
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
    module: {
        rules: [
            loaders("js", [babelLoader("js"), eslintLoader()]),
            loaders("jsm", [babelLoader("jsm"), eslintLoader()]),
            loaders("jsx", [babelLoader("jsx"), eslintLoader()]),
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
    output: {
        chunkFilename: "[name].[chunkhash].js",
        filename: ({ chunk }) => {
            return ["main-authorize", "main-device"].includes(chunk.name)
                ? "[name].js"
                : "[name].[chunkhash].js";
        }
    },
    plugins: [
        new DefinePlugin({
            "process.env.NODE_ENV": JSON.stringify("production")
        }),
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
});
