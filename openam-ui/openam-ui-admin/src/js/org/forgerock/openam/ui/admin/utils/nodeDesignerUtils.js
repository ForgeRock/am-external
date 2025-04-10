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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { t } from "i18next";

// The names of the categories use translations which is the same way the
// categories appear in the tree editor
const NODE_CATEGORIES = [
    t("console.authentication.nodes.edit.selectCategory"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.basicAuthentication"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.mfa"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.risk"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.behavioral"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.contextual"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.federation"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.identityManagement"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.utilities"),
    t("console.authentication.trees.edit.nodes.nodeTypes.groups.iot")
];

// These values match the tag that is being used to sort nodes into categories
// defined in views/realms/authentication/trees/edit/nodeTypes/groupsData.json
const NODE_CATEGORIES_VALUES = [
    "",
    "basic authentication",
    "mfa",
    "risk",
    "behavioral",
    "contextual",
    "federation",
    "identity management",
    "utilities",
    "iot"
];

export const categorySchema = {
    "title": t("console.authentication.nodes.edit.category"),
    "description": t("console.authentication.nodes.edit.categoryDesc"),
    "propertyOrder": 670, // appear after error outcome toggle and before tags field
    "required": false,
    "options": {
        "enum_titles": NODE_CATEGORIES
    },
    "enumNames": NODE_CATEGORIES,
    "enum": NODE_CATEGORIES_VALUES,
    "type": "string",
    "exampleValue": ""
};

/**
 * In the UI custom nodes have a separate field for category and search tags
 * but the backend makes no distinction between the two and both get saved under
 * the tags property. On load we want to split these fields up again to display
 * them separately back to the user
 * @param {Array} tags custom nodes tags array
 * @returns {object} containing a category if one is found and the remaining tags
 */
export function splitCategoryAndTags (tags) {
    const split = {
        category: "",
        tags: []
    };
    for (let i = 0; i < tags.length; i += 1) {
        const match = NODE_CATEGORIES_VALUES.find((category) => category === tags[i]);
        if (match) {
            split.category = match;
        } else {
            split.tags.push(tags[i]);
        }
    }
    return split;
}
