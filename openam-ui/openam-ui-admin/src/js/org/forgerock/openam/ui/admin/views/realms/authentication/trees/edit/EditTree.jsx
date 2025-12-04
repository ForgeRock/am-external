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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
import { debounce, get, has, max, pull, reduce, without } from "lodash";
import { DragDropContext } from "react-dnd";
import { Grid, Row } from "react-bootstrap";
import classnames from "classnames";
import HTML5Backend from "react-dnd-html5-backend";
import Measure from "react-measure";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

import EditTreeNodeTypesContainer from "./nodeTypes/EditTreeNodeTypesContainer";
import EditTreeNodeProperties from "./EditTreeNodeProperties";
import EditTreeToolbar from "./toolbar/EditTreeToolbar";
import Fullscreen from "components/Fullscreen";
import Loading from "components/Loading";
import {
    INNER_TREE_NODE_TYPE,
    PAGE_NODE_TYPE
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";
import SubNav from "components/SubNav";
import SubNavRealmHomeLink from "org/forgerock/openam/ui/admin/views/realms/SubNavRealmHomeLink";
import Tree from "./tree/Tree";
import { TREE_PADDING } from "./tree/TreePadding";

const calculateTreeContainerHeight = (isFullscreen) => {
    const NAVBAR_HEIGHT = 76;
    const FOOTER_HEIGHT = 81;
    const SUBNAV_HEIGHT = 57;
    const TOOLBAR_HEIGHT = 55;

    return isFullscreen
        ? window.innerHeight - TOOLBAR_HEIGHT
        : window.innerHeight - TOOLBAR_HEIGHT - SUBNAV_HEIGHT - NAVBAR_HEIGHT - FOOTER_HEIGHT;
};

const calculateCanvasDimensions = (dimensions, nodes, containerHeight, containerWidth) => {
    const furthestBottom = reduce(dimensions, (furthestBottom, dimension) => {
        return max([furthestBottom, nodes[dimension.id].y + dimension.height]);
    }, 0);
    const furthestRight = reduce(dimensions, (furthestRight, dimension) => {
        return max([furthestRight, nodes[dimension.id].x + dimension.width]);
    }, 0);

    const canvasHeight = max([containerHeight, furthestBottom + TREE_PADDING]);
    const canvasWidth = max([containerWidth, furthestRight + TREE_PADDING]);

    return { canvasWidth, canvasHeight };
};

const debouncedNodePropertiesChange = debounce((onNodePropertiesChange, id, type, properties) => {
    // The need to have instant feedback upon changing an input, is greater than the need to debounce when entering
    // multiple characters into an input. The compromise for usability is to set the leading flag to true.
    onNodePropertiesChange(id, type, properties);
}, 500, { leading: true });

class EditTree extends Component {
    constructor (props) {
        super(props);
        this.state = { containerWidth: 0 };
    }

    UNSAFE_componentWillMount () {
        window.addEventListener("resize", debounce(this.handleWindowResize, 100));
    }

    componentWillUnmount () {
        window.removeEventListener("resize", this.handleWindowResize);
    }

    handleWindowResize = () => {
        this.forceUpdate();
    };

    handleTreeMeasure = (dimensions) => {
        this.setState({
            containerWidth: dimensions.width,
            containerX: dimensions.left,
            containerY: dimensions.top
        });
    };

    handleNodeDelete = () => {
        this.props.onNodeDelete(this.props.selectedNode.id);
    };

    handleNodePropertiesChange = (id, type, properties) => {
        debouncedNodePropertiesChange(this.props.onNodePropertiesChange, id, type, properties);
    };

    render () {
        const containerHeight = calculateTreeContainerHeight(this.props.isFullscreen);
        const { canvasHeight, canvasWidth } = calculateCanvasDimensions(
            this.props.dimensions, this.props.nodes, containerHeight, this.state.containerWidth
        );
        const getSelectedNodePageProperties = () => {
            const selectedNodeId = this.props.selectedNode.id;
            const pageId = this.props.nodesInPages[selectedNodeId];
            if (pageId) {
                return this.props.localNodeProperties[pageId].nodes.filter((n) => n._id === selectedNodeId)[0];
            } else {
                return this.props.nodes[selectedNodeId];
            }
        };

        const removeCurrentTreeFromInnerNodeSchema = ({ ...schema }) => {
            schema.properties.tree.enumNames =
                without(schema.properties.tree.enumNames, this.props.treeId);
            schema.properties.tree.enum =
                without(schema.properties.tree.enum, this.props.treeId);
            return schema;
        };

        const removeNodesFromPageNodeSchema = ({ ...schema }) => {
            delete schema.properties.nodes;
            if (schema.required) {
                return {
                    ...schema,
                    required: pull(schema.required, "nodes")
                };
            }
        };

        const getSchema = (selectedNodeType) => {
            let schema;
            if (selectedNodeType === INNER_TREE_NODE_TYPE) {
                schema = removeCurrentTreeFromInnerNodeSchema(this.props.selectedNodeSchema);
            } else if (selectedNodeType === PAGE_NODE_TYPE &&
                has(this.props.selectedNodeSchema, "properties.nodes")) {
                schema = removeNodesFromPageNodeSchema(this.props.selectedNodeSchema);
            } else {
                schema = { ...this.props.selectedNodeSchema };
            }
            return schema;
        };

        if (this.props.isFetching) {
            return <Loading />;
        } else {
            const nodeTreeProperties = getSelectedNodePageProperties();
            const selectedNodeType = get(this.props.selectedNode, "type");
            const schema = this.props.selectedNodeSchema ? getSchema(selectedNodeType) : undefined;
            const collapsedRight = !!(this.props.selectedNodeSchema && this.props.selectedNode.id);
            return (
                <Fragment>
                    <SubNav fluid>
                        <SubNavRealmHomeLink />
                    </SubNav>
                    <Grid fluid>
                        <Row>
                            <Fullscreen isFullscreen={ this.props.isFullscreen }>
                                <div className="authtree-container">
                                    <EditTreeNodeTypesContainer />
                                    <div
                                        className={ classnames({
                                            "authtree-content-centre": true,
                                            "collapsed-right": collapsedRight
                                        }) }
                                    >
                                        <EditTreeToolbar
                                            invertTooltipPlacement={ this.props.isFullscreen }
                                            isDeleteNodeEnabled={ !!this.props.selectedNode.id }
                                            onAutoLayout={ this.props.onAutoLayout }
                                            onFullscreenToggle={ this.props.onFullscreenToggle }
                                            onNodeDelete={ this.handleNodeDelete }
                                            onTreeSave={ this.props.onTreeSave }
                                        />
                                        <Measure onMeasure={ this.handleTreeMeasure }>
                                            <Tree
                                                canvasHeight={ canvasHeight }
                                                canvasWidth={ canvasWidth }
                                                containerHeight={ containerHeight }
                                                dimensions={ this.props.dimensions }
                                                localNodeProperties={ this.props.localNodeProperties }
                                                nodes={ this.props.nodes }
                                                nodesInPages={ this.props.nodesInPages }
                                                onNewConnection={ this.props.onNewConnection }
                                                onNewNodeCreate={ this.props.onNewNodeCreate }
                                                onNodeDeselect={ this.props.onNodeDeselect }
                                                onNodeDimensionsChange={ this.props.onNodeDimensionsChange }
                                                onNodeMove={ this.props.onNodeMove }
                                                onNodePropertiesChange={ this.props.onNodePropertiesChange }
                                                onNodeSelect={ this.props.onNodeSelect }
                                                selectedNodeId={ this.props.selectedNode.id }
                                            />
                                        </Measure>
                                    </div>
                                    <EditTreeNodeProperties
                                        isExpanded={ collapsedRight }
                                        isNew={ this.props.selectedNodeIsNew }
                                        nodeId={ this.props.selectedNode.id }
                                        nodeName={ get(nodeTreeProperties, "displayName") }
                                        nodeType={ this.props.selectedNode.type }
                                        onDisplayNameChange={ this.props.onNodeDisplayNameChange }
                                        onFieldChange={ this.props.onNodePropertiesFieldChange }
                                        onPropertiesChange={ this.handleNodePropertiesChange }
                                        properties={ this.props.selectedNodeProperties }
                                        schema={ schema }
                                    />
                                </div>
                            </Fullscreen>
                        </Row>
                    </Grid>
                </Fragment>
            );
        }
    }
}

EditTree.propTypes = {
    dimensions: PropTypes.objectOf(PropTypes.object).isRequired,
    isFetching: PropTypes.bool.isRequired,
    isFullscreen: PropTypes.bool.isRequired,
    localNodeProperties: PropTypes.objectOf(PropTypes.shape({
        _type: PropTypes.shape({
            _id: PropTypes.string
        }).isRequired,
        nodes: PropTypes.arrayOf(PropTypes.shape({
            _id: PropTypes.string
        }))
    })).isRequired,
    nodes: PropTypes.objectOf(PropTypes.object).isRequired,
    nodesInPages: PropTypes.objectOf(PropTypes.string).isRequired,
    onAutoLayout: PropTypes.func.isRequired,
    onFullscreenToggle: PropTypes.func.isRequired,
    onNewConnection: PropTypes.func.isRequired,
    onNewNodeCreate: PropTypes.func.isRequired,
    onNodeDelete: PropTypes.func.isRequired,
    onNodeDeselect: PropTypes.func.isRequired,
    onNodeDimensionsChange: PropTypes.func.isRequired,
    onNodeDisplayNameChange: PropTypes.func.isRequired,
    onNodeMove: PropTypes.func.isRequired,
    onNodePropertiesChange: PropTypes.func.isRequired,
    onNodePropertiesFieldChange: PropTypes.func.isRequired,
    onNodeSelect: PropTypes.func.isRequired,
    onTreeSave: PropTypes.func.isRequired,
    selectedNode: PropTypes.shape({
        id: PropTypes.string,
        type: PropTypes.string
    }),
    selectedNodeIsNew: PropTypes.bool.isRequired,
    selectedNodeProperties: PropTypes.objectOf(PropTypes.any),
    selectedNodeSchema: PropTypes.objectOf(PropTypes.any),
    treeId: PropTypes.string.isRequired
};

export default DragDropContext(HTML5Backend)(EditTree);
