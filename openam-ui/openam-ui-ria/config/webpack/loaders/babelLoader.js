/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const { resolve } = require("path");

const babelLoader = (extension) => ({
    loader: "babel-loader",
    options: {
        "extends": resolve(process.cwd(), "config", "babel", `.${extension}.babelrc`)
    }
});

module.exports = babelLoader;
