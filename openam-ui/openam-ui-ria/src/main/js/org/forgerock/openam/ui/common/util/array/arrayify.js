/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/util/array/arrayify
 */
define(() => {
    /**
     * Wraps any value in an array.
     * <p/>
     * If the value is an array itself, a new array with the same elements is returned.
     * @param   {*} value Value to wrap in an array
     * @returns {Array}   Array containing the value
     */
    const exports = function (value) {
        return [].concat(value);
    };

    return exports;
});
