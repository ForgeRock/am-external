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
 * Copyright 2017 ForgeRock AS.
 */

import { findKey, map, max, reduce } from "lodash";
/**
 * The margins are used to space the nodes apart from one another and are
 * included within the widths and heights of the columns.
 **/
export const VERTICAL_NODE_MARGIN = 25;
export const HORIZONTAL_NODE_MARGIN = 50;

/**
 * Given columns and heights of all nodes, this function returns an array of all column heights.
 *
 * @param {Array[]} columns array of columns
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {number[]} array of all column heights
 */
export const getColumnHeights = (columns, dimensions) =>
    map(columns, (column) => reduce(column, (result, node) =>
        result + dimensions[findKey(node)].height + VERTICAL_NODE_MARGIN, VERTICAL_NODE_MARGIN));

/**
 * Given columns and widths of all nodes, this function returns an array of all column widths.
 *
 * @param {Array[]} columns array of columns
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {number[]} array of all column widths
 */
const getColumnMaxWidths = (columns, dimensions) =>
    map(columns, (column) => reduce(column, (result, node) => {
        return dimensions[findKey(node)].width + HORIZONTAL_NODE_MARGIN > result
            ? dimensions[findKey(node)].width + HORIZONTAL_NODE_MARGIN : result;
    }, HORIZONTAL_NODE_MARGIN));

/**
 * Given columns, column index and heights of all nodes, this function returns a top margin for the column
 * (Y position of the first node in the column).
 *
 * @param {Array[]} columns array of columns
 * @param {number} columnIndex column index
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {number[]} array of all column heights
 */
const getColumnYPosition = (columns, columnIndex, dimensions) => {
    const columnHeights = getColumnHeights(columns, dimensions);
    return (max(columnHeights) - columnHeights[columnIndex]) / 2;
};

/**
 * Given columns, column index, row index and widths of all nodes,
 * this function returns a X position for the given node.
 *
 * @param {Array[]} columns array of columns
 * @param {number} columnIndex column index
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {number} X position
 */
export const getNodeXPosition = (columns, columnIndex, dimensions) => {
    const columnsToLeft = columns.slice(0, columnIndex);
    const columnWidths = getColumnMaxWidths(columnsToLeft, dimensions);
    const totalWidthOfColumnsToLeft = reduce(columnWidths, (result, width) => result + width, HORIZONTAL_NODE_MARGIN);
    return totalWidthOfColumnsToLeft;
};

/**
 * Given columns, column index, row index and heights of all nodes,
 * this function returns a Y position for the given node
 *
 * @param {Array[]} columns array of columns
 * @param {number} columnIndex column index
 * @param {number} rowIndex row index
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {number} Y position
 */
export const getNodeYPosition = (columns, columnIndex, rowIndex, dimensions) => {
    const allNodesInTheColumn = columns[columnIndex];
    const allNodesAbove = allNodesInTheColumn.slice(0, rowIndex);
    const heightOfTheNodesAbove = reduce(allNodesAbove, (result, node) =>
        result + dimensions[findKey(node)].height + VERTICAL_NODE_MARGIN, VERTICAL_NODE_MARGIN);
    const columnY = getColumnYPosition(columns, columnIndex, dimensions);
    return columnY + heightOfTheNodesAbove;
};
