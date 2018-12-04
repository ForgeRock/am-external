/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */

import { cloneDeep, isArray, mapValues, omit } from "lodash";

/**
 * Inspects a pre Draft 04 JSON Schema for `required` attributes on properties, and converts
 * them to an `Array` of `required` properties (removing the originals). If there is already
 * the `required` property of the `Array` type (post Draft 04 JSON Schema) the schema is returned untouched.
 * @module components/form/schema/convertToDraft4PlusRequired
 * @param {Object} schema JSON Schema.
 * @returns {Object} JSON Schema with `required` attributes convert to Draft 04 JSON Schema+.
 * @see https://datatracker.ietf.org/doc/draft-handrews-json-schema-validation
 */
const convertToDraft4PlusRequired = (schema) => {
    const isDraft4Plus = isArray(schema.required);

    if (isDraft4Plus) {
        return schema;
    }

    const requiredKeys = [];
    const clonedSchema = cloneDeep(schema);

    clonedSchema.properties = mapValues(schema.properties, (property, key) => {
        if (property.required) {
            requiredKeys.push(key);
        }
        return omit(property, "required");
    });
    clonedSchema.required = requiredKeys;

    return clonedSchema;
};

export default convertToDraft4PlusRequired;
