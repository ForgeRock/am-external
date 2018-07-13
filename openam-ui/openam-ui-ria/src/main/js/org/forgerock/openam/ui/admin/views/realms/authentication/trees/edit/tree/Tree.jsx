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
import { chain, contains, findIndex, map, mapValues, max, pluck, reduce, round, size, union, values } from "lodash";
import { DropTarget } from "react-dnd";
import classNames from "classnames";
import React, { Component, PropTypes } from "react";

import Connection from "./Connection";
import Node from "./Node";
import { NODE_TYPE } from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/EditTreeDragItemTypes";
import { TREE_PADDING } from "./TreePadding";

const INPUT_Y_OFFSET = 22;
const SINGLE_OUTCOME_Y_OFFSET = 22;
const OUTCOME_Y_SPACING = 17;
const MULTI_OUTCOME_Y_OFFSET = 55;

const computeInputPosition = (measurements) => ({
    x: measurements.x,
    y: measurements.y + INPUT_Y_OFFSET
});
const computeOutcomePosition = (measurements, outcomes, outcomeId) => {
    const width = measurements.width || 0;
    const outcomeIndex = findIndex(outcomes, { id: outcomeId });
    const yOffset = size(outcomes) > 1
        ? MULTI_OUTCOME_Y_OFFSET + OUTCOME_Y_SPACING * outcomeIndex : SINGLE_OUTCOME_Y_OFFSET;
    return {
        x: measurements.x + width,
        y: measurements.y + yOffset
    };
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

    setRef (element) {
        this.domNode = element;
    }

    render () {
        const handleDrag = (id, x, y) => {
            this.setState({
                draggingNodeId: id,
                draggingNodeX: x,
                draggingNodeY: y
            });
        };
        const handleDragStop = (id, x, y) => {
            this.setState({ draggingNodeId: null });
            this.props.onNodeMove(id, round(x), round(y));
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

        const nodeComponents = map(this.props.nodes, (node, nodeId) => {
            const measurement = measurements[nodeId];
            const x = measurement ? measurement.x : 0;
            const y = measurement ? measurement.y : 0;
            return (
                <Node
                    id={ nodeId }
                    isInputConnected={ contains(nodesWithConnectedInputs, nodeId) }
                    isSelected={ nodeId === this.props.selectedNodeId }
                    key={ nodeId }
                    node={ node }
                    onConnectionFinish={ this.handleConnectionFinish }
                    onConnectionStart={ this.handleConnectionStart }
                    onDrag={ handleDrag }
                    onDragStop={ handleDragStop }
                    onMeasure={ handleNodeMeasure }
                    onSelect={ this.props.onNodeSelect }
                    x={ x }
                    y={ y }
                />
            );
        });

        const connectionComponents = chain(this.props.nodes)
            .map((node, nodeId) => {
                const connections = this.props.nodes[nodeId].connections;
                const outcomes = this.props.nodes[nodeId]._outcomes;
                const components = map(connections, (destinationNodeId, outcomeId) => {
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
                return components;
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

        const handleSvgClick = () => this.props.onNodeDeselect();

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
                        onClick={ handleSvgClick }
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
    onNewConnection: PropTypes.func.isRequired,
    onNodeDeselect: PropTypes.func.isRequired,
    onNodeDimensionsChange: PropTypes.func.isRequired,
    onNodeMove: PropTypes.func.isRequired,
    onNodeSelect: PropTypes.func.isRequired,
    selectedNodeId: PropTypes.string.isRequired
};

const dropTarget = {
    drop (props, monitor, component) {
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
        props.onNewNodeCreate(monitor.getItem(), position);
    }
};

function collect (connect) {
    return {
        connectDropTarget: connect.dropTarget()
    };
}

export default DropTarget(NODE_TYPE, dropTarget, collect)(Tree);
