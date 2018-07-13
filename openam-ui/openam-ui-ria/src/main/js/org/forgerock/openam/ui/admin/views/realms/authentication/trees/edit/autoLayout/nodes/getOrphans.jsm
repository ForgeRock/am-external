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
