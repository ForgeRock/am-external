/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { isEmpty } from "lodash";

/**
  * @param {Object} schemaValuePair Map of a schema property and its values.
  * @returns {boolean} True if the default properties are empty, false if they are not.
  * @module org/forgerock/openam/ui/common/views/jsonSchema/iteratees/emptyProperties
  */
export default function (schemaValuePair) {
    return isEmpty(schemaValuePair.schema.raw.defaultProperties);
}
