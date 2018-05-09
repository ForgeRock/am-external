/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { chain, max } from "lodash";

import { getColumnHeights, HORIZONTAL_NODE_MARGIN, VERTICAL_NODE_MARGIN } from "./stage";

/**
 * Given the array of orphan nodes and columns, and all the node dimensions, this function returns
 * positions (x, y) for all orphans.
 *
 * @param {Object[]} orphans array of all orphans
 * @param {Array[]} columns array of columns
 * @param {Object} dimensions heights and widths of the nodes, indexed by node id
 * @returns {Object} object with node ids as keys and their positions as values
 */
const getOrphanPositions = (orphans, columns, dimensions) => {
    const treeHeight = max(getColumnHeights(columns, dimensions));
    let nextXPosition = HORIZONTAL_NODE_MARGIN;
    const calculateNextXPosition = (width) => {
        const xPosition = nextXPosition;
        nextXPosition = nextXPosition + HORIZONTAL_NODE_MARGIN + width;
        return xPosition;
    };

    return chain(orphans)
        .map((orphan, orphanId) => ({
            id: orphanId,
            x: calculateNextXPosition(dimensions[orphanId].width),
            y: treeHeight + VERTICAL_NODE_MARGIN
        }))
        .sortBy("id")
        .value();
};

export default getOrphanPositions;
