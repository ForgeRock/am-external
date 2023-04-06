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
 * Copyright 2019-2022 ForgeRock AS.
 */

import object from "./object";
import boolean from "./boolean";
import string from "./string";
import array from "./array";

const uiSchema = (schema, isEditValidationMode) => {
    // If schema has originalValue property then the value is a placeholder and
    // we should render a readonly text input field containing the placeholder key
    if (schema.originalValue) {
        return { "ui:field": "string" };
    } else {
        switch (schema.type) {
            case "object": return object(schema, isEditValidationMode);
            case "boolean": return boolean(schema);
            case "string": return string(schema, isEditValidationMode);
            case "array": return array(schema);
        }
    }
};

export default uiSchema;
