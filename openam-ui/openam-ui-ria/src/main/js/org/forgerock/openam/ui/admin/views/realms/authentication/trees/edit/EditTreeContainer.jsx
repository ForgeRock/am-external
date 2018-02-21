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
import { bindActionCreators } from "redux";
import { chain, difference, each, every, findKey, isEmpty, keys, map, omit, pluck, values } from "lodash";
import React, { Component, PropTypes } from "react";
import uuidv4 from "uuid";

import { addOrUpdate as propertiesToLocal } from "store/modules/local/authentication/trees/current/nodes/properties";
import {
    addOrUpdate as propertiesToRemote,
    remove as removePropertiesFromRemote
} from "store/modules/remote/authentication/trees/current/nodes/properties";
import { addOrUpdate as treeToLocal } from "store/modules/local/authentication/trees/list";
import { addOrUpdate as treeToRemote } from "store/modules/remote/authentication/trees/list";
import { addOrUpdateSchema as addNodeSchema } from "store/modules/remote/authentication/trees/nodeTypes/schema";
import { set as setNodeTypes } from "store/modules/remote/authentication/trees/nodeTypes/list";
import { update as updateTree, get as getTree }
    from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";
import { start, isStaticNodeType, FAILURE_NODE_TYPE, SUCCESS_NODE_TYPE, SUCCESS_NODE_ID, FAILURE_NODE_ID }
    from "store/modules/local/authentication/trees/current/nodes/static";
import {
    createOrUpdate as createOrUpdateNode,
    get as getNodeProperties,
    getAllTypes as getAllNodeTypes,
    getSchema as getNodeTypeSchema,
    getTemplate as getNodeTypeTemplate,
    listOutcomes as listNodeOutcomes,
    remove as deleteNode
} from "org/forgerock/openam/ui/admin/services/realm/authentication/NodeService";
import {
    addOrUpdateNode as addNodeToLocal,
    setNodes as nodesToLocal,
    setOutcomes as setNodeOutcomes,
    removeConnection as removeNodeConnection,
    addOrUpdateConnection as connectionToLocal
} from "store/modules/local/authentication/trees/current/tree";
import { removeNode, removeCurrentTree } from "store/modules/local/authentication/trees/current/index";
import { setNodes as nodesToRemote } from "store/modules/remote/authentication/trees/current/tree";
import { remove as removeSelected, set as setSelected }
    from "store/modules/local/authentication/trees/current/nodes/selected";
import { updateDimensions, updatePosition } from "store/modules/local/authentication/trees/current/nodes/measurements";
import autoLayout from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/index";
import connectWithStore from "components/redux/connectWithStore";
import EditTree from "./EditTree";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const containsNode = (tree, id) => {
    const connections = chain(tree.nodes)
        .values()
        .pluck("connections")
        .map((connections) => values(connections))
        .flatten()
        .value();

    return connections.indexOf(id) !== -1 || tree.entryNodeId === id;
};

class EditTreeContainer extends Component {
    constructor () {
        super();
        this.state = {
            isFetching: true,
            isFullscreen: false
        };
        this.handleAutoLayout = this.handleAutoLayout.bind(this);
        this.handleFullscreenToggle = this.handleFullscreenToggle.bind(this);
        this.handleTreeSave = this.handleTreeSave.bind(this);
        this.performAutoLayout = this.performAutoLayout.bind(this);
    }

