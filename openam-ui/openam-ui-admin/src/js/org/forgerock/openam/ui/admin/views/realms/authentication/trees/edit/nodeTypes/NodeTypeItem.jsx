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
 * Copyright 2017-2021 ForgeRock AS.
 */
import { DragSource } from "react-dnd";
import { Badge, Panel } from "react-bootstrap";
import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

import { NEW_NODE_TYPE } from "./EditTreeDragItemTypes";
import EmphasizedText from "components/EmphasizedText";

const NodeTypeItem = ({ connectDragSource, displayName, filter, icon, isDragging, nodeType, tags }) => {
    const badges = filter && tags && tags.length ? (
        <div style={ { margin: "2px 0 0 -7px" } } >
            { tags.map((tag) => (
                <Badge
                    key={ `${displayName}${tag}` }
                    style={ {
                        border: "1px solid #cadeda",
                        color: "#457d78",
                        backgroundColor: "#f1f6f5",
                        marginRight: 2
                    } }
                >
                    <EmphasizedText match={ filter }>{ tag }</EmphasizedText>
                </Badge>
            )) }
        </div>
    ) : undefined;

    return connectDragSource(
        <div
            className={ classnames({
                "edit-tree-type-item-selected": isDragging,
                "authtree-content-left-item": true
            }) }
            data-node-type={ nodeType }
        >
            <Panel>
                <Panel.Body>
                    <span className="fa-stack authtree-content-left-item-icon">
                        <i className="fa fa-circle fa-stack-2x text-primary" />
                        <i className={ `fa fa-${icon} fa-stack-1x fa-inverse` } />
                    </span>
                    <EmphasizedText match={ filter }>{ displayName }</EmphasizedText>
                    { badges }
                </Panel.Body>
            </Panel>
        </div>
    );
};

NodeTypeItem.propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    displayName: PropTypes.string.isRequired,
    filter: PropTypes.string,
    icon: PropTypes.string.isRequired,
    isDragging: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    tags: PropTypes.arrayOf(PropTypes.string)
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

export default DragSource(NEW_NODE_TYPE, source, collect)(NodeTypeItem);
