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

define([
    "lodash",
    "store/modules/local/config/realm/authentication/trees/current/nodes/static",
    "support/propTypesChecked"
], (_, staticNodeProperties, propTypesChecked) => {
    let module;

    const nodesReducer = (state = {}) => state;
    const treeReducer = (state = {}) => state;
    const PAGE_TYPE = staticNodeProperties.PAGE_NODE_TYPE;

    describe("store/modules/local/config/realm/authentication/trees/current/index", () => {
        beforeEach(() => {
            const injector =
                require("inject-loader!store/modules/local/config/realm/authentication/trees/current/index");

            module = injector({
                "./nodes/index": nodesReducer,
                "./tree": treeReducer
            });
            module = propTypesChecked.default(module);
        });

        describe("actions", () => {
            describe("#moveNodeToTree", () => {
                it("creates an action", () => {
                    expect(module.moveNodeToTree("node-id")).eql({
                        type: "local/config/realm/authentication/trees/current/MOVE_NODE_TO_TREE",
                        payload: "node-id"
                    });
                });
            });
            describe("#moveNodeToPage", () => {
                it("creates an action", () => {
                    expect(module.moveNodeToPage({ nodeId: "node-id", pageId: "page-id" })).eql({
                        type: "local/config/realm/authentication/trees/current/MOVE_NODE_TO_PAGE",
                        payload: {
                            nodeId: "node-id",
                            pageId: "page-id"
                        }
                    });
                });
            });
            describe("#removeCurrentTree", () => {
                it("creates an action", () => {
                    expect(module.removeCurrentTree()).eql({
                        type: "local/config/realm/authentication/trees/current/REMOVE_CURRENT_TREE"
                    });
                });
            });
            describe("#removeNode", () => {
                it("creates an action", () => {
                    expect(module.removeNode("node-id")).eql({
                        type: "local/config/realm/authentication/trees/current/REMOVE_NODE",
                        payload: "node-id"
                    });
                });
            });
            describe("#setDisplayName", () => {
                it("creates an action", () => {
                    expect(module.setDisplayName({ displayName: "Node name", id: "node-id" })).eql({
                        type: "local/config/realm/authentication/trees/current/SET_DISPLAY_NAME",
                        payload: {
                            displayName: "Node name",
                            id: "node-id"
                        }
                    });
                });
            });
        });

        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({
                    nodes: {},
                    tree: {}
                });
            });

            const dimensions = () => ({
                height: 50,
                width: 150,
                x: 250,
                y: 100
            });
            const nodeProperties = (id, type = "MyNode", extraProperties = {}) => _.assign({
                _id: id,
                _type: { _id: type }
            }, extraProperties);

            const state = {
                nodes: {
                    measurements: {
                        node1: _.assign(dimensions(), { id: "node1" }),
                        node2: _.assign(dimensions(), { id: "node1" }),
                        node3: _.assign(dimensions(), { id: "node1" }),
                        pageNode: _.assign(dimensions(), { id: "pageNode" })
                    },
                    pages: {
                        childnodes: {
                            childNode1: "pageNode",
                            childNode2: "pageNode",
                            childNode3: "pageNode"
                        },
                        positions: {
                            pageNode: dimensions()
                        }
                    },
                    properties: {
                        childNode1: nodeProperties("childNode1"),
                        childNode2: nodeProperties("childNode2"),
                        childNode3: nodeProperties("childNode3"),
                        node1: nodeProperties("node1"),
                        node2: nodeProperties("node2"),
                        node3: nodeProperties("node3"),
                        pageNode: nodeProperties("pageNode", PAGE_TYPE, {
                            nodes: [{
                                _id: "childNode1",
                                nodeType: "MyNode",
                                displayName: "Child Node 1"
                            }, {
                                _id: "childNode2",
                                nodeType: "MyNode",
                                displayName: "Child Node 2"
                            }, {
                                _id: "childNode3",
                                nodeType: "MyNode",
                                displayName: "Child Node 3"
                            }]
                        })
                    },
                    selected: {}
                },
                tree: {
                    nodes: {
                        node1: {
                            connections: {
                                "outcome1": "node2"
                            },
                            displayName: "Node 1",
                            nodeType: "MyNode"
                        },
                        node2: {
                            connections: {
                                "outcome1": "node3"
                            },
                            displayName: "Node 2",
                            nodeType: "MyNode"
                        },
                        node3: {
                            connections: {
                                "outcome1": "node1",
                                "outcome2": "pageNode"
                            },
                            displayName: "Node 3",
                            nodeType: "MyNode"
                        },
                        pageNode: {
                            connections: {
                                "outcome1": "node1",
                                "outcome2":  "node2"
                            },
                            displayName: "Page Node",
                            nodeType: PAGE_TYPE
                        }
                    }
                }
            };

            let newState;
            describe("#moveNodeToPage", () => {
                describe("moving a node that is in the same page", () => {
                    it("leaves the state unchanged", () => {
                        const action = module.moveNodeToPage({ nodeId: "childNode1", pageId: "pageNode" });
                        expect(module.default(state, action)).eql(state);
                    });
                });
                describe("moving a node that is in another page", () => {
                    beforeEach(() => {
                        const oldState = _.cloneDeep(state);
                        oldState.nodes.properties.pageNode2 = nodeProperties("pageNode2", PAGE_TYPE, { nodes: [] });
                        const action = module.moveNodeToPage({ nodeId: "childNode1", pageId: "pageNode2" });
                        newState = module.default(oldState, action);
                    });
                    it("removes the node from the original page", () => {
                        expect(newState.nodes.properties.pageNode.nodes).eql([{
                            _id: "childNode2",
                            nodeType: "MyNode",
                            displayName: "Child Node 2"
                        }, {
                            _id: "childNode3",
                            nodeType: "MyNode",
                            displayName: "Child Node 3"
                        }]);
                    });
                    it("updates the childnodes entry", () => {
                        expect(newState.nodes.pages.childnodes.childNode1).eql("pageNode2");
                    });
                    it("adds the node as the last in the new page", () => {
                        expect(newState.nodes.properties.pageNode2.nodes).eql([{
                            _id: "childNode1",
                            nodeType: "MyNode",
                            displayName: "Child Node 1"
                        }]);
                    });
                    it("leaves the other node properties unchanged", () => {
                        expect(_.omit(newState.nodes.properties, ["pageNode", "pageNode2"]))
                            .eql(_.omit(state.nodes.properties, ["pageNode", "pageNode2"]));
                    });
                });
                describe("moving a node that is in a tree", () => {
                    beforeEach(() => {
                        const action = module.moveNodeToPage({ nodeId: "node1", pageId: "pageNode" });
                        newState = module.default(state, action);
                    });
                    it("removes the node from the tree", () => {
                        expect(newState.tree.nodes).not.have.property("node1");
                    });
                    it("removes the node's measurements", () => {
                        expect(newState.nodes.measurements).not.have.property("node1");
                    });
                    it("leaves the node's properties untouched", () => {
                        expect(newState.nodes.properties.node1).eql(state.nodes.properties.node1);
                    });
                    it("adds the node to the childnodes", () => {
                        expect(newState.nodes.pages.childnodes.node1).eql("pageNode");
                    });
                    it("adds the node as the last in the page", () => {
                        expect(newState.nodes.properties.pageNode.nodes).eql([{
                            _id: "childNode1",
                            nodeType: "MyNode",
                            displayName: "Child Node 1"
                        }, {
                            _id: "childNode2",
                            nodeType: "MyNode",
                            displayName: "Child Node 2"
                        }, {
                            _id: "childNode3",
                            nodeType: "MyNode",
                            displayName: "Child Node 3"
                        }, {
                            _id: "node1",
                            nodeType: "MyNode",
                            displayName: "Node 1"
                        }]);
                    });
                });
            });
            describe("#moveNodeToTree", () => {
                describe("moving a node that is in a page", () => {
                    beforeEach(() => {
                        newState = module.default(state, module.moveNodeToTree("childNode1"));
                    });
                    it("removes the node from the page", () => {
                        expect(newState.nodes.properties.pageNode.nodes).eql([{
                            _id: "childNode2",
                            nodeType: "MyNode",
                            displayName: "Child Node 2"
                        }, {
                            _id: "childNode3",
                            nodeType: "MyNode",
                            displayName: "Child Node 3"
                        }]);
                    });
                    it("removes the node from the childnodes", () => {
                        expect(newState.nodes.pages.childnodes).to.not.have.property("childNode1");
                    });
                    it("adds the node to the tree", () => {
                        expect(newState.tree.nodes.childNode1).eql({
                            _outcomes: {},
                            connections: {},
                            displayName: "Child Node 1",
                            nodeType: "MyNode"
                        });
                    });
                    it("leaves the node's properties unchanged", () => {
                        expect(newState.nodes.properties.childNode1).eql(state.nodes.properties.childNode1);
                    });
                });
                describe("moving a node that is not in a page", () => {
                    it("leaves the state unchanged", () => {
                        expect(module.default(state, module.moveNodeToTree("node1"))).eql(state);
                    });
                });
            });
            describe("#removeNode", () => {
                describe("when the node is a top-level node in the tree", () => {
                    beforeEach(() => {
                        newState = module.default(state, module.removeNode("node1"));
                    });
                    it("removes the node from the tree and any connections to the deleted node", () => {
                        expect(newState.tree).eql({
                            nodes: {
                                node2: {
                                    connections: {
                                        "outcome1": "node3"
                                    },
                                    displayName: "Node 2",
                                    nodeType: "MyNode"
                                },
                                node3: {
                                    connections: {
                                        "outcome2": "pageNode"
                                    },
                                    displayName: "Node 3",
                                    nodeType: "MyNode"
                                },
                                pageNode: {
                                    connections: {
                                        "outcome2":  "node2"
                                    },
                                    displayName: "Page Node",
                                    nodeType: PAGE_TYPE
                                }
                            }
                        });
                    });
                    it("removes the node properties", () => {
                        expect(newState.nodes.properties).eql(_.omit(state.nodes.properties, "node1"));
                    });
                    it("removes the node measurements", () => {
                        expect(newState.nodes.measurements).eql(_.omit(state.nodes.measurements, "node1"));
                    });
                    it("clears the selection if the deleted node was selected", () => {
                        newState = module.default({
                            nodes: {
                                measurements: {},
                                pages: { childnodes: {}, positions: {} },
                                properties: {},
                                selected: {
                                    id: "node1"
                                }
                            },
                            tree: {}
                        }, module.removeNode("node1"));
                        expect(newState.nodes.selected).eql({});
                    });
                    it("preserves the selection if the deleted node was not selected", () => {
                        newState = module.default({
                            nodes: {
                                measurements: {},
                                pages: { childnodes: {}, positions: {} },
                                properties: {},
                                selected: {
                                    id: "node2"
                                }
                            },
                            tree: {}
                        }, module.removeNode("node1"));
                        expect(newState.nodes.selected).eql({
                            id: "node2"
                        });
                    });
                });
                describe("when the node is in a page", () => {
                    beforeEach(() => {
                        newState = module.default(state, module.removeNode("childNode1"));
                    });
                    it("removes the node from the page", () => {
                        expect(newState.nodes.properties.pageNode.nodes).eql([{
                            _id: "childNode2",
                            nodeType: "MyNode",
                            displayName: "Child Node 2"
                        }, {
                            _id: "childNode3",
                            nodeType: "MyNode",
                            displayName: "Child Node 3"
                        }]);
                    });
                    it("removes the node's properties", () => {
                        expect(newState.nodes.properties).eql(_.assign(_.omit(state.nodes.properties, "childNode1"), {
                            pageNode: nodeProperties("pageNode", PAGE_TYPE, {
                                nodes: [{
                                    _id: "childNode2",
                                    nodeType: "MyNode",
                                    displayName: "Child Node 2"
                                }, {
                                    _id: "childNode3",
                                    nodeType: "MyNode",
                                    displayName: "Child Node 3"
                                }]
                            })
                        }));
                    });
                    it("removes the node from the childnodes object", () => {
                        expect(newState.nodes.pages.childnodes).eql(_.omit(state.nodes.pages.childnodes, "childNode1"));
                    });
                    it("leaves the rest of the state unchanged", () => {
                        expect(newState.nodes.measurements).eql(state.nodes.measurements);
                        expect(newState.nodes.pages.positions).eql(state.nodes.pages.positions);
                        expect(newState.tree).eql(state.tree);
                    });
                });
                describe("when the node is a page", () => {
                    beforeEach(() => {
                        newState = module.default(state, module.removeNode("pageNode"));
                    });
                    it("removes the node's properties", () => {
                        expect(newState.nodes.properties).to.not.have.property("pageNode");
                    });
                    it("removes the child nodes' properties", () => {
                        expect(newState.nodes.properties).to.not.have.property("childNode1");
                        expect(newState.nodes.properties).to.not.have.property("childNode2");
                        expect(newState.nodes.properties).to.not.have.property("childNode3");
                    });
                    it("removes all nodes that reference it from the childnodes object", () => {
                        expect(newState.nodes.pages.childnodes).eql({});
                    });
                    it("removes it from the page positions", () => {
                        expect(newState.nodes.pages.positions).eql({});
                    });
                    it("removes it from the measurements", () => {
                        expect(newState.nodes.measurements).to.not.have.property("pageNode");
                    });
                    it("removes it from the tree", () => {
                        expect(newState.tree.nodes).to.not.have.property("pageNode");
                    });
                });
            });
            describe("#removeCurrentTree", () => {
                it("replaces the state with a blank copy", () => {
                    expect(module.default(state, module.removeCurrentTree())).eql({
                        nodes: {
                            measurements: {},
                            pages: {
                                childnodes: {},
                                positions: {}
                            },
                            properties: {},
                            selected: {}
                        },
                        tree: {
                            nodes: {}
                        }
                    });
                });
            });
            describe("#setDisplayName", () => {
                describe("when the node is in a page", () => {
                    beforeEach(() => {
                        newState = module.default(state, module.setDisplayName({
                            displayName: "New name",
                            id: "childNode2"
                        }));
                    });
                    it("changes the display name", () => {
                        expect(newState.nodes.properties.pageNode.nodes[1].displayName).eql("New name");
                    });
                    it("makes no change to other nodes in the page", () => {
                        expect(newState.nodes.properties.pageNode.nodes[0])
                            .eql(state.nodes.properties.pageNode.nodes[0]);
                        expect(newState.nodes.properties.pageNode.nodes[2])
                            .eql(state.nodes.properties.pageNode.nodes[2]);
                    });
                    it("makes no other change", () => {
                        expect(newState.tree).eql(state.tree);
                        expect(_.omit(newState.nodes, "properties")).eql(_.omit(state.nodes, "properties"));
                        expect(_.omit(newState.nodes.properties, "pageNode"))
                            .eql(_.omit(state.nodes.properties, "pageNode"));
                    });
                });
                describe("when the node is in the tree", () => {
                    beforeEach(() => {
                        newState = module.default(state, module.setDisplayName({
                            displayName: "New name",
                            id: "node2"
                        }));
                    });
                    it("changes the display name", () => {
                        expect(newState.tree.nodes.node2.displayName).eql("New name");
                    });
                    it("makes no change to other nodes in the tree", () => {
                        expect(_.omit(newState.tree.nodes, "node2")).eql(_.omit(state.tree.nodes, "node2"));
                    });
                    it("makes no other change", () => {
                        expect(newState.nodes).eql(state.nodes);
                    });
                });
            });
        });
    });
});
