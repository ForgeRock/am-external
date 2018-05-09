/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/constructFieldParams
 */

/**
 * Assembles the fields query parameter string.
 * @param {Array<String>} [fields] An array of fields to include
 * @returns {string} Returns the _fields query string or empty string.
 */
const constructFieldParams = (fields) => {
    if (fields) {
        return `&_fields=${fields.join(",")}`;
    } else {
        return "";
    }
};

export default constructFieldParams;
