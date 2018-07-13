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
import { DragDropContext } from "react-dnd";
import { debounce, get, max, reduce } from "lodash";
import { Grid, Row } from "react-bootstrap";
import classnames from "classnames";
import HTML5Backend from "react-dnd-html5-backend";
import React, { Component, PropTypes } from "react";
import Measure from "react-measure";

import EditTreeNodeTypes from "./EditTreeNodeTypes";
import EditTreeNodeProperties from "./EditTreeNodeProperties";
import EditTreeToolbar from "./toolbar/EditTreeToolbar";
import Fullscreen from "components/Fullscreen";
import Loading from "components/Loading";
import SubNav from "components/SubNav";
import SubNavRealmHomeLink from "org/forgerock/openam/ui/admin/views/realms/SubNavRealmHomeLink";
import Tree from "./tree/Tree";
import { TREE_PADDING } from "./tree/TreePadding";

const calculateTreeContainerHeight = (isFullscreen) => {
    const NAVBAR_HEIGHT = 76;
    const FOOTER_HEIGHT = 80;
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

class EditTree extends Component {
    constructor (props) {
        super(props);
        this.state = { containerWidth: 0 };
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
            containerWidth: dimensions.width
        });
    }

    render () {
        const handleNodeDelete = () => this.props.onNodeDelete(this.props.selectedNode.id);
        const containerHeight = calculateTreeContainerHeight(this.props.isFullscreen);
        const { canvasHeight, canvasWidth } =
            calculateCanvasDimensions(this.props.measurements, containerHeight, this.state.containerWidth);

        if (this.props.isFetching) {
            return <Loading />;
        } else {
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
                                            "collapsed-right": !!this.props.selectedNodeSchema
                                        }) }
                                    >
                                        <EditTreeToolbar
                                            invertTooltipPlacement={ this.props.isFullscreen }
                                            isDeleteNodeEnabled={ !!this.props.selectedNode.id }
                                            onAutoLayout={ this.props.onAutoLayout }
                                            onFullscreenToggle={ this.props.onFullscreenToggle }
                                            onNodeDelete={ handleNodeDelete }
                                            onTreeSave={ this.props.onTreeSave }
                                        />
                                        <Measure onMeasure={ this.handleTreeMeasure }>
                                            <Tree
                                                canvasHeight={ canvasHeight }
                                                canvasWidth={ canvasWidth }
                                                containerHeight={ containerHeight }
                                                measurements={ this.props.measurements }
                                                nodes={ this.props.nodes }
                                                onNewConnection={ this.props.onNewConnection }
                                                onNewNodeCreate={ this.props.onNewNodeCreate }
                                                onNodeDeselect={ this.props.onNodeDeselect }
                                                onNodeDimensionsChange={ this.props.onNodeDimensionsChange }
                                                onNodeMove={ this.props.onNodeMove }
                                                onNodeSelect={ this.props.onNodeSelect }
                                                selectedNodeId={ this.props.selectedNode.id }
                                            />
                                        </Measure>
                                    </div>
                                    <EditTreeNodeProperties
                                        isExpanded={ !!this.props.selectedNodeSchema }
                                        nodeId={ this.props.selectedNode.id }
                                        nodeName={ get(this.props.nodes[this.props.selectedNode.id], "displayName") }
                                        nodeType={ this.props.selectedNode.type }
                                        onFieldChange={ this.props.onNodePropertiesFieldChange }
                                        onPropertiesChange={ this.props.onNodePropertiesChange }
                                        properties={ this.props.selectedNodeProperties }
                                        schema={ this.props.selectedNodeSchema }
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
    measurements: PropTypes.objectOf(PropTypes.object).isRequired,
    nodeTypes: PropTypes.objectOf(PropTypes.object).isRequired,
    nodes: PropTypes.objectOf(PropTypes.object).isRequired,
    onAutoLayout: PropTypes.func.isRequired,
    onFullscreenToggle: PropTypes.func.isRequired,
    onNewConnection: PropTypes.func.isRequired,
    onNewNodeCreate: PropTypes.func.isRequired,
    onNodeDelete: PropTypes.func.isRequired,
    onNodeDeselect: PropTypes.func.isRequired,
    onNodeDimensionsChange: PropTypes.func.isRequired,
    onNodeMove: PropTypes.func.isRequired,
    onNodePropertiesChange: PropTypes.func.isRequired,
    onNodePropertiesFieldChange: PropTypes.func.isRequired,
    onNodeSelect: PropTypes.func.isRequired,
    onTreeSave: PropTypes.func.isRequired,
    selectedNode: PropTypes.shape({
        id: PropTypes.string,
        type: PropTypes.string
    }),
    selectedNodeProperties: PropTypes.objectOf(PropTypes.any),
    selectedNodeSchema: PropTypes.objectOf(PropTypes.any)
};

export default DragDropContext(HTML5Backend)(EditTree);
