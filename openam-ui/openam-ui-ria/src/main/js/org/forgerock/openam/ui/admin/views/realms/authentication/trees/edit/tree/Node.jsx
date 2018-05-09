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
import { Clearfix } from "react-bootstrap";
import { findKey, has, map, size } from "lodash";
import Draggable from "react-draggable";
import PropTypes from "prop-types";
import React from "react";
import classNames from "classnames";

import NodeOutcome from "./NodeOutcome";
import NodeOutcomeWithLabel from "./NodeOutcomeWithLabel";
import { TREE_PADDING } from "./TreePadding";
import TreeComponent from "./TreeComponent";
import { start } from "store/modules/local/config/realm/authentication/trees/current/nodes/static";

const Node = ({
    children,
    id,
    isInputConnected,
    isSelected,
    node,
    onConnectionFinish,
    onConnectionStart,
    onDrag,
    onDragStop,
    onMeasure,
    onSelect,
    x = 0,
    y = 0
}) => {
    const isStartNode = id === findKey(start());
    const handleDrag = (event, { x, y }) => {
        onDrag(id, x, y, x + event.offsetX, y + event.offsetY);
    };
    const handleDragStop = (event, { x, y }) => onDragStop(id, x, y, x + event.offsetX, y + event.offsetY);
    const handleNodeMouseDown = () => {
        const selectedId = isStartNode ? null : id;
        onSelect(selectedId, node.nodeType);
    };
    const handleNodeMouseUp = () => {
        if (!isStartNode) {
            onConnectionFinish(id);
        }
    };
    const handleOutcomeMouseDown = (event, name) => {
        event.stopPropagation();
        onConnectionStart(id, event, name);
    };
    const handleOutcomeMouseUp = (event) => event.stopPropagation();

    const outcomes = node._outcomes;
    let contentBefore = "";
    let contentAfter = "";
    if (outcomes.length === 1) {
        const outcomeId = outcomes[0].id;
        contentBefore = (
            <div className="pull-right">
                <NodeOutcome
                    id={ outcomeId }
                    isConnected={ has(node.connections, outcomeId) }
                    name={ outcomes[0].displayName }
                    onMouseDown={ handleOutcomeMouseDown }
                    onMouseUp={ handleOutcomeMouseUp }
                />
            </div>
        );
    } else if (outcomes.length > 1) {
        contentAfter = (
            <Clearfix className="authtree-node-outcomes">
                <ul className="pull-right">{ map(outcomes, ({ id, displayName }) => (
                    <NodeOutcomeWithLabel
                        id={ id }
                        isConnected={ has(node.connections, id) }
                        key={ id }
                        name={ displayName }
                        onMouseDown={ handleOutcomeMouseDown }
                        onMouseUp={ handleOutcomeMouseUp }
                    />)
                )}
                </ul>
            </Clearfix>
        );
    }

    const isOutputsConnected = size(node.connections) === size(node._outcomes);

    return (
        <Draggable
            bounds={ { top: TREE_PADDING, left: TREE_PADDING } }
            onDrag={ handleDrag }
            onMouseDown={ handleNodeMouseDown }
            onStop={ handleDragStop }
            position={ { x, y } }
        >
            <div id={ `node-${id}` /* used to identify node in UI tests */ }>
                <TreeComponent
                    className={ classNames({
                        "authtree-node-has-input": !isStartNode,
                        "authtree-node-has-input-invalid": !isInputConnected,
                        "authtree-node-invalid": !isStartNode && !isInputConnected || !isOutputsConnected
                    }) }
                    id={ id }
                    isSelected={ isSelected }
                    node={ node }
                    onMeasure={ onMeasure }
                    onMouseDown={ handleNodeMouseDown }
                    onMouseUp={ handleNodeMouseUp }
                    titleDecoration={ contentBefore }
                >
                    { children }
                    { contentAfter }
                </TreeComponent>
            </div>
        </Draggable>
    );
};

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
        connections: PropTypes.objectOf(PropTypes.objectOf(PropTypes.string)).isRequired,
        displayName: PropTypes.string.isRequired,
        nodeType: PropTypes.string.isRequired
    }).isRequired,
    onConnectionFinish: PropTypes.func.isRequired,
    onConnectionStart: PropTypes.func.isRequired,
    onDrag: PropTypes.func.isRequired,
    onDragStop: PropTypes.func.isRequired,
    onMeasure: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    x: PropTypes.number,
    y: PropTypes.number
};

export default Node;
