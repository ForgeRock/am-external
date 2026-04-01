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

/**
 * List of node properties related to node versioning. Used to omit these properties
 * from node save data
 */
export const NODE_VERSIONING_PROPERTIES = [
    "version",
    "versions",
    "isUpgradable",
    "nodeType",
    "nodeVersion"
];

/**
 * Compares two versions to test whether version a is greater than version b
 * @param {string} a version A
 * @param {string} b version b
 * @returns {boolean} Whether a is a greater version than b
 */
export const isHigherVersion = (a, b) => a.localeCompare(b, undefined, { numeric: true }) > 0;

/**
 * Tests whether a node is upgradable by checking if its current version is
 * less than the highest available version.
 * @param {string} currentVersion the nodes current version
 * @param {Array} availableVersions the list of available node versions
 * @returns {boolean} whether the current version is the highest version available
 */
export const isNodeUpgradable = (currentVersion, availableVersions) => {
    return availableVersions.length
        ? availableVersions.some((version) =>
            version.localeCompare(currentVersion, undefined, { numeric: true }) > 0) : false;
};

/**
 * Extracts node versions for a given node type from the full list of nodes and
 * works out if the node is upgradable.
 * @param {Array} nodeTypes the full list of nodes
 * @param {object} node the given node to get the versions of
 * @returns {object} with the node types versions array and isUpgrdable property
 */
export const getVersionDataForNodeType = (nodeTypes, node) => {
    const versions = nodeTypes.find((nodeType) => nodeType._id === node.nodeType)?.versions || [];
    let isUpgradable;
    // Nodes outside of page nodes have the property version, nodes within a
    // page node have the property nodeVersion
    if (node.version) {
        isUpgradable = isNodeUpgradable(node.version, versions);
    } else {
        isUpgradable = isNodeUpgradable(node.nodeVersion, versions);
    }
    return { versions, isUpgradable };
};

/**
 * Sorts node versions from lowest to highest
 * @param {Array} versions the list of node versions to sort
 * @returns {Array} sorted list of node versions
 */
export const sortVersions = (versions) => [...versions].sort((a, b) => a.localeCompare(b, undefined, { numeric: true })); // eslint-disable-line