    componentDidMount () {
        this.props.removeCurrentTree();
        const realm = this.props.router.params[0];

        getAllNodeTypes(realm).then((response) => {
            this.props.setNodeTypes(response.result);
        }, (reason) => Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER }));

        const treeId = this.props.router.params[1];
        getTree(realm, treeId).then((tree) => {
            this.props.updateTree.inLocal(tree);
            this.props.updateTree.inRemote(tree);

            const hasSuccessNode = containsNode(tree, SUCCESS_NODE_ID);
            const hasFailureNode = containsNode(tree, FAILURE_NODE_ID);

            this.props.setNodes.toLocal(tree.nodes, tree.entryNodeId, hasSuccessNode, hasFailureNode);
            this.props.setNodes.toRemote(tree.nodes);

            this.setState({ isFetching: false });
        }, (reason) => {
            Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER });
            this.setState({ isFetching: false });
        });
    }

    componentWillReceiveProps (nextProps) {
        const hasDimensions = (measurements) => {
            return !isEmpty(measurements) && every(measurements, ({ height, width }) => {
                return height > 0 && width > 0;
            });
        };

        const propsHasDimensions = hasDimensions(this.props.measurements);
        const nextPropsHasDimensions = hasDimensions(nextProps.measurements);

        if (!propsHasDimensions && nextPropsHasDimensions) {
            this.performAutoLayout(nextProps.measurements);
        }
    }

    performAutoLayout (measurements = this.props.measurements) {
        const newPositions = autoLayout(
            findKey(start(this.props.tree.entryNodeId)),
            this.props.localNodes,
            measurements
        );

        each(newPositions, ({ x, y }, id) => {
            this.props.handleNodeMove({ id, x, y });
        });
    }

    handleAutoLayout () {
        this.performAutoLayout(this.props.measurements);
    }

    handleFullscreenToggle () {
        this.setState({ isFullscreen: !this.state.isFullscreen });
    }

    handleTreeSave () {
        const realm = this.props.router.params[0];
        const nodeCreateOrUpdatePromises = map(this.props.localNodeProperties, (properties, nodeId) =>
            createOrUpdateNode(realm, properties, properties._type._id, nodeId).then((node) => {
                this.props.addNodeProperties.toRemote(node);
            }));

        Promise.all(nodeCreateOrUpdatePromises).then(() => {
            updateTree(realm, {
                ...this.props.tree,
                nodes: omit(this.props.localNodes, [
                    FAILURE_NODE_ID,
                    findKey(start()),
                    SUCCESS_NODE_ID
                ])
            }, this.props.tree._id).then((tree) => {
                const nodesToDelete = difference(keys(this.props.remoteNodes), keys(this.props.localNodes));
                const nodeDeletePromises = map(nodesToDelete, (nodeId) =>
                    deleteNode(realm, this.props.remoteNodes[nodeId].nodeType, nodeId).then(() => {
                        this.props.removeNodePropertiesFromRemote(nodeId);
                    }));

                this.props.updateTree.inRemote(tree);
                this.props.setNodes.toRemote(tree.nodes);

                Promise.all(nodeDeletePromises).then(() => {
                    Messages.messages.displayMessageFromConfig("changesSaved");
                }, (reason) => {
                    Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER });
                });
            }, (reason) => {
                Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER });
            });
        }, (reason) => {
            Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER });
        });
    }

    updateLocalEntryNodeId (entryNodeId) {
        this.props.updateTree.inLocal({
            ...this.props.tree,
            entryNodeId
        });
    }

    render () {
        const handleNodeDimensionsChange = (id, height, width) =>
            this.props.handleNodeDimensionsChange({ id, height, width });
        const handleNodeMove = (id, x, y) => this.props.handleNodeMove({ id, x, y });
        const handleNewConnection = (nodeID, outcomeID, destinationNodeId) => {
            this.props.addOrUpdateConnection({ [outcomeID]: destinationNodeId }, nodeID);
            if (nodeID === findKey(start())) {
                this.updateLocalEntryNodeId(destinationNodeId);
            }
        };
        const handleNodeDelete = (id) => {
            if (this.props.tree.entryNodeId === id) {
                this.updateLocalEntryNodeId(null);
            }
            this.props.removeNode(id);
        };
        const handleNodeSelect = (id, type) => {
            this.props.handleNodeSelect({ id, type });

            if (!isStaticNodeType(type) && !this.props.localNodeProperties[id]) {
                const realm = this.props.router.params[0];
                getNodeTypeSchema(realm, type).then((response) => this.props.addNodeSchema(response, type));

                const isNew = !this.props.remoteNodes[id];
                if (isNew) {
                    getNodeTypeTemplate(realm, type).then((response) => {
                        this.props.addNodeProperties.toLocal({
                            ...response,
                            _id: id,
                            _type: { _id: type }
                        });
                    });
                } else {
                    getNodeProperties(realm, type, id).then((response) => {
                        this.props.addNodeProperties.toRemote(response);
                        this.props.addNodeProperties.toLocal(response);
                    });
                }
            }
        };
        const handleNodePropertiesFieldChange = this.props.addNodeProperties.toLocal;
        const handleNodePropertiesChange = (id, type, properties) => {
            const realm = this.props.router.params[0];
            listNodeOutcomes(realm, properties, type).then((response) => {
                const currentOutcomes = this.props.localNodes[id]._outcomes;
                const currentOutcomeKeys = pluck(currentOutcomes, "id");
                const newOutcomeKeys = pluck(response, "id");

                this.props.setNodeOutcomes(response, id);
                this.props.removeConnection(difference(currentOutcomeKeys, newOutcomeKeys), id);
            }, (reason) => Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER }));
        };
        const handleNewNode = (newNode, position) => {
            let id;

            switch (newNode.nodeType) {
                case SUCCESS_NODE_TYPE: id = SUCCESS_NODE_ID; break;
                case FAILURE_NODE_TYPE: id = FAILURE_NODE_ID; break;
                default: id = uuidv4();
            }

            this.props.addNodeToLocal({ [id]: {
                ...newNode,
                _outcomes: []
            } });
            this.props.handleNodeMove({ id, ...position });
            if (!isStaticNodeType(newNode.nodeType)) {
                handleNodeSelect(id, newNode.nodeType, true);
                handleNodePropertiesChange(id, newNode.nodeType);
            }
        };

        return (
            <EditTree
                isFetching={ this.state.isFetching }
                isFullscreen={ this.state.isFullscreen }
                measurements={ this.props.measurements }
                nodeTypes={ this.props.nodeTypes }
                nodes={ this.props.localNodes }
                onAutoLayout={ this.handleAutoLayout }
                onFullscreenToggle={ this.handleFullscreenToggle }
                onNewConnection={ handleNewConnection }
                onNewNodeCreate={ handleNewNode }
                onNodeDelete={ handleNodeDelete }
                onNodeDeselect={ this.props.handleNodeDeselect }
                onNodeDimensionsChange={ handleNodeDimensionsChange }
                onNodeMove={ handleNodeMove }
                onNodePropertiesChange={ handleNodePropertiesChange }
                onNodePropertiesFieldChange={ handleNodePropertiesFieldChange }
                onNodeSelect={ handleNodeSelect }
                onTreeSave={ this.handleTreeSave }
                selectedNode={ this.props.selectedNode }
                selectedNodeProperties={ this.props.selectedNodeProperties }
                selectedNodeSchema={ this.props.selectedNodeSchema }
            />
        );
    }
}

