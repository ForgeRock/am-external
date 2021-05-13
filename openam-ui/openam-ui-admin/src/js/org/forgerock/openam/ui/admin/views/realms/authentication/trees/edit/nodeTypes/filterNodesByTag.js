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
 * Copyright 2019 ForgeRock AS.
 */

import { filter } from "lodash";

/**
 * @module org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/nodeTypes/filterNodesByTag
 */

/**
 * Given the nodeTypes object and a filter, this function returns an array of nodeTypes where the name or tags contain
 * the filter
 * @param {object} nodeTypes The realm path to the administered realm
 * @param {string} nodeTypes.name The name property of the nodeType
 * @param {Array<string>}  nodeTypes.tags An array of tags or an empty array
 * @param {string} filterString The string used to filter the nodeTypes
 * @returns {Array<object>} The filtered nodeTypes with matching name or tags
 */
const filterNodesByTag = (nodeTypes, filterString) => {
    const lowercaseFilter = filterString.toLowerCase();
    return filter(nodeTypes, ({ name, tags }) => {
        const nameMatches = name.toLowerCase().includes(lowercaseFilter);
        return nameMatches || tags && tags.join().toLowerCase().includes(lowercaseFilter);
    });
};

export default filterNodesByTag;
