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

/*
  Switch off the eslint rule for not creating multiple react components in a file for this file, as the functions
  exported are mistaken for components.
*/

/* eslint react/no-multi-comp: "off" */

import React from "react";

import Node from "./Node";
import PageNode from "./PageNode";

function nodeProperties (nodeId, node, isInputConnected, treeProperties) {
    return {
        id: nodeId,
        isInputConnected,
        isSelected: nodeId === treeProperties.selectedNodeId,
        key: nodeId,
        node,
        onConnectionFinish: treeProperties.onConnectionFinish,
        onConnectionStart: treeProperties.onConnectionStart,
        onDrag: treeProperties.onDrag,
        onDragStop: treeProperties.onDragStop,
        onMeasure: treeProperties.onMeasure,
        onSelect: treeProperties.onNodeSelect
    };
}

/*
  We export an object which can then be queried to see if it has a create<Node Type> function. If so, that function
  should be called, and if none exists then the generic createNode should be called. All functions defined here
  should take the same argument list, as the invoking code should not need to know which factory function it is calling.
 */
export default {
    createNode:
        /**
         * Create a standard node.
         * @param {string} nodeId The ID of the node.
         * @param {object} [node] The node.
         * @param {boolean} isInputConnected Whether the node's input is connected.
         * @param {object} [treeProperties] The tree properties.
         * @param {Function} [treeProperties.onConnectionFinish] The tree editor connection-finish event handler.
         * @param {Function} [treeProperties.onConnectionStart] The tree editor connection-start event handler.
         * @param {Function} [treeProperties.onDrag] The tree editor drag event handler.
         * @param {Function} [treeProperties.onDragStop] The tree editor drag-stop event handler.
         * @param {Function} [treeProperties.onMeasure] The tree editor measure event handler.
         * @param {Function} [treeProperties.onNodeSelect] The tree editor node-select event handler.
         * @param {string} [treeProperties.selectedNodeId] The selected node ID.
         * @returns {object} A react component for the node.
         */
        function createNode (nodeId, node, isInputConnected, treeProperties) {
            return <Node { ...nodeProperties(nodeId, node, isInputConnected, treeProperties) } />;
        },

    createPageNode:
        /**
         * Create a page node.
         * @param {string} nodeId The ID of the node.
         * @param {object} [node] The node.
         * @param {boolean} isInputConnected Whether the node's input is connected.
         * @param {object} [treeProperties] The tree properties
         * @param {object} [treeProperties.draggingNode] The node currently being dragged - can be undefined.
         * @param {object} [treeProperties.localNodeProperties] The properties of all the nodes in the tree.
         * @param {object} [treeProperties.nodesInPages] The nodes in the tree that are contained in pages.
         * @param {Function} [treeProperties.onConnectionFinish] The tree editor connection-finish event handler.
         * @param {Function} [treeProperties.onConnectionStart] The tree editor connection-start event handler.
         * @param {Function} [treeProperties.onDrag] The tree editor drag event handler.
         * @param {Function} [treeProperties.onDragStop] The tree editor drag-stop event handler.
         * @param {Function} [treeProperties.onMeasure] The tree editor measure event handler.
         * @param {Function} [treeProperties.onNewNodeCreate] The tree editor new-node-create event handler.
         * @param {Function} [treeProperties.onNodePropertiesChange] The tree editor node-properties-change event
         * handler.
         * @param {Function} [treeProperties.onNodeSelect] The tree editor node-select event handler.
         * @param {string} [treeProperties.selectedNodeId] The selected node ID.
         * @returns {object} A react component for the node.
         */
        function createPageNode (nodeId, node, isInputConnected, treeProperties) {
            const properties = {
                ...nodeProperties(nodeId, node, isInputConnected, treeProperties),
                draggingNode: treeProperties.draggingNode,
                localNodeProperties: treeProperties.localNodeProperties,
                nodesInPages: treeProperties.nodesInPages,
                onNewNodeCreate: treeProperties.onNewNodeCreate,
                onNodePropertiesChange: treeProperties.onNodePropertiesChange,
                pageConfig: treeProperties.localNodeProperties[nodeId] || { nodes: [] },
                selectedNodeId: treeProperties.selectedNodeId
            };
            return <PageNode { ...properties } />;
        }
};
