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
import {
    chain, findIndex, get, includes, keys, map, mapValues, max, omit, reduce, round, size, union, values
} from "lodash";
import { DropTarget } from "react-dnd";
import classNames from "classnames";
import PropTypes from "prop-types";
import React, { Component } from "react";

import Connection from "./Connection";
import { NEW_NODE_TYPE, PAGE_CHILD_NODE_TYPE }
    from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/nodeTypes/EditTreeDragItemTypes";
import NodeFactory from "./NodeFactory";
import { TREE_PADDING } from "./TreePadding";

const INPUT_Y_OFFSET = 22;
const SINGLE_OUTCOME_Y_OFFSET = 22;
const OUTCOME_Y_SPACING = 17;
const MULTI_OUTCOME_Y_OFFSET = -2;

const computeInputPosition = (positions) => ({
    x: positions.x,
    y: positions.y + INPUT_Y_OFFSET
});
const computeOutcomePosition = (dimensions, positions, outcomes, outcomeId) => {
    const width = dimensions.width || 0;
    const outcomeIndex = findIndex(outcomes, { id: outcomeId });
    const x = positions.x + width;
    const numberOutcomes = size(outcomes);
    const outcomeScale = numberOutcomes - outcomeIndex;
    const y = numberOutcomes === 1
        ? positions.y + SINGLE_OUTCOME_Y_OFFSET
        : positions.y + dimensions.height - (MULTI_OUTCOME_Y_OFFSET + OUTCOME_Y_SPACING * outcomeScale);
    return { x, y };
};
const getNewConnectionEnd = (event, node) => {
    const rect = node.getBoundingClientRect();
    return {
        x: event.clientX + node.scrollLeft - rect.left,
        y: event.clientY + node.scrollTop - rect.top
    };
};

class Tree extends Component {
    constructor (props) {
        super(props);
        this.state = { newConnectionFromNodeId: null };
    }

    componentWillUnmount () {
        const { ownerDocument } = this.domNode;
        ownerDocument.removeEventListener("mousemove", this.handleNewConnectionMove);
        ownerDocument.removeEventListener("mouseup", this.handleNewConnectionFinish);
    }

    handleConnectionStart = (nodeId, event, outcome) => {
        const node = this.domNode;
        const newConnectionEnd = getNewConnectionEnd(event, node);

        const { ownerDocument } = node;
        ownerDocument.addEventListener("mousemove", this.handleNewConnectionMove);
        ownerDocument.addEventListener("mouseup", this.handleNewConnectionFinish);

        this.setState({
            newConnectionFromNodeId: nodeId,
            newConnectionFromOutcome: outcome,
            newConnectionEndX: newConnectionEnd.x,
            newConnectionEndY: newConnectionEnd.y
        });
    };

    handleNewConnectionMove = (event) => {
        const newConnectionEnd = getNewConnectionEnd(event, this.domNode);
        this.setState({
            newConnectionEndX: newConnectionEnd.x,
            newConnectionEndY: newConnectionEnd.y
        });
    };

    handleNewConnectionFinish = () => {
        const { ownerDocument } = this.domNode;
        ownerDocument.removeEventListener("mousemove", this.handleNewConnectionMove);
        ownerDocument.removeEventListener("mouseup", this.handleNewConnectionFinish);
        this.setState({
            newConnectionFromNodeId: null
        });
    };

    handleConnectionFinish = (nodeId) => {
        if (this.state.newConnectionFromNodeId !== null) {
            this.props.onNewConnection(
                this.state.newConnectionFromNodeId,
                this.state.newConnectionFromOutcome,
                nodeId
            );
        }
    };

    handleSvgClick = () => {
        this.props.onNodeDeselect();
    };

    setRef = (element) => {
        this.domNode = element;
    };