EditTreeContainer.propTypes = {
    addNodeProperties: PropTypes.func.isRequired,
    addNodeSchema: PropTypes.func.isRequired,
    addNodeToLocal: PropTypes.func.isRequired,
    addOrUpdateConnection: PropTypes.func.isRequired,
    handleNodeDeselect: PropTypes.func.isRequired,
    handleNodeDimensionsChange: PropTypes.func.isRequired,
    handleNodeMove: PropTypes.func.isRequired,
    handleNodeSelect: PropTypes.func.isRequired,
    localNodeProperties: PropTypes.objectOf(PropTypes.shape({
        _type: PropTypes.shape({
            _id: PropTypes.string
        })
    })).isRequired,
    localNodes: PropTypes.objectOf(PropTypes.object).isRequired,
    measurements: PropTypes.objectOf(PropTypes.object).isRequired,
    nodeTypes: PropTypes.objectOf(PropTypes.object).isRequired,
    remoteNodes: PropTypes.objectOf(PropTypes.object).isRequired,
    removeConnection: PropTypes.func.isRequired,
    removeCurrentTree: PropTypes.func.isRequired,
    removeNode: PropTypes.func.isRequired,
    removeNodePropertiesFromRemote: PropTypes.func.isRequired,
    router: withRouterPropType,
    selectedNode: PropTypes.shape({
        id: PropTypes.string,
        type: PropTypes.string
    }),
    selectedNodeProperties: PropTypes.objectOf(PropTypes.any),
    selectedNodeSchema: PropTypes.objectOf(PropTypes.any),
    setNodeOutcomes: PropTypes.func.isRequired,
    setNodeTypes: PropTypes.func.isRequired,
    setNodes: PropTypes.objectOf(PropTypes.func.isRequired).isRequired,
    tree: PropTypes.shape({
        _id: PropTypes.string.isRequired,
        entryNodeId: PropTypes.string
    }),
    updateTree: PropTypes.func.isRequired
};

EditTreeContainer = connectWithStore(EditTreeContainer,
    (state, ownProps) => {
        const treeId = ownProps.router.params[1];
        const selectedNode = state.local.authentication.trees.current.nodes.selected;
        const localNodeProperties = state.local.authentication.trees.current.nodes.properties;

        return {
            localNodeProperties,
            localNodes: state.local.authentication.trees.current.tree,
            measurements: state.local.authentication.trees.current.nodes.measurements,
            nodeTypes: state.remote.authentication.trees.nodeTypes.list,
            remoteNodes: state.remote.authentication.trees.current.tree,
            selectedNode,
            selectedNodeProperties: localNodeProperties[selectedNode.id],
            selectedNodeSchema: state.remote.authentication.trees.nodeTypes.schema[selectedNode.type],
            tree: state.local.authentication.trees.list[treeId]
        };
    },
    (dispatch) => ({
        addNodeProperties: bindActionCreators({ toRemote: propertiesToRemote, toLocal: propertiesToLocal }, dispatch),
        addNodeSchema: bindActionCreators(addNodeSchema, dispatch),
        addNodeToLocal: bindActionCreators(addNodeToLocal, dispatch),
        addOrUpdateConnection: bindActionCreators(connectionToLocal, dispatch),
        handleNodeDeselect: bindActionCreators(removeSelected, dispatch),
        handleNodeDimensionsChange: bindActionCreators(updateDimensions, dispatch),
        handleNodeMove: bindActionCreators(updatePosition, dispatch),
        handleNodeSelect: bindActionCreators(setSelected, dispatch),
        removeConnection: bindActionCreators(removeNodeConnection, dispatch),
        removeCurrentTree: bindActionCreators(removeCurrentTree, dispatch),
        removeNode: bindActionCreators(removeNode, dispatch),
        removeNodePropertiesFromRemote: bindActionCreators(removePropertiesFromRemote, dispatch),
        setNodeOutcomes: bindActionCreators(setNodeOutcomes, dispatch),
        setNodes: bindActionCreators({ toRemote: nodesToRemote, toLocal: nodesToLocal }, dispatch),
        setNodeTypes: bindActionCreators(setNodeTypes, dispatch),
        updateTree: bindActionCreators({ inRemote: treeToRemote, inLocal: treeToLocal }, dispatch)
    })
);
EditTreeContainer = withRouter(EditTreeContainer);

export default EditTreeContainer;
