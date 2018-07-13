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
