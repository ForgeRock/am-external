/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

const cssLoader = (optimize) => {
    const options = {};

    if (optimize) {
        options.minify = true;
    }

    return {
        loader: "css-loader",
        options
    };
};

module.exports = cssLoader;
