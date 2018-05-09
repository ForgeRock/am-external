/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { chain, findKey, map } from "lodash";

import { getNodeXPosition, getNodeYPosition } from "./stage";

/**
 * Given the array of columns and node dimensions, this function returns positions (x, y) for all connected tree nodes.
 *
 * @param {Array[]} columns array of columns
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {Object} object with node ids as keys and their positions as values
 */
const getTreeNodePositions = (columns, dimensions) => {
    return chain(columns)
        .map((column, columnIndex) => {
            return map(column, (node, rowIndex) => ({
                id: findKey(node),
                x: getNodeXPosition(columns, columnIndex, dimensions),
                y: getNodeYPosition(columns, columnIndex, rowIndex, dimensions)
            }));
        })
        .flatten()
        .value();
};

export default getTreeNodePositions;
