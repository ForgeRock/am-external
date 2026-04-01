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
 * Copyright 2017-2026 Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import {
    chain,
    cloneDeep,
    difference,
    each,
    every,
    filter,
    findKey,
    get,
    isEmpty,
    keys,
    map,
    mapValues,
    omit,
    reject,
    sum,
    toPairs,
    union,
    values
} from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";
import uuidv4 from "uuid/v4";

import {
    addOrUpdate as propertiesToLocal
} from "store/modules/local/config/realm/authentication/trees/current/nodes/properties";
import {
    addOrUpdate as propertiesToRemote,
    remove as removePropertiesFromRemote
} from "store/modules/remote/config/realm/authentication/trees/current/nodes/properties";
import { addOrUpdate as treeToLocal } from "store/modules/local/config/realm/authentication/trees/list";
import { addOrUpdate as treeToRemote } from "store/modules/remote/config/realm/authentication/trees/list";
import {
    addOrUpdateSchema as addNodeSchema
} from "store/modules/remote/config/realm/authentication/trees/nodeTypes/schema";
import {
    update as updateTree,
    get as getTree
} from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";
import {
    failure,
    FAILURE_NODE_ID,
    FAILURE_NODE_TYPE,
    INNER_TREE_NODE_TYPE,
    PAGE_NODE_TYPE,
    isStaticNodeType,
    start,
    START_NODE_ID,
    success,
    SUCCESS_NODE_ID,
    SUCCESS_NODE_TYPE
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";
import {
    createOrUpdateNode,
    getNodeProperties,
    getAllTypes,
    getNodeTypeSchema,
    getNodeTypeDynamicSchema,
    getNodeTypeDynamicTemplate,
    getNodeTypeTemplate,
    listLatestTypes,
    listNodeOutcomes,
    deleteNode,
    getNodeType
} from "org/forgerock/openam/ui/admin/services/realm/authentication/NodeService";
import {
    addOrUpdateConnection as connectionToLocal,
    addOrUpdateNode as addNodeToLocal,
    propType as treePropType,
    removeConnection as removeNodeConnection,
    setNodes as nodesToLocal,
    setOutcomes as setNodeOutcomes,
    updateNodePosition
} from "store/modules/local/config/realm/authentication/trees/current/tree";
import {
    moveNodeToPage,
    moveNodeToTree,
    removeNode,
    removeCurrentTree,
    setDisplayName as setNodeDisplayName
} from "store/modules/local/config/realm/authentication/trees/current";
import {
    addOrUpdateNode as addNodeToRemote,
    setNodes as nodesToRemote
} from "store/modules/remote/config/realm/authentication/trees/current/tree";
import {
    remove as removeSelected,
    set as setSelected
} from "store/modules/local/config/realm/authentication/trees/current/nodes/selected";
import {
    updateDimensions
} from "store/modules/local/config/realm/authentication/trees/current/nodes/dimensions";
import {
    add as addNodeInPage,
    remove as removeNodeInPage
} from "store/modules/local/config/realm/authentication/trees/current/nodes/pages/childnodes";
import autoLayout from "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout";
import connectWithStore from "components/redux/connectWithStore";
import convertFormatToIsPassword from "components/form/schema/convertFormatToIsPassword";
import EditTree from "./EditTree";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import store from "store";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";
import {
    getVersionDataForNodeType,
    NODE_VERSIONING_PROPERTIES
} from "org/forgerock/openam/ui/admin/utils/nodeVersioningUtils";
import {
    SUCCESS_NODE,
    FAILURE_NODE,
    NODE_GROUPS,
    shouldValidateAsStandaloneNode
} from "org/forgerock/openam/ui/admin/utils/treeUtils";

const containsNode = (tree, id) => {
    const connections = chain(tree.nodes)
        .values()
        .map("connections")
        .map((connections) => values(connections))
        .flatten()
        .value();

    return connections.indexOf(id) !== -1 || tree.entryNodeId === id;
};

const containsOnlyNode = (tree, id) => {
    return tree.entryNodeId === id;
};

class EditTreeContainer extends Component {
    constructor () {
        super();
        this.state = {
            autoLayoutRequired: true,
            isFetching: true,
            isFullscreen: false,
            nodeTypes: [],
            groupedNodeTypes: {},
            nodeUpdate: {},
            selectNodeInProgress: false
        };
    }

    async componentDidMount () {
        const [realm, treeId] = this.props.router.params;

        this.props.removeCurrentTree();

        await this.fetchNodeTypes(realm);
        await this.fetchTreeData(realm, treeId);
    }

    componentDidUpdate () {
        if (!this.state.autoLayoutRequired) {
            return;
        }

        const hasPositioning = sum(map(this.props.localNodes, "x")) > 0;
        const hasAllDimensions = (dimensions) => {
            return !isEmpty(dimensions) && every(dimensions, ({ height, width }) => {
                return height > 0 && width > 0;
            });
        };

        if (hasAllDimensions(this.props.dimensions)) {
            if (!hasPositioning) {
                this.performAutoLayout();
            }
            this.setState({ autoLayoutRequired: false });  // eslint-disable-line
        }
    }

    fetchNodeTypes = async (realm) => {
        try {
            const { result: nodeTypeList } = await listLatestTypes(realm);
            const { result: nodeTypesWithVersions } = await getAllTypes(realm);

            const nodes = [FAILURE_NODE, SUCCESS_NODE, ...nodeTypeList];
            const nodeTypes = nodes.map((nodeType) => {
                const updatedNodeType = { ...nodeType };
                const matchingGroup = Object.values(NODE_GROUPS).find((groupName) =>
                    updatedNodeType.tags?.join().toLowerCase().includes(groupName.tag.toLowerCase()));
                updatedNodeType.icon = matchingGroup ? matchingGroup.icon : NODE_GROUPS.uncategorized.icon;
                const found = nodeTypesWithVersions.find((node) => node._id === updatedNodeType._id);
                updatedNodeType.versions = found && found.versions ? found.versions : [];
                return updatedNodeType;
            });

            const sortedNodeTypes = nodeTypes.sort((node) => node.name.toUpperCase());

            const nodeTypesWithinGroups = Object.entries(NODE_GROUPS).reduce((result, [key, { tag }]) => {
                const groupName = tag.toLowerCase();
                result[key] = sortedNodeTypes.reduce((result, nodeType) => {
                    if (nodeType.tags?.join().toLowerCase().includes(groupName)) {
                        result.push(nodeType);
                    }
                    return result;
                }, []);
                return result;
            }, {});

            const groupNames = Object.values(NODE_GROUPS).map((group) => group.tag);

            const uncategorizedNodes = sortedNodeTypes.filter((nodeType) => {
                if (!nodeType.tags?.length) {
                    return true;
                }
                const hasMatchingGroupName = groupNames.some((tag) => nodeType.tags.includes(tag));
                return !hasMatchingGroupName;
            });

            const groupedNodeTypes = mapValues({
                ...nodeTypesWithinGroups,
                uncategorized: [
                    ...nodeTypesWithinGroups["uncategorized"],
                    ...uncategorizedNodes
                ]
            }, (tagGroup) => tagGroup);

            this.setState({
                nodeTypes: sortedNodeTypes,
                groupedNodeTypes
            });
        } catch {
            Messages.addMessage({
                message: t("console.authentication.trees.edit.nodes.nodeTypes.errorLoading"), type: Messages.TYPE_DANGER
            });
        }
    };

    fetchTreeData = async (realm, treeId) => {
        try {
            const tree = await getTree(realm, treeId);

            this.props.updateTree.inLocal(tree);
            this.props.updateTree.inRemote(tree);

            Object.entries(tree.nodes).forEach(([nodeId, nodeConfig]) => {
                const { versions, isUpgradable } = getVersionDataForNodeType(this.state.nodeTypes, nodeConfig);
                tree.nodes[nodeId].versions = versions;
                tree.nodes[nodeId].isUpgradable = isUpgradable;
            });

            const { x, y } = get(tree.staticNodes, START_NODE_ID, {});
            let compositeNodes = {
                ...tree.nodes,
                ...start(tree.entryNodeId, x, y)
            };

            if (containsNode(tree, SUCCESS_NODE_ID) || containsOnlyNode(tree, FAILURE_NODE_ID)) {
                const { x, y } = get(tree.staticNodes, SUCCESS_NODE_ID, {});
                compositeNodes = {
                    ...compositeNodes,
                    ...success(x, y)
                };
            }

            if (containsNode(tree, FAILURE_NODE_ID)) {
                const { x, y } = get(tree.staticNodes, FAILURE_NODE_ID, {});
                compositeNodes = {
                    ...compositeNodes,
                    ...failure(x, y)
                };
            }

            // Local here is a reference to the set of nodes that are currently
            // being edited, including unsaved changes. Remote is the set of nodes
            // that were last fetched from, or saved to the server.
            this.props.setNodes.toLocal(compositeNodes);
            this.props.setNodes.toRemote(tree.nodes);

            await this.setAllNodeProperties(realm, tree.nodes);
        } catch {
            Messages.addMessage({
                message: t("console.authentication.trees.edit.errorLoading"), type: Messages.TYPE_DANGER
            });
        }

        this.setState({ isFetching: false });
    };

    performAutoLayout = (dimensions = this.props.dimensions) => {
        const newPositions = autoLayout(
            findKey(start(this.props.tree.entryNodeId)),
            omit(this.props.localNodes, keys(this.props.nodesInPages)),
            dimensions
        );

        each(newPositions, ({ x, y }, id) => {
            this.props.handleNodeMove({ x, y }, id);
        });
    };

    handleAutoLayout = () => {
        this.performAutoLayout(this.props.dimensions);
    };

    handleFullscreenToggle = () => {
        this.setState({ isFullscreen: !this.state.isFullscreen });
    };

    handleTreeSave = async () => {
        const realm = this.props.router.params[0];

        const isPageNode = { "_type": { "_id": "PageNode" } };
        const nodes = reject(this.props.localNodeProperties, isPageNode);
        const pageNodes = filter(this.props.localNodeProperties, isPageNode);

        const createOrUpdateFromNodes = (nodes) => map(nodes, async (properties) => {
            // Omit properties related to node versioning before save
            const nodeDetails = omit(properties, NODE_VERSIONING_PROPERTIES);

            // Do the same for any nodes that are inside a page node
            if (nodeDetails.nodes) {
                nodeDetails.nodes = nodeDetails.nodes.map((node) => {
                    const cleanNode = omit(node, ["version", "versions", "isUpgradable"]);
                    cleanNode.nodeVersion = node.version || node.nodeVersion;
                    return cleanNode;
                });
            }

            const node = await createOrUpdateNode(
                realm,
                nodeDetails,
                properties._type._id,
                properties._id,
                properties.version
            );
            this.props.addNodeProperties.toRemote(node);
        });
        const nodeCreateOrUpdatePromises = createOrUpdateFromNodes(nodes);
        await Promise.all(nodeCreateOrUpdatePromises);

        const pageNodeCreateOrUpdatePromises = createOrUpdateFromNodes(pageNodes);
        await Promise.all(pageNodeCreateOrUpdatePromises);

        const treeNodes = omit(
            this.props.localNodes,
            [FAILURE_NODE_ID, findKey(start()), SUCCESS_NODE_ID].concat(keys(this.props.nodesInPages))
        );

        let staticNodes = {
            startNode: { x: this.props.localNodes.startNode.x, y: this.props.localNodes.startNode.y }
        };

        if (this.props.localNodes[SUCCESS_NODE_ID]) {
            const { x, y } = this.props.localNodes[SUCCESS_NODE_ID];
            staticNodes = {
                ...staticNodes,
                [SUCCESS_NODE_ID]: { x, y }
            };
        }

        if (this.props.localNodes[FAILURE_NODE_ID]) {
            const { x, y } = this.props.localNodes[FAILURE_NODE_ID];
            staticNodes = {
                ...staticNodes,
                [FAILURE_NODE_ID]: { x, y }
            };
        }

        // The versions array and isUpgradable are both properties used in the
        // UI only and should be removed from nodes before saving the tree
        const treeNodesSaveData = cloneDeep(treeNodes);
        Object.entries(treeNodesSaveData).forEach(([nodeId, nodeDetails]) => {
            delete nodeDetails.versions;
            delete nodeDetails.isUpgradable;

            // If the node is not in a page node, we can also remove nodeVersion
            if (!this.props.nodesInPages[nodeId]) {
                delete nodeDetails.nodeVersion;
            }
        });

        const tree = await updateTree(
            realm, {
                ...this.props.tree,
                nodes: treeNodesSaveData,
                staticNodes
            },
            this.props.tree._id
        );
        const nodesToDelete = difference(
            keys(this.props.remoteNodes),
            union(
                keys(this.props.nodesInPages),
                keys(this.props.localNodes)
            )
        );

        const nodeDeletePromises = map(nodesToDelete, (nodeId) => {
            const nodeVersion = this.props.remoteNodes[nodeId].version
                ? this.props.remoteNodes[nodeId].version : this.props.remoteNodes[nodeId].nodeVersion;
            return deleteNode(realm, this.props.remoteNodes[nodeId].nodeType, nodeId, nodeVersion).then(() => {
                this.props.removeNodePropertiesFromRemote(nodeId);
            });
        });
        this.props.updateTree.inRemote(tree);
        this.props.setNodes.toRemote(tree.nodes);
        this.setAllNodeProperties(realm, tree.nodes);

        await Promise.all(nodeDeletePromises);
        Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
    };

    handleNodeDimensionsChange = (id, height, width) => {
        this.props.handleNodeDimensionsChange({ id, height, width });
    };

    handleNodeMove = async (id, x, y, mouseX, mouseY) => {
        const realm = this.props.router.params[0];
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
                }

                try {
                    const pageNode = this.props.localNodes[dropPageId];
                    const childNodeType = await getNodeType(realm, nodeType, nodeProperties.version);
                    if (pageNode._outcomes.length > 1 &&
                        !shouldValidateAsStandaloneNode(childNodeType, pageNode.version)) {
                        Messages.addMessage({
                            message: t("console.authentication.trees.edit.nodes.pages.alreadyComplete"),
                            type: Messages.TYPE_DANGER
                        });
                    } else {
                        this.props.moveNodeToPage({ nodeId: id, pageId: dropPageId });
                        this.loadOutcomes(
                            dropPageId,
                            PAGE_NODE_TYPE,
                            this.props.localNodeProperties[dropPageId],
                            this.props.localNodeProperties[dropPageId].version
                        );
                    }
                    return;
                } catch {
                    // if the call to getNodeType above fails an error will
                    // automatically be shown via crest api middleware
                }
            } else if (currentPage) {
                this.props.moveNodeToTree(id);
                this.loadOutcomes(
                    currentPage,
                    PAGE_NODE_TYPE,
                    this.props.localNodeProperties[currentPage],
                    this.props.localNodeProperties[currentPage].version
                );
                this.loadOutcomes(id, nodeType, nodeProperties, nodeProperties.version);
            }
        }
        this.props.handleNodeMove({ x, y }, id);
    };

    handleNewConnection = (nodeID, outcomeID, destinationNodeId) => {
        this.props.addOrUpdateConnection({ [outcomeID]: destinationNodeId }, nodeID);
        if (nodeID === findKey(start())) {
            this.updateLocalEntryNodeId(destinationNodeId);
        }
    };

    handleNodePropertiesChange = (id, type, properties, nodeVersion) => {
        if (this.props.nodesInPages[id]) {
            const pageId = this.props.nodesInPages[id];
            this.handleNodePropertiesChange(
                pageId,
                PAGE_NODE_TYPE,
                this.props.localNodeProperties[pageId],
                this.props.localNodeProperties[pageId].version
            );
        }
        this.reloadSchema(type, properties, nodeVersion);
        this.reloadTemplate(type, properties, nodeVersion);
        this.loadOutcomes(id, type, properties, nodeVersion);
    };

    handleNodeDelete = (id) => {
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
            this.handleNodePropertiesChange(pageId, PAGE_NODE_TYPE, newPageProperties, pageProperties.version);
        }
    };

    handleNodeSelect = (id, type, nodeVersion, name) => {
        // If we are already in the process of selecting a node or the node is
        // already selected, exit before repeating ourselves
        if (this.state.selectNodeInProgress || this.props.selectedNode.id === id) {
            return;
        }
        this.setState({ selectNodeInProgress: true });
        this.props.handleNodeSelect({ id, type, version: nodeVersion });

        const nodeWithoutProperties = !isStaticNodeType(type) && !this.props.localNodeProperties[id];
        const pageNodeWithoutSchema = type === PAGE_NODE_TYPE &&
            !(this.props.nodeTypeSchema[PAGE_NODE_TYPE] && this.props.nodeTypeSchema[PAGE_NODE_TYPE][nodeVersion]);
        if (pageNodeWithoutSchema || nodeWithoutProperties) {
            const realm = this.props.router.params[0];
            const isNew = !this.props.remoteNodes[id];
            if (isNew) {
                getNodeTypeSchema(realm, type, nodeVersion)
                    .then(convertFormatToIsPassword)
                    .then((response) => {
                        this.props.addNodeSchema(response, type, nodeVersion);
                    });
                getNodeTypeTemplate(realm, type, nodeVersion).then((response) => {
                    const { versions, isUpgradable } = getVersionDataForNodeType(
                        this.state.nodeTypes,
                        { nodeType: type, version: nodeVersion }
                    );
                    const properties = {
                        ...response,
                        _id: id,
                        _type: { _id: type, name },
                        nodeType: type,
                        version: nodeVersion,
                        versions,
                        isUpgradable
                    };
                    this.props.addNodeProperties.toLocal(properties);

                    this.handleNodePropertiesChange(id, type, properties, nodeVersion);
                    this.setState({ selectNodeInProgress: false });
                });
            } else {
                getNodeProperties(realm, type, id, nodeVersion).then((node) => {
                    getNodeTypeDynamicSchema(realm, node, type, nodeVersion)
                        .then(convertFormatToIsPassword)
                        .then((schema) => {
                            this.props.addNodeSchema(schema, type, nodeVersion);
                        });
                    // If this is a page node i.e. a node that contains other nodes,
                    // add version information to each node
                    if (node.nodes) {
                        node.nodes.forEach((nodeInPage, index) => {
                            node.nodes[index].version = nodeInPage.version || nodeInPage.nodeVersion;
                            const { versions, isUpgradable } = getVersionDataForNodeType(
                                this.state.nodeTypes,
                                nodeInPage
                            );
                            node.nodes[index].versions = versions;
                            node.nodes[index].isUpgradable = isUpgradable;
                        });
                    }

                    // Add version information to the current node
                    node.nodeType = node._type._id;
                    node.version = nodeVersion;
                    const { versions, isUpgradable } = getVersionDataForNodeType(this.state.nodeTypes, node);
                    node.versions = versions;
                    node.isUpgradable = isUpgradable;

                    // Update the redux stores with the latest information we
                    // just loaded
                    this.props.addNodeProperties.toRemote(node);
                    this.props.addNodeProperties.toLocal(node);
                    this.setState({ selectNodeInProgress: false });
                });
            }
        } else if (!isStaticNodeType(type)) { // eslint-disable-line
            const realm = this.props.router.params[0];
            getNodeTypeDynamicSchema(realm, this.props.localNodeProperties[id], type, nodeVersion)
                .then(convertFormatToIsPassword)
                .then((schema) => {
                    this.props.addNodeSchema(schema, type, nodeVersion);
                    this.setState({ selectNodeInProgress: false });
                });
        } else {
            // The node selected is a start node, failure node or success node in
            // which case hide the edit panel
            this.setState({ selectNodeInProgress: false });
        }
    };

    handleNodeDisplayNameChange = (id, displayName) => {
        this.props.setNodeDisplayName({ displayName, id });
    };

    handleNewNode = (newNode, position) => {
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
            this.props.handleNodeMove(position, id);
        }

        if (!isStaticNodeType(newNode.nodeType)) {
            this.handleNodeSelect(id, newNode.nodeType, newNode.version, newNode.displayName);
        }
        return id;
    };

    loadOutcomes = (id, type, properties, nodeVersion) => {
        const realm = this.props.router.params[0];
        const outcomeProps = properties && type === PAGE_NODE_TYPE ? {
            nodes: properties.nodes.map((node) => ({
                ...node,
                _properties: this.props.localNodeProperties[node._id]
            }))
        } : properties;
        listNodeOutcomes(realm, outcomeProps, type, nodeVersion).then((response) => {
            const currentOutcomes = this.props.localNodes[id]._outcomes;
            const currentOutcomeKeys = map(currentOutcomes, "id");
            const newOutcomeKeys = map(response, "id");

            this.props.setNodeOutcomes(response, id);
            this.props.removeConnection(difference(currentOutcomeKeys, newOutcomeKeys), id);
        });
    };

    reloadSchema = (type, properties, nodeVersion) => {
        const realm = this.props.router.params[0];
        getNodeTypeDynamicSchema(realm, properties, type, nodeVersion)
            .then(convertFormatToIsPassword)
            .then((response) => this.props.addNodeSchema(response, type, nodeVersion));
    };

    reloadTemplate = (type, properties, nodeVersion) => {
        const realm = this.props.router.params[0];
        getNodeTypeDynamicTemplate(realm, properties, type, nodeVersion)
            .then((response) => {
                const nodeProps = this.props.localNodeProperties[properties._id];
                Object.entries(response).forEach(([key, value]) => {
                    if (nodeProps[key] === null || nodeProps[key] === undefined) {
                        nodeProps[key] = value;
                    }
                });
            });
    };

    setAllNodeProperties (realm, nodes) {
        // Some of the nodes for rendering are in Page Nodes, not in the tree itself, so we need to load all the
        // PageNode properties too.
        return Promise.all(toPairs(nodes)
            .filter(([, node]) => node.nodeType === PAGE_NODE_TYPE)
            .map(([id, page]) => {
                return getNodeProperties(realm, page.nodeType, id, page.version).then((properties) => {
                    const { versions, isUpgradable } = getVersionDataForNodeType(this.state.nodeTypes, page);
                    properties.versions = versions;
                    properties.isUpgradable = isUpgradable;
                    properties.version = page.version;
                    properties.nodeType = page.nodeType;
                    this.props.addNodeProperties.toRemote(properties);
                    this.props.addNodeProperties.toLocal(properties);

                    properties.nodes.forEach((node) => {
                        // Nodes within a page node have the nodeVersion property,
                        // set that as version here to make it easier for the UI
                        // to track
                        node.version = node.nodeVersion;
                        const { versions, isUpgradable } = getVersionDataForNodeType(this.state.nodeTypes, node);
                        node.versions = versions;
                        node.isUpgradable = isUpgradable;
                        /**
                         * #addNodeInPage must be invoked before #addNode or the tree editor will attempt to
                         * render the node as a regular node when it has no outcomes, throwing an error.
                         */
                        this.props.addNodeInPage(node._id, id);
                        this.props.addNode.toLocal({ [node._id]: node });
                        this.props.addNode.toRemote({ [node._id]: node });
                    });
                });
            })
        );
    }

    updateLocalEntryNodeId (entryNodeId) {
        this.props.updateTree.inLocal({
            ...this.props.tree,
            entryNodeId
        });
    }

    /**
    * Sets the data used by the node update modals and decides which one to show
    * based on the current version of the node and intended update version
    */
    handleUpdateNodeVersionClicked = (node, toVersion) => {
        const nodeData = omit(node, NODE_VERSIONING_PROPERTIES);
        this.setState({
            nodeUpdate: {
                fromVersion: node.version,
                toVersion,
                nodeType: node.nodeType,
                nodeId: node._id,
                template: nodeData
            }
        });
    };

    /**
      * Updates a nodes version by upgrading its config
      */
    handleUpdateNode = async (nodeConfig) => {
        try {
            const realm = this.props.router.params[0];
            const config = cloneDeep(nodeConfig);

            // Get the type data for the new node version
            const type = await getNodeType(realm, config.nodeType, this.state.nodeUpdate.toVersion);
            type._id = config.nodeType;
            config._type = type;

            // Update the version data for the new node version
            config.version = this.state.nodeUpdate.toVersion;
            const { versions, isUpgradable } = getVersionDataForNodeType(this.state.nodeTypes, config);
            config.versions = versions;
            config.isUpgradable = isUpgradable;

            // Load the new node versions schema, template and outcomes
            this.handleNodePropertiesChange(nodeConfig._id, config.nodeType, config, this.state.nodeUpdate.toVersion);
            this.props.handleNodeSelect({
                id: nodeConfig._id,
                type: config.nodeType,
                version: this.state.nodeUpdate.toVersion
            });

            // Page nodes also contain a copy of the configuration of the nodes
            // they contain. If the node we are updating is inside a page node,
            // update the page nodes copy of that configuration too
            if (this.props.nodesInPages[nodeConfig._id]) {
                const pageId = this.props.nodesInPages[nodeConfig._id];
                const pageProperties = cloneDeep(this.props.localNodeProperties[pageId]);
                const nodeIndex = pageProperties.nodes.findIndex((n) => n._id === nodeConfig._id);
                if (nodeIndex !== -1) {
                    pageProperties.nodes[nodeIndex].version = config.version;
                    pageProperties.nodes[nodeIndex].nodeVersion = config.version;
                    pageProperties.nodes[nodeIndex].isUpgradable = config.isUpgradable;
                    pageProperties.nodes[nodeIndex].versions = config.versions;
                    this.props.addNodeProperties.toLocal(pageProperties);
                }
                // Load the updated page nodes schema, template and outcomes
                this.handleNodePropertiesChange(
                    pageId,
                    pageProperties._type._id,
                    pageProperties,
                    pageProperties.version
                );
            }

            // Update the local redux store with the latest information on this node.
            // Note: We do not update the remote redux store yet as we have not
            // saved this node update yet. That will happen when we save the tree.
            this.props.addNodeProperties.toLocal(config);

            this.props.addNode.toLocal({
                [nodeConfig._id]: {
                    ...this.props.localNodes[nodeConfig._id],
                    version: config.version,
                    nodeType: config.nodeType,
                    isUpgradable: config.isUpgradable,
                    versions: config.versions
                }
            });
        } catch {
            Messages.addMessage({
                message: t("console.authentication.trees.edit.nodes.updateNodeModal.errorUpdatingNodeVersion"),
                type: Messages.TYPE_DANGER
            });
        }
    };

    render () {
        const treeId = this.props.router.params[1];
        const handleNodePropertiesFieldChange = this.props.addNodeProperties.toLocal;

        return (
            <EditTree
                dimensions={ this.props.dimensions }
                groupedNodeTypes={ this.state.groupedNodeTypes }
                isFetching={ this.state.isFetching }
                isFullscreen={ this.state.isFullscreen }
                localNodeProperties={ this.props.localNodeProperties }
                nodes={ this.props.localNodes }
                nodesInPages={ this.props.nodesInPages }
                nodeTypes={ this.state.nodeTypes }
                nodeUpdate={ this.state.nodeUpdate }
                onAutoLayout={ this.handleAutoLayout }
                onFullscreenToggle={ this.handleFullscreenToggle }
                onNewConnection={ this.handleNewConnection }
                onNewNodeCreate={ this.handleNewNode }
                onNodeDelete={ this.handleNodeDelete }
                onNodeDeselect={ this.props.handleNodeDeselect }
                onNodeDimensionsChange={ this.handleNodeDimensionsChange }
                onNodeDisplayNameChange={ this.handleNodeDisplayNameChange }
                onNodeMove={ this.handleNodeMove }
                onNodePropertiesChange={ this.handleNodePropertiesChange }
                onNodePropertiesFieldChange={ handleNodePropertiesFieldChange }
                onNodeSelect={ this.handleNodeSelect }
                onTreeSave={ this.handleTreeSave }
                onUpdateNode={ this.handleUpdateNode }
                onUpdateNodeVersion={ this.handleUpdateNodeVersionClicked }
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
    addNode: PropTypes.objectOf(PropTypes.func.isRequired).isRequired,
    addNodeInPage: PropTypes.func.isRequired,
    addNodeProperties: PropTypes.objectOf(PropTypes.func.isRequired).isRequired,
    addNodeSchema: PropTypes.func.isRequired,
    addOrUpdateConnection: PropTypes.func.isRequired,
    dimensions: PropTypes.objectOf(PropTypes.object).isRequired,
    handleNodeDeselect: PropTypes.func.isRequired,
    handleNodeDimensionsChange: PropTypes.func.isRequired,
    handleNodeMove: PropTypes.func.isRequired,
    handleNodeSelect: PropTypes.func.isRequired,
    localNodeProperties: PropTypes.objectOf(PropTypes.shape({
        _type: PropTypes.shape({
            _id: PropTypes.string
        }),
        nodes: PropTypes.arrayOf(PropTypes.string.isRequired),
        version: PropTypes.string
    })).isRequired,
    localNodes: treePropType.nodes,
    moveNodeToPage: PropTypes.func.isRequired,
    moveNodeToTree: PropTypes.func.isRequired,
    nodeTypeSchema: PropTypes.objectOf(PropTypes.object).isRequired,
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
        type: PropTypes.string,
        version: PropTypes.string
    }),
    selectedNodeProperties: PropTypes.objectOf(PropTypes.any),
    selectedNodeSchema: PropTypes.objectOf(PropTypes.any),
    setNodeDisplayName: PropTypes.func.isRequired,
    setNodeOutcomes: PropTypes.func.isRequired,
    setNodes: PropTypes.objectOf(PropTypes.func.isRequired).isRequired,
    tree: PropTypes.shape({
        _id: PropTypes.string.isRequired,
        entryNodeId: PropTypes.string
    }),
    updateTree: PropTypes.objectOf(PropTypes.func.isRequired).isRequired
};

