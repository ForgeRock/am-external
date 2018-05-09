/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/common/util/Promise
  */
define([
    "jquery",
    "lodash"
], ($, _) => ({
    /**
     * Returns a promise that resolves when all of the promises in the iterable argument have resolved, or rejects
     * with the reason of the first passed promise that rejects.
     * @param {Array} promises An array of promises
     * @returns {Promise} A promise that represents all of the specified promises
     * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all
     */
    all (promises) {
        if (_.isArray(promises)) {
            if (promises.length) {
                return $.when(...promises).then(function () {
                    const args = Array.prototype.slice.call(arguments);

                    if (args.length === 1 || promises.length !== 1) {
                        return args;
                    }

                    return [args];
                });
            } else {
                return $.Deferred().resolve([]).promise();
            }
        } else {
            return $.Deferred().reject(new TypeError("Expected an array of promises")).promise();
        }
    }
}));
