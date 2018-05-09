/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { compact, map, zip } from "lodash";

import getCompactedPaths from "./getCompactedPaths";

/**
 * Takes an array of all possible paths, removes all repeating nodes and organizes them in columns.
 *
 * @param {Array[]} paths array of all possible paths
 * @returns {Array[]} array of columns
 */
const getColumns = (paths) => {
    const compactedPaths = getCompactedPaths(paths);

    return map(zip(...compactedPaths), (column) => compact(column));
};

export default getColumns;
