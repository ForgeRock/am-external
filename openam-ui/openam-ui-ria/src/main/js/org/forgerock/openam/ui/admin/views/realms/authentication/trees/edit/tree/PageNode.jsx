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

import React, { Component } from "react";

import Node from "./Node";
import PageNodeList from "./PageNodeList";
import PropTypes from "prop-types";

class PageNode extends Component {
    constructor (props) {
        super(props);
        this.handleMeasure = this.handleMeasure.bind(this);
        this.state = {
            left: 0,
            top: 0
        };
    }

    handleMeasure (id, dimensions) {
        this.props.onMeasure(id, dimensions);
        this.setState(dimensions);
    }

    render () {
        const {
            draggingNode,
            id,
            isInputConnected,
            isSelected,
            node,
            onConnectionFinish,
            onConnectionStart,
            onDrag,
            onDragStop,
            onNewNodeCreate,
            onNodePropertiesChange,
            onSelect,
            pageConfig,
            selectedNodeId,
            x,
            y
        } = this.props;
        return (
            <Node
                id={ id }
                isInputConnected={ isInputConnected }
                isSelected={ isSelected }
                key={ id }
                node={ node }
                onConnectionFinish={ onConnectionFinish }
                onConnectionStart={ onConnectionStart }
                onDrag={ onDrag }
                onDragStop={ onDragStop }
                onMeasure={ this.handleMeasure }
                onSelect={ onSelect }
                x={ x }
                y={ y }
            >
                <PageNodeList
                    draggingNode={ draggingNode }
                    id={ id }
                    localNodeProperties={ this.props.localNodeProperties }
                    numberOutcomes={ node._outcomes.length }
                    onConnectionFinish={ onConnectionFinish }
                    onNewNodeCreate={ onNewNodeCreate }
                    onNodePropertiesChange={ onNodePropertiesChange }
                    onNodeSelect={ onSelect }
                    pageConfig={ pageConfig }
                    pageNodeLeft={ this.state.left || 0 }
                    pageNodeTop={ this.state.top || 0 }
                    pageNodeX={ x }
                    pageNodeY={ y }
                    selectedNodeId={ selectedNodeId }
                />
            </Node>
        );
    }
}

PageNode.propTypes = {
    draggingNode: PropTypes.shape({
        id: PropTypes.string.isRequired
    }),
    id: PropTypes.string.isRequired,
    isInputConnected: PropTypes.bool.isRequired,
    isSelected: PropTypes.bool.isRequired,
    localNodeProperties: PropTypes.objectOf(PropTypes.object).isRequired,
    node: PropTypes.shape({
        _outcomes: PropTypes.array.isRequired
    }).isRequired,
    onConnectionFinish: PropTypes.func.isRequired,
    onConnectionStart: PropTypes.func.isRequired,
    onDrag: PropTypes.func.isRequired,
    onDragStop: PropTypes.func.isRequired,
    onMeasure: PropTypes.func.isRequired,
    onNewNodeCreate: PropTypes.func.isRequired,
    onNodePropertiesChange: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    pageConfig: PropTypes.shape({
        nodes: PropTypes.arrayOf(PropTypes.shape({
            _id: PropTypes.string.isRequired
        })).isRequired
    }),
    selectedNodeId: PropTypes.string.isRequired,
    x: PropTypes.number,
    y: PropTypes.number
};

export default PageNode;