    render () {
        const handleDrag = (id, x, y, mouseX, mouseY) => {
            this.setState({
                draggingNodeId: id,
                draggingNodeX: x,
                draggingNodeY: y,
                mouseX,
                mouseY
            });
        };
        const handleDragStop = (id, x, y, mouseX, mouseY) => {
            this.setState({ draggingNodeId: null });
            this.props.onNodeMove(id, round(x), round(y), round(mouseX), round(mouseY));
        };
        const handleNodeMeasure = (id, dimensions) =>
            this.props.onNodeDimensionsChange(id, dimensions.height, dimensions.width);

        const nodesWithConnectedInputs = reduce(map(this.props.nodes, "connections"),
            (result, connections) => union(result, values(connections)), []);
        const nodesWithoutPages = omit(this.props.nodes, keys(this.props.nodesInPages));
        const nodeComponents = map(nodesWithoutPages, (node, nodeId) => {
            const nodeProperties = this.props.localNodeProperties[this.state.draggingNodeId];
            const nodeType = get(nodeProperties, "_type._id");
            const createNode = NodeFactory[`create${node.nodeType}`] || NodeFactory.createNode;

            return createNode(nodeId, node, includes(nodesWithConnectedInputs, nodeId), {
                draggingNode: this.state.draggingNodeId && {
                    id: this.state.draggingNodeId,
                    mouseX: this.state.mouseX,
                    mouseY: this.state.mouseY,
                    type: nodeType,
                    x: this.state.draggingNodeX,
                    y: this.state.draggingNodeY
                },
                localNodeProperties: this.props.localNodeProperties,
                nodesInPages: this.props.nodesInPages,
                onConnectionFinish: this.handleConnectionFinish,
                onConnectionStart: this.handleConnectionStart,
                onDrag: handleDrag,
                onDragStop: handleDragStop,
                onMeasure: handleNodeMeasure,
                onNewNodeCreate: this.props.onNewNodeCreate,
                onNodePropertiesChange: this.props.onNodePropertiesChange,
                onNodeSelect: this.props.onNodeSelect,
                selectedNodeId: this.props.selectedNodeId
            });
        });

        const dimensions = mapValues(this.props.dimensions, (dimension) => {
            const draggedNode = dimension.id === this.state.draggingNodeId;
            return draggedNode ? { ...dimension, x: this.state.draggingNodeX, y: this.state.draggingNodeY } : dimension;
        });

        const getNodePositions = (id) => {
            return id === this.state.draggingNodeId
                ? {
                    x: this.state.draggingNodeX,
                    y: this.state.draggingNodeY
                }
                : {
                    x: this.props.nodes[id].x || 0,
                    y: this.props.nodes[id].y || 0
                };
        };

        const connectionComponents = chain(this.props.nodes)
            .map((node, fromNodeId) => {
                const fromNode = this.props.nodes[fromNodeId];
                return map(fromNode.connections, (toNodeId, outcomeId) => {
                    const fromNodeDimensions = dimensions[fromNodeId];
                    const fromNodePositions = getNodePositions(fromNodeId);
                    const toNodeDimensions = dimensions[toNodeId];
                    const toNodePositions = getNodePositions(toNodeId);

                    if (fromNodeDimensions && fromNodePositions && toNodeDimensions && toNodePositions) {
                        const start = {
                            ...fromNodeDimensions,
                            ...computeOutcomePosition(
                                fromNodeDimensions, fromNodePositions, fromNode._outcomes, outcomeId
                            ),
                            id: fromNodeId
                        };
                        const end = {
                            id: toNodeId,
                            ...computeInputPosition(toNodePositions)
                        };

                        return (
                            <Connection
                                end={ end }
                                isInputForSelectedNode={ this.props.selectedNodeId === toNodeId }
                                isOutputForSelectedNode={ this.props.selectedNodeId === fromNodeId }
                                key={ `connection-${start.id}-${end.id}` }
                                start={ start }
                            />
                        );
                    } else {
                        return null;
                    }
                });
            })
            .flatten()
            .value();

        if (this.state.newConnectionFromNodeId !== null) {
            const fromNodeDimensions = this.props.dimensions[this.state.newConnectionFromNodeId];
            const { x, y, _outcomes } = this.props.nodes[this.state.newConnectionFromNodeId];
            const start = {
                ...fromNodeDimensions,
                ...computeOutcomePosition(
                    fromNodeDimensions, { x, y }, _outcomes, this.state.newConnectionFromOutcome
                ),
                id: this.state.newConnectionFromNodeId
            };
            const end = {
                x: this.state.newConnectionEndX,
                y: this.state.newConnectionEndY
            };

            connectionComponents.push(
                <Connection
                    end={ end }
                    isNew
                    key={ `connection-${start.id}-${connectionComponents.length + 1}` }
                    start={ start }
                />
            );
        }

        return this.props.connectDropTarget(
            <div
                className={ classNames({
                    "authtree-editor-container": true,
                    "authtree-editor-container-dragging-connector": this.state.newConnectionFromNodeId !== null
                }) }
                style={ { height: this.props.containerHeight } }
            >
                <div className="authtree-editor" ref={ this.setRef } >
                    { nodeComponents }
                    <svg
                        className="authtree-editor-svg"
                        onClick={ this.handleSvgClick }
                        style={ {
                            height: this.props.canvasHeight,
                            width: this.props.canvasWidth
                        } }
                    >
                        { connectionComponents }
                    </svg>
                </div>
            </div>
        );
    }
}

