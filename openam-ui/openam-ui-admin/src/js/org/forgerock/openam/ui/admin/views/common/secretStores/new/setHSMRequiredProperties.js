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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

import { cloneDeep } from "lodash";

/**
 * @module org/forgerock/openam/ui/admin/views/common/secretStores/new/setHSMRequiredProperties
 */

/**
 * The API requires either the 'providerGuiceKey' and/or the 'file' property. Neither are required in the
 * API, but at least one of them must always be set. As it is purely a UI decision to hide non-required fields
 * in the creation pages, this function is sets both properties as reqiured so they are displayed in the UI.
 * @param {string} schema The json schema for a property
 * @param {string} type The type of store
 * @returns {object} schema A modified schema with HsmSecretStore's providerGuiceKey and file properties set as reqiured
 */
const setHSMRequiredProperties = (schema, type) => {
    const modifiedSchema = cloneDeep(schema);
    if (type === "HsmSecretStore") {
        modifiedSchema.properties.providerGuiceKey.required = true;
        modifiedSchema.properties.file.required = true;
    }
    return modifiedSchema;
};

export default setHSMRequiredProperties;
