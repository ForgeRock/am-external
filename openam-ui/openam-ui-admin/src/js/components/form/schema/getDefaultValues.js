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
 * Copyright 2020 ForgeRock AS.
 */

import { forEach } from "lodash";
import isObjectType from "./isObjectType";

/**
 * Recursively extracts the default values from the provided schema.
 * @param {object} schema Schema
 * @returns {object} the default values extracted from the JSON schema.
 */
const getDefaultValues = (schema) => {
    const defaults = {};

    if (isObjectType(schema)) {
        forEach(schema.properties, (property, key) => {
            if (isObjectType(property)) {
                defaults[key] = getDefaultValues(property);
            } else if (property.default) {
                defaults[key] = property.default;
            }
        });
    }

    return defaults;
};

export default getDefaultValues;
