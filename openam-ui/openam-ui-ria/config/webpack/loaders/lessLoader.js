/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const LessPluginCleanCSS = require("less-plugin-clean-css");

const lessLoader = (optimize) => {
    const options = {};

    if (optimize) {
        options.compress = true;
        options.plugins = [new LessPluginCleanCSS({})];
    }

    return {
        loader: "less-loader",
        options
    };
};

module.exports = lessLoader;
