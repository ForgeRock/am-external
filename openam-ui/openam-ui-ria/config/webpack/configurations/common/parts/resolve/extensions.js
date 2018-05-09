/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

module.exports = [
    ".js", ".jsm", ".jsx", // Resolve JavaScript first so other types are not accidentally imported
    ".css",
    ".html",
    ".json",
    ".less",
    ".png"
];
