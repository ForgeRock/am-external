/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const { resolve } = require("path");

const alias = require("./common/parts/resolve/alias");
const babelLoader = require("../loaders/babelLoader");
const extensions = require("./common/parts/resolve/extensions");
const loaders = require("../loaders/loaders");
const modules = require("./common/parts/resolve/modules");

module.exports = {
    module: {
        rules: [
            loaders("js", [babelLoader("js")]),
            loaders("jsm", [babelLoader("jsm")]),
            loaders("jsx", [babelLoader("jsx")]),
            loaders(["css", "html", "less", "png"], "null-loader")
        ]
    },
    resolve: {
        alias,
        extensions,
        modules: [
            ...modules,
            resolve(process.cwd(), "src", "test", "js")
        ]
    }
};