Tree.propTypes = {
    canvasHeight: PropTypes.number.isRequired,
    canvasWidth: PropTypes.number.isRequired,
    connectDropTarget: PropTypes.func.isRequired,
    containerHeight: PropTypes.number.isRequired,
    dimensions: PropTypes.objectOf(PropTypes.shape({
        id: PropTypes.string,
        height: PropTypes.number,
        width: PropTypes.number
    })),
    localNodeProperties: PropTypes.objectOf(PropTypes.shape({
        _type: PropTypes.shape({
            _id: PropTypes.string
        }),
        nodes: PropTypes.arrayOf(PropTypes.string)
    })).isRequired,
    nodes: PropTypes.objectOf(PropTypes.shape({
        _outcomes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.string.isRequired,
            displayName: PropTypes.string.isRequired
        })).isRequired,
        connections: PropTypes.objectOf(PropTypes.string).isRequired,
        x: PropTypes.number,
        y: PropTypes.number
    })).isRequired,
    nodesInPages: PropTypes.objectOf(PropTypes.string).isRequired,
    onNewConnection: PropTypes.func.isRequired,
    onNewNodeCreate: PropTypes.func.isRequired,
    onNodeDeselect: PropTypes.func.isRequired,
    onNodeDimensionsChange: PropTypes.func.isRequired,
    onNodeMove: PropTypes.func.isRequired,
    onNodePropertiesChange: PropTypes.func.isRequired,
    onNodeSelect: PropTypes.func.isRequired,
    selectedNodeId: PropTypes.string
};

const dropTarget = {
    drop (props, monitor, component) {
        if (monitor.didDrop()) {
            // A child element handled this drop event
            return;
        }
        const authtreeEditorClientRect = component.domNode.getBoundingClientRect();
        const iconRadius = 15;
        const mousePositionRelativeToElement = {
            x: monitor.getInitialClientOffset().x - monitor.getInitialSourceClientOffset().x,
            y: monitor.getInitialClientOffset().y - monitor.getInitialSourceClientOffset().y
        };
        const position = {
            x: monitor.getClientOffset().x -
                authtreeEditorClientRect.left -
                mousePositionRelativeToElement.x +
                iconRadius,
            y: monitor.getClientOffset().y -
                authtreeEditorClientRect.top -
                mousePositionRelativeToElement.y
        };

        position.x = max([position.x, TREE_PADDING]);
        position.y = max([position.y, TREE_PADDING]);
        if (monitor.getItemType() === NEW_NODE_TYPE) {
            props.onNewNodeCreate(monitor.getItem(), position);
        } else {
            props.onNodeMove(monitor.getItem().nodeId, position.x, position.y,
                position.x + mousePositionRelativeToElement.x, position.y + mousePositionRelativeToElement.y);
        }
    }
};

function collect (connect) {
    return {
        connectDropTarget: connect.dropTarget()
    };
}

export default DropTarget([NEW_NODE_TYPE, PAGE_CHILD_NODE_TYPE], dropTarget, collect)(Tree);
