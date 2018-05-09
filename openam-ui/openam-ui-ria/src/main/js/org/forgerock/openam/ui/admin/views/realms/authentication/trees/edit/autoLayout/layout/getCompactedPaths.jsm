/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { chain, filter, findIndex, isEmpty, map } from "lodash";

/**
 * Takes an array of all possible paths and removes repeating nodes, so they are not rendered several times. Returns
 * <code>null</code> in place of removed nodes.
 *
 * @param {Array[]} paths array of all possible paths
 * @returns {Array[]} array of compacted paths
 */
const getCompactedPaths = (paths) => {
    const connectedNodes = [];

    return chain(paths)
        .map((row) => {
            const compactedPath = map(row, (node) => {
                if (findIndex(connectedNodes, node) === -1) {
                    connectedNodes.push(node);
                    return node;
                } else {
                    return null;
                }
            });

            return filter(compactedPath, (node) => !isEmpty(node)).length > 0
                ? compactedPath
                : null;
        })
        .compact()
        .value();
};

export default getCompactedPaths;
