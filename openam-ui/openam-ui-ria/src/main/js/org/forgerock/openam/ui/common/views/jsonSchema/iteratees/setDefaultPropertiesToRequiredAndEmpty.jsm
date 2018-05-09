/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { extend, intersection } from "lodash";

/**
  * @param {Object} schemaValuePair Map of a schema property and its values.
  * @returns {Object} Only the required and empty properties.
  * @module org/forgerock/openam/ui/common/views/jsonSchema/iteratees/setDefaultPropertiesToRequiredAndEmpty
  */
export default function (schemaValuePair) {
    const requiredSchemaKeys = schemaValuePair.schema.getRequiredPropertyKeys();
    const emptyValueKeys = schemaValuePair.values.getEmptyValueKeys();
    const requiredAndEmptyKeys = intersection(requiredSchemaKeys, emptyValueKeys);

    return extend(schemaValuePair, {
        schema: schemaValuePair.schema.addDefaultProperties(requiredAndEmptyKeys)
    });
}
