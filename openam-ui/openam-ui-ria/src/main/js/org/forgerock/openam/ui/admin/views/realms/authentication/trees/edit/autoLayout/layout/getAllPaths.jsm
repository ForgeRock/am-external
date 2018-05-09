/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { clone, find, findKey, forEach, isEmpty, keys } from "lodash";

/**
 * This function recursivly iterates through the node's outcomes sorted by their ids and returns all
 * possible paths for it sorted by their length. If there are loops that are connected to the starting node, they will
 * be returned as well.
 *
 * @param {Object} nodeId node id to inspect
 * @param {Object} nodes Object of all nodes, indexed by node ID
 * @param {Object[]} [path=[]] path to add the node to
 * @returns {Array[]} array of all possible paths
 */
const getAllPaths = (nodeId, nodes, path = []) => {
    const allPathsForNode = [];

    if (find(path, (node) => findKey(node) === nodeId)) {
        // if there is a loop detected
        allPathsForNode.push(path);
    } else {
        path.push({ [nodeId]: nodes[nodeId] });

        const nodeConnections = nodes[nodeId].connections;
        if (isEmpty(nodeConnections)) {
            allPathsForNode.push(path);
        } else {
            forEach(keys(nodeConnections).sort(), (outcomeId) => {
                forEach(getAllPaths(nodeConnections[outcomeId], nodes, clone(path)), (path) => {
                    allPathsForNode.push(path);
                });
            });
        }
    }

    return allPathsForNode.sort((path1, path2) => path2.length - path1.length);
};

export default getAllPaths;
