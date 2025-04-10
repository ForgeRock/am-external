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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Clearfix } from "react-bootstrap";
import { DropTarget } from "react-dnd";
import Measure from "react-measure";
import PropTypes from "prop-types";
import React, { Component } from "react";
import classNames from "classnames";
import { cloneDeep, includes, isEqual } from "lodash";
import { t } from "i18next";

import {
    FAILURE_NODE_ID,
    FAILURE_NODE_TYPE,
    INNER_TREE_NODE_TYPE,
    PAGE_NODE_TYPE,
    START_NODE_ID,
    SUCCESS_NODE_ID,
    SUCCESS_NODE_TYPE
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";
import { NEW_NODE_TYPE, PAGE_CHILD_NODE_TYPE }
    from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/nodeTypes/EditTreeDragItemTypes";
import PageNodeListItem from "./PageNodeListItem";
import { add as addNodeInPage }
    from "store/modules/local/config/realm/authentication/trees/current/nodes/pages/childnodes";
import { addOrUpdate as setLocalNodeProperties }
    from "store/modules/local/config/realm/authentication/trees/current/nodes/properties";
import { add as addPagePosition }
    from "store/modules/local/config/realm/authentication/trees/current/nodes/pages/positions";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import store from "store";

class PageNodeList extends Component {
    componentDidUpdate () {
        const xOffset = this.props.pageNodeLeft - this.props.pageNodeX;
        const yOffset = this.props.pageNodeTop - this.props.pageNodeY;
        const position = {
            height: this.state.height,
            width: this.state.width,
            x: this.state.left - xOffset,
            y: this.state.top - yOffset
        };
        store.dispatch(addPagePosition(this.props.id, position));
    }

    handleAddNode (node) {
        if (this.props.numberOutcomes > 1) {
            Messages.addMessage({
                message: t("console.authentication.trees.edit.nodes.pages.alreadyComplete"),
                type: Messages.TYPE_DANGER
            });
        } else {
            const id = this.props.onNewNodeCreate(node);
            const config = cloneDeep(this.props.pageConfig);
            config.nodes.push({ ...node, _id: id });
            store.dispatch(setLocalNodeProperties(config));
            store.dispatch(addNodeInPage(id, this.props.id));
            this.props.onNodePropertiesChange(this.props.id, PAGE_NODE_TYPE, config);
        }
    }

    handleMeasure = (dimensions) => {
        if (!isEqual(dimensions, this.state)) {
            this.setState(dimensions);
        }
    };

    handleMoveNodeToPage ({ nodeId, pageId }) {
        if (pageId === this.props.id) {
            return;
        }
        store.dispatch(addNodeInPage(nodeId, pageId));
        const config = cloneDeep(this.props.pageConfig);
        this.props.onNodePropertiesChange(this.props.id, PAGE_NODE_TYPE, config);
    }

    isDraggingNodeOver (draggingNode) {
        if (!draggingNode) {
            return false;
        }
        const invalidIds = [FAILURE_NODE_ID, START_NODE_ID, SUCCESS_NODE_ID];
        const invalidTypes = [INNER_TREE_NODE_TYPE, PAGE_NODE_TYPE];
        if (includes(invalidIds, draggingNode.id) || includes(invalidTypes, draggingNode.type)) {
            return false;
        }
        const pagePositions = store.getState().local.config.realm.authentication.trees.current.nodes.pages.positions;
        const currentPosition = pagePositions[this.props.id];
        return draggingNode.mouseX > currentPosition.x &&
            draggingNode.mouseX < currentPosition.x + currentPosition.width &&
            draggingNode.mouseY > currentPosition.y &&
            draggingNode.mouseY < currentPosition.y + currentPosition.height;
    }

    render () {
        const {
            canDrop,
            connectDropTarget,
            draggingNode,
            id,
            isDragAndDropOver,
            pageConfig,
            onNodeSelect
        } = this.props;
        const isOver = (isDragAndDropOver && canDrop) || this.isDraggingNodeOver(draggingNode);

        const handleNodeMouseDown = (id, nodeType) => (event) => {
            event.stopPropagation();
            onNodeSelect(id, nodeType);
        };
        const content = pageConfig.nodes.map((node) => {
            return (
                <PageNodeListItem
                    isSelected={ node._id === this.props.selectedNodeId }
                    key={ node._id }
                    node={ node }
                    onMouseDown={ handleNodeMouseDown(node._id, node.nodeType) }
                    pageId={ id }
                />
            );
        });
        if (pageConfig.nodes.length === 0) {
            content.push((
                <small key="noNodes">
                    { t("console.authentication.trees.edit.nodes.pages.noNodes") }
                </small>
            ));
        }
        return connectDropTarget(
            <div>
                <Measure onMeasure={ this.handleMeasure }>
                    <Clearfix
                        className={ classNames({
                            "authtree-page-nodes": true,
                            "authtree-page-nodes-drag-hover": isOver
                        }) }
                    >
                        { content }
                    </Clearfix>
                </Measure>
            </div>
        );
    }
}

PageNodeList.propTypes = {
    canDrop: PropTypes.bool.isRequired,
    connectDropTarget: PropTypes.func.isRequired,
    draggingNode: PropTypes.shape({
        id: PropTypes.string.isRequired,
        type: PropTypes.string.isRequired,
        mouseX: PropTypes.number.isRequired,
        mouseY: PropTypes.number.isRequired
    }),
    id: PropTypes.string.isRequired,
    isDragAndDropOver: PropTypes.bool.isRequired,
    numberOutcomes: PropTypes.number.isRequired,
    onNewNodeCreate: PropTypes.func.isRequired,
    onNodePropertiesChange: PropTypes.func.isRequired,
    onNodeSelect: PropTypes.func.isRequired,
    pageConfig: PropTypes.shape({
        nodes: PropTypes.arrayOf(PropTypes.shape({
            _id: PropTypes.string.isRequired
        })).isRequired
    }),
    pageNodeLeft: PropTypes.number.isRequired,
    pageNodeTop: PropTypes.number.isRequired,
    pageNodeX: PropTypes.number.isRequired,
    pageNodeY: PropTypes.number.isRequired,
    selectedNodeId: PropTypes.string
};

const dropTarget = {
    drop (props, monitor, component) {
        if (monitor.getItemType() === NEW_NODE_TYPE) {
            component.handleAddNode(monitor.getItem());
        } else {
            component.handleMoveNodeToPage(monitor.getItem());
        }
    },
    canDrop (props, monitor) {
        const nodeType = monitor.getItem().nodeType;
        const draggingType = monitor.getItemType();
        const invalidNodeTypes = [FAILURE_NODE_TYPE, PAGE_NODE_TYPE, INNER_TREE_NODE_TYPE, SUCCESS_NODE_TYPE];
        return draggingType === PAGE_CHILD_NODE_TYPE || !includes(invalidNodeTypes, nodeType);
    }
};

function collect (connect, monitor) {
    return {
        connectDropTarget: connect.dropTarget(),
        isDragAndDropOver: monitor.isOver(),
        canDrop: monitor.canDrop()
    };
}

export default DropTarget([NEW_NODE_TYPE, PAGE_CHILD_NODE_TYPE], dropTarget, collect)(PageNodeList);
