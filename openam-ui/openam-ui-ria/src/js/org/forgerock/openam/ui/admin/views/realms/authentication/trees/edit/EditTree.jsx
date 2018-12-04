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
import { cloneDeep, debounce, get, max, reduce, without } from "lodash";
import { DragDropContext } from "react-dnd";
import { Grid, Row } from "react-bootstrap";
import classnames from "classnames";
import HTML5Backend from "react-dnd-html5-backend";
import Measure from "react-measure";
import PropTypes from "prop-types";
import React, { Component } from "react";

import EditTreeNodeTypes from "./nodeTypes/EditTreeNodeTypes";
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

const calculateCanvasDimensions = (measurements, containerHeight, containerWidth) => {
    const furthestBottom = reduce(measurements, (furthestBottom, measurement) => {
        return max([furthestBottom, measurement.y + measurement.height]);
    }, 0);
    const furthestRight = reduce(measurements, (furthestRight, measurement) => {
        return max([furthestRight, measurement.x + measurement.width]);
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
        this.handleNodeDelete = this.handleNodeDelete.bind(this);
        this.handleNodePropertiesChange = this.handleNodePropertiesChange.bind(this);
        this.handleTreeMeasure = this.handleTreeMeasure.bind(this);
        this.handleWindowResize = this.handleWindowResize.bind(this);
    }

    componentWillMount () {
        window.addEventListener("resize", debounce(this.handleWindowResize, 100));
    }

    componentWillUnmount () {
        window.removeEventListener("resize", this.handleWindowResize);
    }

    handleWindowResize () {
        this.forceUpdate();
    }

    handleTreeMeasure (dimensions) {
        this.setState({
            containerWidth: dimensions.width,
            containerX: dimensions.left,
            containerY: dimensions.top
        });
    }

    handleNodeDelete () {
        this.props.onNodeDelete(this.props.selectedNode.id);
    }

    handleNodePropertiesChange (id, type, properties) {
        debouncedNodePropertiesChange(this.props.onNodePropertiesChange, id, type, properties);
    }

    render () {
        const containerHeight = calculateTreeContainerHeight(this.props.isFullscreen);
        const { canvasHeight, canvasWidth } =
            calculateCanvasDimensions(this.props.measurements, containerHeight, this.state.containerWidth);
        const getSelectedNodePageProperties = () => {
            const selectedNodeId = this.props.selectedNode.id;
            const pageId = this.props.nodesInPages[selectedNodeId];
            if (pageId) {
                return this.props.localNodeProperties[pageId].nodes.filter((n) => n._id === selectedNodeId)[0];
            } else {
                return this.props.nodes[selectedNodeId];
            }
        };

        const removeCurrentTreeFromInnerNodeSchema = (schema) => {
            const updatedSchema = cloneDeep(schema);
            updatedSchema.properties.tree.enumNames =
                without(updatedSchema.properties.tree.enumNames, this.props.treeId);
            updatedSchema.properties.tree.enum =
                without(updatedSchema.properties.tree.enum, this.props.treeId);
            return updatedSchema;
        };

        const getSchema = (selectedNodeType) => {
            let schema;
            if (this.props.selectedNodeSchema) {
                if (selectedNodeType === PAGE_NODE_TYPE) {
                    schema = {};
                } else if (selectedNodeType === INNER_TREE_NODE_TYPE) {
                    schema = removeCurrentTreeFromInnerNodeSchema(this.props.selectedNodeSchema);
                } else {
                    schema = this.props.selectedNodeSchema;
                }
            }
            return schema;
        };

        if (this.props.isFetching) {
            return <Loading />;
        } else {
            const nodeTreeProperties = getSelectedNodePageProperties();
            const selectedNodeType = get(this.props.selectedNode, "type");
            const schema = getSchema(selectedNodeType);
            const collapsedRight = !!this.props.selectedNodeSchema && this.props.selectedNode.id;
            return (
                <div>
                    <SubNav fluid>
                        <SubNavRealmHomeLink />
                    </SubNav>
                    <Grid fluid>
                        <Row>
                            <Fullscreen isFullscreen={ this.props.isFullscreen }>
                                <div className="authtree-container">
                                    <EditTreeNodeTypes
                                        nodeTypes={ this.props.nodeTypes }
                                    />
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
                                                localNodeProperties={ this.props.localNodeProperties }
                                                measurements={ this.props.measurements }
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
                </div>
            );
        }
    }
}

EditTree.propTypes = {
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
    measurements: PropTypes.objectOf(PropTypes.object).isRequired,
    nodeTypes: PropTypes.objectOf(PropTypes.object).isRequired,
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
