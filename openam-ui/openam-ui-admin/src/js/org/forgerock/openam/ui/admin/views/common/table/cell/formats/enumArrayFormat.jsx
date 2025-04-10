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

import { get, map } from "lodash";
import React from "react";

/**
 * Given a jsonschema property and the array of cell data, this module will return a renderable React element
 * containing an unordered list of the selected enum items.
 * @param {Object} cell The cell data
 * @param {Object} schema The jsonschema property
 * @returns {ReactElement} Renderable React element
 */
const enumArrayFormat = (cell, schema) => {
    const listItems = map(cell, (value) => {
        const index = schema.items.enum.indexOf(value);
        const enumName = get(schema.items, `enumNames[${index}]`, value);
        return (
            <li>
                <span>{ enumName }</span>
                <span aria-hidden="true" className="am-multi-select-enum-format-icon">×</span>
            </li>
        );
    });
    return (
        <ul className="am-multi-select-enum-format list-unstyled">
            { listItems }
        </ul>
    );
};

export default enumArrayFormat;
