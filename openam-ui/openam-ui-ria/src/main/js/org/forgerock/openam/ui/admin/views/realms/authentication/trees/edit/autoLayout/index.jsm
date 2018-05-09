/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { indexBy } from "lodash";

import getAllPaths from "./layout/getAllPaths";
import getColumns from "./layout/getColumns";
import getOrphanPositions from "./positions/getOrphanPositions";
import getOrphans from "./nodes/getOrphans";
import getTreeNodePositions from "./positions/getTreeNodePositions";

/**
 * Given the entry node id, a collection of the nodes, and the dimensions of all the nodes, this function returns
 * the positions (x, y) for all the nodes - both connected nodes and orphans.
 * @example
 * autoLayout(entryNodeId, nodes, dimensions) =>
 * {
 *     "one": { "id": "one", "x": 25, "y": 125 },
 *     "two": { "id": "two", "x": 205, "y": 37.5 },
 *     "three": { "id": "three", "x": 205, "y": 237.5 },
 *     ...
 * }
 * @param {String} entryNodeId Entry node ID
 * @param {Object} nodes Object of all nodes, indexed by node ID
 * @param {Object} dimensions heights and widths of the nodes, indexed by node ID
 * @returns {Object} Object, indexed by node ID, with a value object of dimensions
 */
const autoLayout = (entryNodeId, nodes, dimensions) => {
    const paths = getAllPaths(entryNodeId, nodes);
    const columns = getColumns(paths);
    const orphans = getOrphans(nodes, columns);

    const treeNodeMeasurements = getTreeNodePositions(columns, dimensions);
    const orphanMeasurements = getOrphanPositions(orphans, columns, dimensions);

    return indexBy(treeNodeMeasurements.concat(orphanMeasurements), "id");
};

export default autoLayout;
