/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { extend } from "lodash";

/**
  * @param {Object} schemaValuePair Map of a schema property and its values.
  * @returns {Object} The required properties.
  * @module org/forgerock/openam/ui/common/views/jsonSchema/iteratees/setDefaultPropertiesToRequired
  */
export default function (schemaValuePair) {
    const requiredSchemaKeys = schemaValuePair.schema.getRequiredPropertyKeys();

    return extend(schemaValuePair, {
        schema: schemaValuePair.schema.addDefaultProperties(requiredSchemaKeys)
    });
}
