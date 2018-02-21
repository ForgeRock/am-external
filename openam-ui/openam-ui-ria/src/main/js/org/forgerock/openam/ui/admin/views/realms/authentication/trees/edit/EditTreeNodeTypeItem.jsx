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
import { DragSource } from "react-dnd";
import { Panel } from "react-bootstrap";
import classnames from "classnames";
import React, { PropTypes } from "react";

import { NODE_TYPE } from "./EditTreeDragItemTypes";

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

export default DragSource(NODE_TYPE, source, collect)(EditTreeNodeTypeItem);
