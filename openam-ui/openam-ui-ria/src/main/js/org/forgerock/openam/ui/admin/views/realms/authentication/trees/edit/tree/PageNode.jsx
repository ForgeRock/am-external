/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
