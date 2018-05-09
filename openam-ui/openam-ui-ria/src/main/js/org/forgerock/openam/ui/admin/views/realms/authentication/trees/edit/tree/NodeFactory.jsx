/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/*
  Switch off the eslint rule for not creating multiple react components in a file for this file, as the functions
  exported are mistaken for components.
*/

/* eslint react/no-multi-comp: "off" */

import React from "react";

import Node from "./Node";
import PageNode from "./PageNode";

function nodeProperties (nodeId, node, position, isInputConnected, treeProperties) {
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
        onSelect: treeProperties.onNodeSelect,
        ...position
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
         *
         * @param {string} nodeId The ID of the node.
         * @param {Object} [node] The node.
         * @param {Object} [position] The node's position.
         * @param {boolean} isInputConnected Whether the node's input is connected.
         * @param {Object} [treeProperties] The tree properties.
         * @param {function} [treeProperties.onConnectionFinish] The tree editor connection-finish event handler.
         * @param {function} [treeProperties.onConnectionStart] The tree editor connection-start event handler.
         * @param {function} [treeProperties.onDrag] The tree editor drag event handler.
         * @param {function} [treeProperties.onDragStop] The tree editor drag-stop event handler.
         * @param {function} [treeProperties.onMeasure] The tree editor measure event handler.
         * @param {function} [treeProperties.onNodeSelect] The tree editor node-select event handler.
         * @param {string} [treeProperties.selectedNodeId] The selected node ID.
         * @returns {Object} A react component for the node.
         */
        function createNode (nodeId, node, position, isInputConnected, treeProperties) {
            return <Node { ...nodeProperties(nodeId, node, position, isInputConnected, treeProperties) } />;
        },

    createPageNode:
        /**
         * Create a page node.
         *
         * @param {string} nodeId The ID of the node.
         * @param {Object} [node] The node.
         * @param {Object} [position] The node's position.
         * @param {boolean} isInputConnected Whether the node's input is connected.
         * @param {Object} [treeProperties] The tree properties
         * @param {object} [treeProperties.draggingNode] The node currently being dragged - can be undefined.
         * @param {object} [treeProperties.localNodeProperties] The properties of all the nodes in the tree.
         * @param {object} [treeProperties.nodesInPages] The nodes in the tree that are contained in pages.
         * @param {function} [treeProperties.onConnectionFinish] The tree editor connection-finish event handler.
         * @param {function} [treeProperties.onConnectionStart] The tree editor connection-start event handler.
         * @param {function} [treeProperties.onDrag] The tree editor drag event handler.
         * @param {function} [treeProperties.onDragStop] The tree editor drag-stop event handler.
         * @param {function} [treeProperties.onMeasure] The tree editor measure event handler.
         * @param {function} [treeProperties.onNewNodeCreate] The tree editor new-node-create event handler.
         * @param {function} [treeProperties.onNodePropertiesChange] The tree editor node-properties-change event
         * handler.
         * @param {function} [treeProperties.onNodeSelect] The tree editor node-select event handler.
         * @param {string} [treeProperties.selectedNodeId] The selected node ID.
         * @returns {Object} A react component for the node.
         */
        function createPageNode (nodeId, node, position, isInputConnected, treeProperties) {
            const properties = {
                ...nodeProperties(nodeId, node, position, isInputConnected, treeProperties),
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
