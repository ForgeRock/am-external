/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { DragSource } from "react-dnd";
import PropTypes from "prop-types";
import React from "react";

import { PAGE_CHILD_NODE_TYPE }
    from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/EditTreeDragItemTypes";
import TreeComponent from "./TreeComponent";

const PageNodeListItem = ({ node, isDragging, isSelected, connectDragSource, onMouseDown }) => {
    return isDragging ? "" : connectDragSource(
        <div
            className="page-list-child"
            id={ `node-${node._id}` }
        >
            <TreeComponent
                id={ node._id /* used to identify node in UI tests */ }
                isInPage
                isSelected={ isSelected }
                key={ node._id }
                node={ node }
                onMouseDown={ onMouseDown }
            />
        </div>
    );
};

PageNodeListItem.propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    isDragging: PropTypes.bool.isRequired,
    isSelected: PropTypes.bool.isRequired,
    node: PropTypes.shape({
        _id: PropTypes.string.isRequired
    }).isRequired,
    onMouseDown: PropTypes.func.isRequired,
    pageId: PropTypes.string.isRequired
};

const source = {
    beginDrag (props) {
        return {
            nodeId: props.node._id,
            pageId: props.pageId
        };
    }
};

const collect = (connect, monitor) => {
    return {
        connectDragSource: connect.dragSource(),
        isDragging: monitor.isDragging()
    };
};

export default DragSource(PAGE_CHILD_NODE_TYPE, source, collect)(PageNodeListItem);
