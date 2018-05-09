/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const { BundleAnalyzerPlugin } = require("webpack-bundle-analyzer");
const webpackMerge = require("webpack-merge");

const production = require("./production");

module.exports = webpackMerge(production, {
    plugins: [
        new BundleAnalyzerPlugin({
            analyzerMode: "static"
        })
    ]
});
