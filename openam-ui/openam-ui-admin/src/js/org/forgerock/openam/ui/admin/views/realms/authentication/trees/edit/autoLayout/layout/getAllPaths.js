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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { clone, find, findKey, forEach, isEmpty, keys } from "lodash";

/**
 * This function recursivly iterates through the node's outcomes sorted by their ids and returns all
 * possible paths for it sorted by their length. If there are loops that are connected to the starting node, they will
 * be returned as well.
 * @param {object} nodeId node id to inspect
 * @param {object} nodes Object of all nodes, indexed by node ID
 * @param {object[]} [path=[]] path to add the node to
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
