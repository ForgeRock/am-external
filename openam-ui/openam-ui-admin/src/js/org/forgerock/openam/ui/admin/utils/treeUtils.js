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
 * Copyright 2026 Ping Identity Corporation.
 */
import { t } from "i18next";
import {
    FAILURE_NODE_TYPE,
    SUCCESS_NODE_TYPE
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";

export const TREE_PADDING = 20;
export const NEW_NODE_TYPE = "NodeTypeItem#NEW_NODE_TYPE";
export const PAGE_CHILD_NODE_TYPE = "NodeTypeItem#PAGE_CHILD_NODE_TYPE";

export const SUCCESS_NODE = {
    name: t("console.authentication.trees.edit.nodes.success.title"),
    _id: SUCCESS_NODE_TYPE,
    tags: ["basic authentication"]
};

export const FAILURE_NODE = {
    name: t("console.authentication.trees.edit.nodes.failure.title"),
    _id: FAILURE_NODE_TYPE,
    tags: ["basic authentication"]
};

export const NODE_GROUPS = {
    basicAuthentication : {
        tag: "basic authentication",
        icon: "user"
    },
    mfa: {
        tag: "mfa",
        icon: "desktop"
    },
    risk: {
        tag: "risk",
        icon: "balance-scale"
    },
    behavioral: {
        tag: "behavioral",
        icon: "puzzle-piece"
    },
    contextual: {
        tag: "contextual",
        icon: "bullseye"
    },
    federation: {
        tag: "federation",
        icon: "building"
    },
    identityManagement: {
        tag: "identity management",
        icon: "address-card"
    },
    utilities: {
        tag: "utilities",
        icon: "plug"
    },
    iot: {
        tag: "iot",
        icon: "microchip"
    },
    uncategorized: {
        tag: "uncategorized",
        icon: "square"
    }
};

/**
 * Given the nodeTypes object and a filter, this function returns an array of
 * nodeTypes where the name or tags contain the filter
 * @param {Array} nodeTypes list of node types
 * @param {string} nodeTypes.name The name property of the nodeType
 * @param {Array<string>}  nodeTypes.tags An array of tags or an empty array
 * @param {string} filterString The string used to filter the nodeTypes
 * @returns {Array<object>} The filtered nodeTypes with matching name or tags
 */
export const filterNodesByTag = (nodeTypes = [], filterString = "") => {
    const lowercaseFilter = filterString.toLowerCase();
    return nodeTypes.filter(({ name, tags }) => {
        const nameMatches = name.toLowerCase().includes(lowercaseFilter);
        return nameMatches || tags && tags.join().toLowerCase().includes(lowercaseFilter);
    });
};

/**
 * Checks if a node should be validated as a standalone node by checking the pageNodeCapability metadata entry of the
 * node. Does not apply to Page Node version 1.0.
 * @param {object} childNodeType - The type of the node to check the pageNodeCapabilities of.
 * @param {string} pageNodeVersion - The version of the page node.
 * @returns {boolean} - True if the node should be validated as standalone, false otherwise.
 */
export function shouldValidateAsStandaloneNode (childNodeType, pageNodeVersion) {
    if (!pageNodeVersion || pageNodeVersion === "1.0") {
        return false;
    }

    if (!childNodeType) {
        return false;
    }

    const metadata = childNodeType.metadata;
    return metadata && metadata.pageNodeCompatibility === "standalone";
}
