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
 * Copyright 2017-2018 ForgeRock AS.
 */
import {
    chain, contains, findIndex, get, keys, map, mapValues, max, omit, pluck, reduce, round, size, union, values
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

const computeInputPosition = (measurements) => ({
    x: measurements.x,
    y: measurements.y + INPUT_Y_OFFSET
});
const computeOutcomePosition = (measurements, outcomes, outcomeId) => {
    const width = measurements.width || 0;
    const outcomeIndex = findIndex(outcomes, { id: outcomeId });
    const x = measurements.x + width;
    const numberOutcomes = size(outcomes);
    const outcomeScale = numberOutcomes - outcomeIndex;
    const y = numberOutcomes === 1
        ? measurements.y + SINGLE_OUTCOME_Y_OFFSET
        : measurements.y + measurements.height - (MULTI_OUTCOME_Y_OFFSET + OUTCOME_Y_SPACING * outcomeScale);
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
        this.handleConnectionFinish = this.handleConnectionFinish.bind(this);
        this.handleConnectionStart = this.handleConnectionStart.bind(this);
        this.handleNewConnectionFinish = this.handleNewConnectionFinish.bind(this);
        this.handleNewConnectionMove = this.handleNewConnectionMove.bind(this);
        this.handleSvgClick = this.handleSvgClick.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentWillUnmount () {
        const { ownerDocument } = this.domNode;
        ownerDocument.removeEventListener("mousemove", this.handleNewConnectionMove);
        ownerDocument.removeEventListener("mouseup", this.handleNewConnectionFinish);
    }

    handleConnectionStart (nodeId, event, outcome) {
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
    }

    handleNewConnectionMove (event) {
        const newConnectionEnd = getNewConnectionEnd(event, this.domNode);
        this.setState({
            newConnectionEndX: newConnectionEnd.x,
            newConnectionEndY: newConnectionEnd.y
        });
    }

    handleNewConnectionFinish () {
        const { ownerDocument } = this.domNode;
        ownerDocument.removeEventListener("mousemove", this.handleNewConnectionMove);
        ownerDocument.removeEventListener("mouseup", this.handleNewConnectionFinish);
        this.setState({
            newConnectionFromNodeId: null
        });
    }

    handleConnectionFinish (nodeId) {
        if (this.state.newConnectionFromNodeId !== null) {
            this.props.onNewConnection(
                this.state.newConnectionFromNodeId,
                this.state.newConnectionFromOutcome,
                nodeId
            );
        }
    }

    handleSvgClick () {
        this.props.onNodeDeselect();
    }

    setRef (element) {
        this.domNode = element;
    }

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

        const measurements = mapValues(this.props.measurements, (measurement) => {
            return measurement.id === this.state.draggingNodeId
                ? { ...measurement, x: this.state.draggingNodeX, y: this.state.draggingNodeY }
                : measurement;
        });

        const nodesWithConnectedInputs = reduce(pluck(this.props.nodes, "connections"),
            (result, connections) => union(result, values(connections)), []);
        const nodeComponents = map(omit(this.props.nodes, keys(this.props.nodesInPages)), (node, nodeId) => {
            const nodeProperties = this.props.localNodeProperties[this.state.draggingNodeId];
            const nodeType = get(nodeProperties, "_type._id");
            const measurement = measurements[nodeId];
            const createNode = NodeFactory[`create${node.nodeType}`] || NodeFactory.createNode;
            return createNode(nodeId, node, measurement || { x: 0, y: 0 }, contains(nodesWithConnectedInputs, nodeId), {
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

        const connectionComponents = chain(this.props.nodes)
            .map((node, nodeId) => {
                const connections = this.props.nodes[nodeId].connections;
                const outcomes = this.props.nodes[nodeId]._outcomes;
                return map(connections, (destinationNodeId, outcomeId) => {
                    const fromNodeMeasurements = measurements[nodeId];
                    const toNodeMeasurements = measurements[destinationNodeId];
                    if (fromNodeMeasurements && toNodeMeasurements) {
                        const start = {
                            id: nodeId,
                            width: fromNodeMeasurements.width,
                            height: fromNodeMeasurements.height,
                            ...computeOutcomePosition(fromNodeMeasurements, outcomes, outcomeId)
                        };
                        const end = {
                            id: destinationNodeId,
                            ...computeInputPosition(toNodeMeasurements)
                        };
                        return (
                            <Connection
                                end={ end }
                                isInputForSelectedNode={ this.props.selectedNodeId === destinationNodeId }
                                isOutputForSelectedNode={ this.props.selectedNodeId === nodeId }
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
            const fromNodeMeasurements = this.props.measurements[this.state.newConnectionFromNodeId];
            const fromNodeOutcomes = this.props.nodes[this.state.newConnectionFromNodeId]._outcomes;
            const start = {
                id: this.state.newConnectionFromNodeId,
                width: fromNodeMeasurements.width,
                height: fromNodeMeasurements.height,
                ...computeOutcomePosition(fromNodeMeasurements, fromNodeOutcomes, this.state.newConnectionFromOutcome)
            };
            const end = {
                x: this.state.newConnectionEndX,
                y: this.state.newConnectionEndY
            };

            connectionComponents.push(
                <Connection end={ end } isNew key={ connectionComponents.length + 1 } start={ start } />
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
    localNodeProperties: PropTypes.objectOf(PropTypes.shape({
        _type: PropTypes.shape({
            _id: PropTypes.string
        }),
        nodes: PropTypes.arrayOf(PropTypes.string)
    })).isRequired,
    measurements: PropTypes.objectOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        height: PropTypes.number.isRequired,
        width: PropTypes.number.isRequired,
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired
    })),
    nodes: PropTypes.objectOf(PropTypes.shape({
        _outcomes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.string.isRequired,
            displayName: PropTypes.string.isRequired
        })).isRequired,
        connections: PropTypes.objectOf(PropTypes.objectOf(PropTypes.string)).isRequired
    })).isRequired,
    nodesInPages: PropTypes.objectOf(PropTypes.string).isRequired,
    onNewConnection: PropTypes.func.isRequired,
    onNewNodeCreate: PropTypes.func.isRequired,
    onNodeDeselect: PropTypes.func.isRequired,
    onNodeDimensionsChange: PropTypes.func.isRequired,
    onNodeMove: PropTypes.func.isRequired,
    onNodePropertiesChange: PropTypes.func.isRequired,
    onNodeSelect: PropTypes.func.isRequired,
    selectedNodeId: PropTypes.string.isRequired
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
