/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/common/util/object/flattenValues
  */
define([
    "lodash"
], (_) => {
    var exports = function (object) {
        return _.mapValues(object, (value) => {
            if (_.isArray(value) && value.length === 1) {
                return value[0];
            }

            return value;
        });
    };

    return exports;
});
