/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { DragSource } from "react-dnd";
import { Panel } from "react-bootstrap";
import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

import { NEW_NODE_TYPE } from "./EditTreeDragItemTypes";

const EditTreeNodeTypeItem = ({ displayName, isDragging, connectDragSource, ...restProps }) => {
    return connectDragSource(
        <div
            className={ classnames({
                "edit-tree-type-item-selected": isDragging,
                "authtree-content-left-item": true
            }) }
            { ...restProps }
        >
            <Panel>
                <span className="fa-stack authtree-content-left-item-icon">
                    <i className="fa fa-circle fa-stack-2x text-primary" />
                    <i className="fa fa-user fa-stack-1x fa-inverse" />
                </span>
                { displayName }
            </Panel>
        </div>
    );
};

EditTreeNodeTypeItem.propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    displayName: PropTypes.string.isRequired,
    isDragging: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired
};

const source = {
    beginDrag (props) {
        return {
            displayName: props.displayName,
            nodeType: props.nodeType
        };
    }
};

const collect = (connect, monitor) => {
    return {
        connectDragSource: connect.dragSource(),
        isDragging: monitor.isDragging()
    };
};

export default DragSource(NEW_NODE_TYPE, source, collect)(EditTreeNodeTypeItem);
