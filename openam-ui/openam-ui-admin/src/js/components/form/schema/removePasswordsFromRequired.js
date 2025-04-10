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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { cloneDeep, filter, mapValues } from "lodash";

import isPassword from "./isPassword";

/**
 * Inspects a JSON Schema for properties that are passwords, and removes them from
 * the list of `required` properties.
 * @param {object} schema JSON Schema draft 4+ with passwords defined as _isPassword.
 * @returns {object} JSON Schema with passwords no longer marked as required.
 */
const removePasswordsFromRequired = (schema) => {
    const clonedSchema = cloneDeep(schema);

    const removePasswords = (schema) => {
        if (schema.required) {
            schema = {
                ...schema,
                required: filter(schema.required, (key) => !isPassword(schema.properties[key]))
            };
        }

        if (schema.properties) {
            schema = {
                ...schema,
                properties: mapValues(schema.properties, (property) => removePasswords(property))
            };
        }

        return schema;
    };
    return removePasswords(clonedSchema);
};

export default removePasswordsFromRequired;
