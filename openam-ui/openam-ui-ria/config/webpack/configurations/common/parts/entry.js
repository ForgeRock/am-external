/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

module.exports = {
    /**
     * "main" must be placed first to ensure the Commons Chunk Plugin creates the async
     * vendor chunk from the correct entry.
     */
    "main": "./src/main/js/main.jsm",
    "main-503": "./src/main/js/main-503.js",
    "main-authorize": "./src/main/js/main-authorize.js",
    "main-device": "./src/main/js/main-device.js"
};
