/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * Ensures that For ES6 modules, we use the default export.
 * @param {Object} module Module to unwrap if a ES6 module is found
 * @returns {Object} Module, possibly unwrapped
 * @module org/forgerock/openam/ui/common/util/es6/normaliseModule
 */
export default function (module) {
    if (module.__esModule) {
        module = module.default;
    }
    return module;
}
