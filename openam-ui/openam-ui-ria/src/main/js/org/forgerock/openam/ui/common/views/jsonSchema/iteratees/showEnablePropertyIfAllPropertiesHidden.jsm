/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { extend, isEmpty } from "lodash";

/**
  * @param {Object} schemaValuePair Map of a schema property and its values.
  * @returns {Object} Schema enable property if all properties are hidden, otherwise all properties.
  * @module org/forgerock/openam/ui/common/views/jsonSchema/iteratees/showEnablePropertyIfAllPropertiesHidden
  */
export default function (schemaValuePair) {
    const allPropertiesHidden = isEmpty(schemaValuePair.schema.raw.defaultProperties);

    if (allPropertiesHidden && schemaValuePair.schema.hasEnableProperty()) {
        return extend(schemaValuePair, {
            schema: schemaValuePair.schema
                .getEnableProperty()
                .addDefaultProperties([schemaValuePair.schema.getEnableKey()])
        });
    }

    return schemaValuePair;
}
