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

import { DragSource } from "react-dnd";
import PropTypes from "prop-types";
import React from "react";

import { PAGE_CHILD_NODE_TYPE }
    from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/nodeTypes/EditTreeDragItemTypes";
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