EditTreeContainer = connectWithStore(EditTreeContainer,
    (state, ownProps) => {
        const treeId = ownProps.router.params[1];
        const selectedNode = state.local.config.realm.authentication.trees.current.nodes.selected;
        const localNodeProperties = state.local.config.realm.authentication.trees.current.nodes.properties;

        return {
            localNodeProperties,
            localNodes: state.local.config.realm.authentication.trees.current.tree.nodes,
            dimensions: state.local.config.realm.authentication.trees.current.nodes.dimensions,
            nodeTypeSchema: state.remote.config.realm.authentication.trees.nodeTypes.schema,
            nodesInPages: state.local.config.realm.authentication.trees.current.nodes.pages.childnodes,
            remoteNodes: state.remote.config.realm.authentication.trees.current.tree,
            selectedNode,
            selectedNodeProperties: localNodeProperties[selectedNode.id],
            selectedNodeSchema: state.remote.config.realm.authentication.trees.nodeTypes.schema?.[selectedNode.type]?.[selectedNode.version], // eslint-disable-line
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
        handleNodeMove: bindActionCreators(updateNodePosition, dispatch),
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
        updateTree: bindActionCreators({ inRemote: treeToRemote, inLocal: treeToLocal }, dispatch)
    })
);
EditTreeContainer = withRouter(EditTreeContainer);

export default EditTreeContainer;
