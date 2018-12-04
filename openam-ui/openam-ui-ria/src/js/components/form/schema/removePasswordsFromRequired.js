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

import { cloneDeep, keys, pick, without } from "lodash";

import isPassword from "./isPassword";

/**
 * Inspects a JSON Schema for properties that are passwords, and removes them from
 * the list of `required` properties.
 * @param {Object} schema JSON Schema.
 * @returns {Object} JSON Schema with passwords no longer marked as required.
 */
const removePasswordsFromRequired = (schema) => {
    const passwordProperties = pick(schema.properties, isPassword);
    const passwordKeys = keys(passwordProperties);

    const clonedSchema = cloneDeep(schema);
    clonedSchema.required = without(schema.required, ...passwordKeys);
    return clonedSchema;
};

export default removePasswordsFromRequired;
