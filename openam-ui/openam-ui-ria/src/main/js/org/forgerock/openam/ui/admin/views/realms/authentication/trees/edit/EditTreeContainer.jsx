/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import {
    chain, difference, each, every, filter, findKey, get, isEmpty, keys, map, mapValues, omit, pluck, pairs, reject,
    union, values
} from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";
import uuidv4 from "uuid/v4";

import { addOrUpdate as propertiesToLocal }
    from "store/modules/local/config/realm/authentication/trees/current/nodes/properties";
import {
    addOrUpdate as propertiesToRemote,
    remove as removePropertiesFromRemote
} from "store/modules/remote/config/realm/authentication/trees/current/nodes/properties";
import { addOrUpdate as treeToLocal } from "store/modules/local/config/realm/authentication/trees/list";
import { addOrUpdate as treeToRemote } from "store/modules/remote/config/realm/authentication/trees/list";
import { addOrUpdateSchema as addNodeSchema }
    from "store/modules/remote/config/realm/authentication/trees/nodeTypes/schema";
import { set as setNodeTypes } from "store/modules/remote/config/realm/authentication/trees/nodeTypes/list";
import {
    update as updateTree,
    get as getTree
} from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";
import {
    start,
    isStaticNodeType,
    FAILURE_NODE_TYPE,
    INNER_TREE_NODE_TYPE,
    PAGE_NODE_TYPE,
    START_NODE_ID,
    SUCCESS_NODE_TYPE,
    SUCCESS_NODE_ID,
    FAILURE_NODE_ID
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";
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
    addOrUpdateConnection as connectionToLocal,
    addOrUpdateNode as addNodeToLocal,
    propType as treePropType,
    removeConnection as removeNodeConnection,
    setNodes as nodesToLocal,
    setOutcomes as setNodeOutcomes
} from "store/modules/local/config/realm/authentication/trees/current/tree";
import {
    moveNodeToPage,
    moveNodeToTree,
    removeNode,
    removeCurrentTree,
    setDisplayName as setNodeDisplayName
} from "store/modules/local/config/realm/authentication/trees/current/index";
import {
    addOrUpdateNode as addNodeToRemote,
    setNodes as nodesToRemote
} from "store/modules/remote/config/realm/authentication/trees/current/tree";
import {
    remove as removeSelected,
    set as setSelected
} from "store/modules/local/config/realm/authentication/trees/current/nodes/selected";
import {
    updateDimensions,
    updatePosition
} from "store/modules/local/config/realm/authentication/trees/current/nodes/measurements";
import {
    add as addNodeInPage,
    remove as removeNodeInPage
} from "store/modules/local/config/realm/authentication/trees/current/nodes/pages/childnodes";
import autoLayout from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/index";
import connectWithStore from "components/redux/connectWithStore";
import EditTree from "./EditTree";
import Messages, { addMessage, TYPE_DANGER } from "org/forgerock/commons/ui/common/components/Messages";
import PromisePolyfill from "org/forgerock/openam/ui/common/util/Promise";
import store from "store/index";
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
            autoLayoutCompleted: false,
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
            return tree.nodes;
        }).then((nodes) => {
            // Some of the nodes for rendering are in pages, not in the tree itself, so we need to load all the PageNode
            // properties too.
            return PromisePolyfill.all(pairs(nodes)
                .filter(([, node]) => node.nodeType === PAGE_NODE_TYPE)
                .map(([id, page]) => {
                    return getNodeProperties(realm, page.nodeType, id).then((properties) => {
                        this.props.addNodeProperties.toRemote(properties);
                        this.props.addNodeProperties.toLocal(properties);
                        properties.nodes.forEach((node) => {
                            this.props.addNode.toLocal({ [node._id]: node });
                            this.props.addNode.toRemote({ [node._id]: node });
                            this.props.addNodeInPage(node._id, id);
                        });
                    });
                })
            );
        }).then(() => {
            this.setState({ isFetching: false });
        }, (reason) => {
            Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER });
            this.setState({ isFetching: false });
        });
    }

    componentWillReceiveProps (nextProps) {
        if (!this.state.autoLayoutCompleted) {
            const hasAllMeasurements = (measurements) => {
                return !isEmpty(measurements) && every(measurements, ({ height, width }) => {
                    return height > 0 && width > 0;
                });
            };
            const propsHasAllMeasurements = hasAllMeasurements(this.props.measurements);
            const nextPropsHasAllMeasurements = hasAllMeasurements(nextProps.measurements);

            if (!propsHasAllMeasurements && nextPropsHasAllMeasurements) {
                this.setState({ autoLayoutCompleted: true });
                this.performAutoLayout(nextProps.measurements);
            }
        }
    }

    performAutoLayout (measurements = this.props.measurements) {
        const newPositions = autoLayout(
            findKey(start(this.props.tree.entryNodeId)),
            omit(this.props.localNodes, keys(this.props.nodesInPages)),
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

    async handleTreeSave () {
        const realm = this.props.router.params[0];

        const isPageNode = { "_type": { "_id": "PageNode" } };
        const nodes = reject(this.props.localNodeProperties, isPageNode);
        const pageNodes = filter(this.props.localNodeProperties, isPageNode);

        try {
            const createOrUpdateFromNodes = (nodes) => map(nodes, async (properties) => {
                const node = await createOrUpdateNode(realm, properties, properties._type._id, properties._id);
                this.props.addNodeProperties.toRemote(node);
            });
            const nodeCreateOrUpdatePromises = createOrUpdateFromNodes(nodes);
            await Promise.all(nodeCreateOrUpdatePromises); // eslint-disable-line

            const pageNodeCreateOrUpdatePromises = createOrUpdateFromNodes(pageNodes);
            await Promise.all(pageNodeCreateOrUpdatePromises); // eslint-disable-line

            const tree = await updateTree(realm, {
                ...this.props.tree,
                nodes: omit(this.props.localNodes, [
                    FAILURE_NODE_ID,
                    findKey(start()),
                    SUCCESS_NODE_ID
                ].concat(keys(this.props.nodesInPages)))
            }, this.props.tree._id);

            const nodesToDelete = difference(keys(this.props.remoteNodes), union(keys(this.props.nodesInPages),
                keys(this.props.localNodes)));
            const nodeDeletePromises = map(nodesToDelete, (nodeId) =>
                deleteNode(realm, this.props.remoteNodes[nodeId].nodeType, nodeId).then(() => {
                    this.props.removeNodePropertiesFromRemote(nodeId);
                }));
            this.props.updateTree.inRemote(tree);
            this.props.setNodes.toRemote(tree.nodes);

            await Promise.all(nodeDeletePromises); // eslint-disable-line
            Messages.messages.displayMessageFromConfig("changesSaved");
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
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
        const loadOutcomes = (id, type, properties) => {
            const realm = this.props.router.params[0];
            const outcomeProps = properties && type === PAGE_NODE_TYPE ? {
                nodes: properties.nodes.map((node) => ({
                    ...node,
                    _properties: this.props.localNodeProperties[node._id]
                }))
            } : properties;
            listNodeOutcomes(realm, outcomeProps, type).then((response) => {
                const currentOutcomes = this.props.localNodes[id]._outcomes;
                const currentOutcomeKeys = pluck(currentOutcomes, "id");
                const newOutcomeKeys = pluck(response, "id");

                this.props.setNodeOutcomes(response, id);
                this.props.removeConnection(difference(currentOutcomeKeys, newOutcomeKeys), id);
            }, (reason) => Messages.addMessage({ response: reason, type: Messages.TYPE_DANGER }));
        };
        const handleNodeMove = (id, x, y, mouseX, mouseY) => {
            const nodeProperties = this.props.localNodeProperties[id];
            const nodeType = get(nodeProperties, "_type._id");
            const nodeCanBeDroppedInPage = id !== FAILURE_NODE_ID && id !== SUCCESS_NODE_ID && id !== START_NODE_ID &&
                nodeType !== INNER_TREE_NODE_TYPE && nodeType !== PAGE_NODE_TYPE;
            if (mouseX && mouseY && nodeCanBeDroppedInPage) {
                const state = store.getState();
                const dropZones = state.local.config.realm.authentication.trees.current.nodes.pages.positions;
                const dropPageId = findKey(dropZones, (dropZone) => {
                    return mouseX > dropZone.x && mouseX < dropZone.x + dropZone.width &&
                        mouseY > dropZone.y && mouseY < dropZone.y + dropZone.height;
                });
                const currentPage = this.props.nodesInPages[id];
                if (dropPageId) {
                    if (currentPage === dropPageId) {
                        return;
                    } else if (this.props.localNodes[dropPageId]._outcomes.length > 1) {
                        addMessage({
                            message: t("console.authentication.trees.edit.nodes.pages.alreadyComplete"),
                            type: TYPE_DANGER
                        });
                    } else {
                        this.props.moveNodeToPage({ nodeId: id, pageId: dropPageId });
                        loadOutcomes(dropPageId, PAGE_NODE_TYPE, this.props.localNodeProperties[dropPageId]);
                    }
                    return;
                } else if (currentPage) {
                    this.props.moveNodeToTree(id);
                    loadOutcomes(currentPage, PAGE_NODE_TYPE, this.props.localNodeProperties[currentPage]);
                    loadOutcomes(id, nodeType, nodeProperties);
                }
            }
            this.props.handleNodeMove({ id, x, y });
        };
        const handleNewConnection = (nodeID, outcomeID, destinationNodeId) => {
            this.props.addOrUpdateConnection({ [outcomeID]: destinationNodeId }, nodeID);
            if (nodeID === findKey(start())) {
                this.updateLocalEntryNodeId(destinationNodeId);
            }
        };
        const handleNodePropertiesChange = (id, type, properties) => {
            if (this.props.nodesInPages[id]) {
                const pageId = this.props.nodesInPages[id];
                handleNodePropertiesChange(pageId, PAGE_NODE_TYPE, this.props.localNodeProperties[pageId]);
            } else {
                loadOutcomes(id, type, properties);
            }
        };
        const handleNodeDelete = (id) => {
            if (this.props.tree.entryNodeId === id) {
                this.updateLocalEntryNodeId(null);
            }
            this.props.removeNode(id);
            if (this.props.nodesInPages[id]) {
                const pageId = this.props.nodesInPages[id];
                const pageProperties = this.props.localNodeProperties[pageId];
                const newPageProperties = {
                    ...pageProperties,
                    nodes: filter(pageProperties.nodes, (n) => n._id !== id)
                };
                this.props.removeNodeInPage(id);
                this.props.addNodeProperties.toLocal(newPageProperties);
                handleNodePropertiesChange(pageId, PAGE_NODE_TYPE, newPageProperties);
            }
        };
        const handleNodeSelect = (id, type, name) => {
            this.props.handleNodeSelect({ id, type });

            if (!isStaticNodeType(type) && !this.props.localNodeProperties[id]) {
                const realm = this.props.router.params[0];
                // See components/form/Form.jsx for more information.
                const transformPasswordFormat = (response) => {
                    response.properties = mapValues(response.properties, (property) => {
                        if (property.format && property.format === "password") {
                            delete property.format;
                            property._isPassword = true;
                        }
                        return property;
                    });
                    return response;
                };
                getNodeTypeSchema(realm, type)
                    .then(transformPasswordFormat)
                    .then((response) => this.props.addNodeSchema(response, type));

                const isNew = !this.props.remoteNodes[id];
                if (isNew) {
                    getNodeTypeTemplate(realm, type).then((response) => {
                        this.props.addNodeProperties.toLocal({
                            ...response,
                            _id: id,
                            _type: { _id: type, name }
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
        const handleNodeDisplayNameChange = (id, displayName) => {
            this.props.setNodeDisplayName({ displayName, id });
        };
        const handleNewNode = (newNode, position) => {
            let id;

            switch (newNode.nodeType) {
                case SUCCESS_NODE_TYPE: id = SUCCESS_NODE_ID; break;
                case FAILURE_NODE_TYPE: id = FAILURE_NODE_ID; break;
                default: id = uuidv4();
            }

            this.props.addNode.toLocal({ [id]: {
                ...newNode,
                _outcomes: []
            } });
            if (position) {
                this.props.handleNodeMove({ id, ...position });
            }
            if (!isStaticNodeType(newNode.nodeType)) {
                handleNodeSelect(id, newNode.nodeType, newNode.displayName);
                handleNodePropertiesChange(id, newNode.nodeType);
            }
            return id;
        };

        const treeId = this.props.router.params[1];

        return (
            <EditTree
                isFetching={ this.state.isFetching }
                isFullscreen={ this.state.isFullscreen }
                localNodeProperties={ this.props.localNodeProperties }
                measurements={ this.props.measurements }
                nodeTypes={ this.props.nodeTypes }
                nodes={ this.props.localNodes }
                nodesInPages={ this.props.nodesInPages }
                onAutoLayout={ this.handleAutoLayout }
                onFullscreenToggle={ this.handleFullscreenToggle }
                onNewConnection={ handleNewConnection }
                onNewNodeCreate={ handleNewNode }
                onNodeDelete={ handleNodeDelete }
                onNodeDeselect={ this.props.handleNodeDeselect }
                onNodeDimensionsChange={ handleNodeDimensionsChange }
                onNodeDisplayNameChange={ handleNodeDisplayNameChange }
                onNodeMove={ handleNodeMove }
                onNodePropertiesChange={ handleNodePropertiesChange }
                onNodePropertiesFieldChange={ handleNodePropertiesFieldChange }
                onNodeSelect={ handleNodeSelect }
                onTreeSave={ this.handleTreeSave }
                selectedNode={ this.props.selectedNode }
                selectedNodeIsNew={ !this.props.remoteNodes[this.props.selectedNode.id] }
                selectedNodeProperties={ this.props.selectedNodeProperties }
                selectedNodeSchema={ this.props.selectedNodeSchema }
                treeId={ treeId }
            />
        );
    }
}

EditTreeContainer.propTypes = {
    addNode: PropTypes.func.isRequired,
    addNodeInPage: PropTypes.func.isRequired,
    addNodeProperties: PropTypes.func.isRequired,
    addNodeSchema: PropTypes.func.isRequired,
    addOrUpdateConnection: PropTypes.func.isRequired,
    handleNodeDeselect: PropTypes.func.isRequired,
    handleNodeDimensionsChange: PropTypes.func.isRequired,
    handleNodeMove: PropTypes.func.isRequired,
    handleNodeSelect: PropTypes.func.isRequired,
    localNodeProperties: PropTypes.objectOf(PropTypes.shape({
        _type: PropTypes.shape({
            _id: PropTypes.string
        }),
        nodes: PropTypes.arrayOf(PropTypes.string.isRequired)
    })).isRequired,
    localNodes: treePropType.nodes,
    measurements: PropTypes.objectOf(PropTypes.object).isRequired,
    moveNodeToPage: PropTypes.func.isRequired,
    moveNodeToTree: PropTypes.func.isRequired,
    nodeTypes: PropTypes.objectOf(PropTypes.object).isRequired,
    nodesInPages: PropTypes.objectOf(PropTypes.string).isRequired,
    remoteNodes: PropTypes.objectOf(PropTypes.object).isRequired,
    removeConnection: PropTypes.func.isRequired,
    removeCurrentTree: PropTypes.func.isRequired,
    removeNode: PropTypes.func.isRequired,
    removeNodeInPage: PropTypes.func.isRequired,
    removeNodePropertiesFromRemote: PropTypes.func.isRequired,
    router: withRouterPropType,
    selectedNode: PropTypes.shape({
        id: PropTypes.string,
        type: PropTypes.string
    }),
    selectedNodeProperties: PropTypes.objectOf(PropTypes.any),
    selectedNodeSchema: PropTypes.objectOf(PropTypes.any),
    setNodeDisplayName: PropTypes.func.isRequired,
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
        const selectedNode = state.local.config.realm.authentication.trees.current.nodes.selected;
        const localNodeProperties = state.local.config.realm.authentication.trees.current.nodes.properties;

        return {
            localNodeProperties,
            localNodes: state.local.config.realm.authentication.trees.current.tree.nodes,
            measurements: state.local.config.realm.authentication.trees.current.nodes.measurements,
            nodeTypes: state.remote.config.realm.authentication.trees.nodeTypes.list,
            nodesInPages: state.local.config.realm.authentication.trees.current.nodes.pages.childnodes,
            remoteNodes: state.remote.config.realm.authentication.trees.current.tree,
            selectedNode,
            selectedNodeProperties: localNodeProperties[selectedNode.id],
            selectedNodeSchema: state.remote.config.realm.authentication.trees.nodeTypes.schema[selectedNode.type],
            tree: state.local.config.realm.authentication.trees.list[treeId]
        };
    },
    (dispatch) => ({
        addNodeInPage: bindActionCreators(addNodeInPage, dispatch),
        addNodeProperties: bindActionCreators({ toRemote: propertiesToRemote, toLocal: propertiesToLocal }, dispatch),
        addNodeSchema: bindActionCreators(addNodeSchema, dispatch),
        addNode: bindActionCreators({ toRemote: addNodeToRemote, toLocal: addNodeToLocal }, dispatch),
        addOrUpdateConnection: bindActionCreators(connectionToLocal, dispatch),
        handleNodeDeselect: bindActionCreators(removeSelected, dispatch),
        handleNodeDimensionsChange: bindActionCreators(updateDimensions, dispatch),
        handleNodeMove: bindActionCreators(updatePosition, dispatch),
        handleNodeSelect: bindActionCreators(setSelected, dispatch),
        moveNodeToPage: bindActionCreators(moveNodeToPage, dispatch),
        moveNodeToTree: bindActionCreators(moveNodeToTree, dispatch),
        removeConnection: bindActionCreators(removeNodeConnection, dispatch),
        removeCurrentTree: bindActionCreators(removeCurrentTree, dispatch),
        removeNode: bindActionCreators(removeNode, dispatch),
        removeNodeInPage: bindActionCreators(removeNodeInPage, dispatch),
        removeNodePropertiesFromRemote: bindActionCreators(removePropertiesFromRemote, dispatch),
        setNodeDisplayName: bindActionCreators(setNodeDisplayName, dispatch),
        setNodeOutcomes: bindActionCreators(setNodeOutcomes, dispatch),
        setNodes: bindActionCreators({ toRemote: nodesToRemote, toLocal: nodesToLocal }, dispatch),
        setNodeTypes: bindActionCreators(setNodeTypes, dispatch),
        updateTree: bindActionCreators({ inRemote: treeToRemote, inLocal: treeToLocal }, dispatch)
    })
);
EditTreeContainer = withRouter(EditTreeContainer);

export default EditTreeContainer;
