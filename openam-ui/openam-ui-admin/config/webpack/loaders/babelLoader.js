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
 * Copyright 2018-2019 ForgeRock AS.
 */

const babelLoader = () => ({
    loader: "babel-loader",
    options: {
        babelrc: false,
        cacheDirectory: true,
        ignore: [
            "./**/libs"
        ],
        presets: [
            "@babel/preset-react",
            ["@babel/preset-env", {
                corejs: 3,
                targets: {
                    android: 6,
                    chrome: 62,
                    edge: 25,
                    firefox: 57,
                    ie: 11,
                    ios: 9,
                    safari: 11
                },
                useBuiltIns: "usage"
            }]
        ],
        plugins: [
            "@babel/plugin-proposal-class-properties",
            "@babel/plugin-proposal-object-rest-spread",
            "@babel/plugin-syntax-dynamic-import"
        ]
    }
});

module.exports = babelLoader;
