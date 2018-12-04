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

const { resolve } = require("path");

const alias = require("./common/parts/resolve/alias");
const babelLoader = require("../loaders/babelLoader");
const extensions = require("./common/parts/resolve/extensions");
const loaders = require("../loaders/loaders");
const modules = require("./common/parts/resolve/modules");

module.exports = {
    mode: "development",
    module: {
        rules: [
            loaders(["js", "jsx"], [babelLoader()]),
            loaders(["css", "html", "less", "png"], "null-loader", true)
        ]
    },
    resolve: {
        alias,
        extensions,
        modules: [
            ...modules,
            resolve(process.cwd(), "src", "test", "js")
        ]
    },
    performance: {
        hints: false
    }
};
