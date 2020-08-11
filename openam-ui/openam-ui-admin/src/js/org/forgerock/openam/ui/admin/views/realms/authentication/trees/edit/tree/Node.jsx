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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { Clearfix } from "react-bootstrap";
import { findKey, has, map, size } from "lodash";
import Draggable from "react-draggable";
import PropTypes from "prop-types";
import React, { Component } from "react";
import classNames from "classnames";

import NodeOutcome from "./NodeOutcome";
import NodeOutcomeWithLabel from "./NodeOutcomeWithLabel";
import { TREE_PADDING } from "./TreePadding";
import TreeComponent from "./TreeComponent";
import { start } from "store/modules/local/config/realm/authentication/trees/current/nodes/static";

class Node extends Component {
    handleDrag = (event, { x, y }) => {
        this.props.onDrag(this.props.id, x, y, x + event.offsetX, y + event.offsetY);
    };

    handleDragStop = (event, { x, y }) => {
        this.props.onDragStop(this.props.id, x, y, x + event.offsetX, y + event.offsetY);
    };

    handleNodeMouseDown = () => {
        const selectedId = this.isStartNode() ? null : this.props.id;
        this.props.onSelect(selectedId, this.props.node.nodeType);
    };

    handleNodeMouseUp = () => {
        if (!this.isStartNode()) {
            this.props.onConnectionFinish(this.props.id);
        }
    };

    handleOutcomeMouseDown = (event, name) => {
        event.stopPropagation();
        this.props.onConnectionStart(this.props.id, event, name);
    };

    handleOutcomeMouseUp = (event) => {
        event.stopPropagation();
    };

    isStartNode = () => {
        return this.props.id === findKey(start());
    };

    render () {
        const outcomes = this.props.node._outcomes;
        let contentBefore = "";
        let contentAfter = "";
        if (outcomes.length === 1) {
            const outcomeId = outcomes[0].id;
            contentBefore = (
                <div className="pull-right">
                    <NodeOutcome
                        id={ outcomeId }
                        isConnected={ has(this.props.node.connections, outcomeId) }
                        name={ outcomes[0].displayName }
                        onMouseDown={ this.handleOutcomeMouseDown }
                        onMouseUp={ this.handleOutcomeMouseUp }
                    />
                </div>
            );
        } else if (outcomes.length > 1) {
            contentAfter = (
                <Clearfix className="authtree-node-outcomes">
                    <ul className="pull-right">{ map(outcomes, ({ id, displayName }) => (
                        <NodeOutcomeWithLabel
                            id={ id }
                            isConnected={ has(this.props.node.connections, id) }
                            key={ id }
                            name={ displayName }
                            onMouseDown={ this.handleOutcomeMouseDown }
                            onMouseUp={ this.handleOutcomeMouseUp }
                        />)
                    )}
                    </ul>
                </Clearfix>
            );
        }

        const isOutputsConnected = size(this.props.node.connections) === size(this.props.node._outcomes);

        return (
            <Draggable
                bounds={ { top: TREE_PADDING, left: TREE_PADDING } }
                onDrag={ this.handleDrag }
                onMouseDown={ this.handleNodeMouseDown }
                onStop={ this.handleDragStop }
                position={ { x: this.props.node.x, y: this.props.node.y } }
            >
                <div id={ `node-${this.props.id}` /* used to identify node in UI tests */ }>
                    <TreeComponent
                        className={ classNames({
                            "authtree-node-has-input": !this.isStartNode(),
                            "authtree-node-has-input-invalid": !this.props.isInputConnected,
                            "authtree-node-invalid": !this.isStartNode() &&
                                !this.props.isInputConnected || !isOutputsConnected
                        }) }
                        id={ this.props.id }
                        isSelected={ this.props.isSelected }
                        node={ this.props.node }
                        onMeasure={ this.props.onMeasure }
                        onMouseDown={ this.handleNodeMouseDown }
                        onMouseUp={ this.handleNodeMouseUp }
                        titleDecoration={ contentBefore }
                    >
                        { this.props.children }
                        { contentAfter }
                    </TreeComponent>
                </div>
            </Draggable>
        );
    }
}

Node.propTypes = {
    children: PropTypes.node,
    id: PropTypes.string.isRequired,
    isInputConnected: PropTypes.bool.isRequired,
    isSelected: PropTypes.bool.isRequired,
    node: PropTypes.shape({
        _outcomes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.string.isRequired,
            displayName: PropTypes.string.isRequired
        })).isRequired,
        connections: PropTypes.objectOf(PropTypes.string),
        displayName: PropTypes.string.isRequired,
        nodeType: PropTypes.string.isRequired,
        x: PropTypes.number,
        y: PropTypes.number
    }).isRequired,
    onConnectionFinish: PropTypes.func.isRequired,
    onConnectionStart: PropTypes.func.isRequired,
    onDrag: PropTypes.func.isRequired,
    onDragStop: PropTypes.func.isRequired,
    onMeasure: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired
};

export default Node;
