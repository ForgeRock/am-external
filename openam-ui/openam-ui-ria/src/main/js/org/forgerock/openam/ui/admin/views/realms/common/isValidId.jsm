/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/common/isValidId
 */

import { contains, endsWith, isEmpty, startsWith } from "lodash";

/**
  * Validates that the given Id satisfies the following conditions:
  * Does not start with the '#' or '"' characters
  * Does not start or end with the space character
  * Does not contain the '\', '/', '+', ';' or ',' characters
  * Is not be '.' or '..'
  * Or is an empty string
  * @param {string} id The Id to validate
  * @returns {boolean} Returns true if the Id does not start with a '#', `"`, or contain
  * the '\', '/', '+', ';' or ',' characters, or start or end with the space character, is not '.' or '..', or is empty.
  */
const isValidId = (id) => {
    if (isEmpty(id)) {
        return true;
    }
    if (id === "." || id === ".." ||
        startsWith(id, " ") ||
        endsWith(id, " ") ||
        startsWith(id, "#") ||
        startsWith(id, "\"") ||
        contains(id, "\\") ||
        contains(id, "/") ||
        contains(id, "+") ||
        contains(id, ";") ||
        contains(id, ",") ||
        contains(id, "%") ||
        contains(id, "[") ||
        contains(id, "]") ||
        contains(id, "|") ||
        contains(id, "?")) {
        return false;
    }

    return true;
};

export default isValidId;
