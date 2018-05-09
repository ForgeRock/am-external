/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([], () => {
    /**
    * Transforms boolean types to checkbox format
    * @param {Object} property Property to transform
    */
    return function transformBooleanTypeToCheckboxFormat (property) {
        if (property.hasOwnProperty("type") && property.type === "boolean") {
            property.format = "checkbox";
        }
    };
});
