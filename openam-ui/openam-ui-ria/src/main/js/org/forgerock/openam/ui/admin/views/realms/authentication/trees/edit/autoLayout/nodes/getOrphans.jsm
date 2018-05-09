/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { difference, findKey, keys, map, pick, union } from "lodash";

/**
 * Calculates orphaned nodes (nodes that are not directly connected to a tree).
 *
 * @param {Object} nodes collection of all nodes
 * @param {Array[]} columns array of columns
 * @returns {Object} object with node IDs as keys and the node as values
 */
const getOrphans = (nodes, columns) => {
    const nodeIDs = keys(nodes);
    const columnIDs = map(union(...columns), (node) => findKey(node));
    const orphanIDs = difference(nodeIDs, columnIDs);

    return pick(nodes, ...orphanIDs);
};

export default getOrphans;
