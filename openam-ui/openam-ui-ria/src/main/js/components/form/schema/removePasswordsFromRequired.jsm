/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
