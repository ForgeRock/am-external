/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { get, set } from "lodash";

const transformEnumTypeToString = (property) => {
    if (property.hasOwnProperty("enum")) {
        const path = get(property, "properties.inherited") ? "properties.value.type" : "type";
        set(property, path, "string");
    }
};

export default transformEnumTypeToString;
