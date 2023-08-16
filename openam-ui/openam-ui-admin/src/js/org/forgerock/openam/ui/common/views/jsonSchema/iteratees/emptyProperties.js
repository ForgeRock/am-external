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
 * Copyright 2016-2019 ForgeRock AS.
 */

import { isEmpty } from "lodash";

/**
  * @param {object} schemaValuePair Map of a schema property and its values.
  * @returns {boolean} True if the default properties are empty, false if they are not.
  * @module org/forgerock/openam/ui/common/views/jsonSchema/iteratees/emptyProperties
  */
export default function (schemaValuePair) {
    return isEmpty(schemaValuePair.schema.raw.defaultProperties);
}