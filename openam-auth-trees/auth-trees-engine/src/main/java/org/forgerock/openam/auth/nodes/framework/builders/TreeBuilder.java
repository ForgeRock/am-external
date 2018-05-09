/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.nodes.framework.builders.BuilderConstants.DESTINATION;
import static org.forgerock.openam.auth.nodes.framework.builders.BuilderConstants.OUTCOME;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;

/**
 * A Tree builder. Gathers required config, such as nodes and connections between nodes and builds the Tree.
 */
public final class TreeBuilder {

    private static final String TREE_ID = "TREE";
    private static final String NODE_ID = "NODE";
    private final AnnotatedServiceRegistry serviceRegistry;

    private ServiceConfig parentConfig;
    private String name;
    private String entryNodeId;
    private List<NodeBuilder> nodeBuilders = new ArrayList<>();
    private ServiceConfig thisConfig;
    private Map<String, Map<String, String>> connections;
    private NodeRegistry nodeRegistry;

    private TreeBuilder(NodeRegistry nodeRegistry, AnnotatedServiceRegistry serviceRegistry,
                        ServiceConfig parentConfig) {
        this.parentConfig = parentConfig;
        this.serviceRegistry = serviceRegistry;
        this.connections = new HashMap<>();
        this.nodeRegistry = nodeRegistry;
    }

    /**
     * Creates a TreeBuilder. Take and holds onto service classes for building.
     *
     * @param nodeRegistry the node registry.
     * @param serviceRegistry the service registry.
     * @param parentConfig the service config.
     * @return a tree builder.
     */
    public static TreeBuilder builder(NodeRegistry nodeRegistry, AnnotatedServiceRegistry serviceRegistry,
                                      ServiceConfig parentConfig) {
        return new TreeBuilder(nodeRegistry, serviceRegistry, parentConfig);
    }

    /**
     * Sets the name of the tree.
     *
     * @param name the name.
     * @return this builder.
     */
    public TreeBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the entry node.
     *
     * @param entryNode the entry node.
     * @return this builder.
     */
    public TreeBuilder entryNode(NodeBuilder entryNode) {
        this.entryNodeId = entryNode.getId();
        return this;
    }

    /**
     * Builds the tree using the config set.
     *
     * @return the service config of the built tree.
     * @throws SMSException if there is a SMSException.
     * @throws SSOException if there is a SSOException.
     */
    public ServiceConfig build() throws SMSException, SSOException {
        parentConfig.addSubConfig(this.name, TREE_ID, 0, singletonMap("entryNodeId", asSet(this.entryNodeId)));
        thisConfig = parentConfig.getSubConfig(this.name);
        for (NodeBuilder nodeBuilder : nodeBuilders) {
            nodeBuilder.accept(this);
            nodeBuilder.build(serviceRegistry);
        }
        for (String key : connections.keySet()) {
            Map<String, String> outcomes = connections.get(key);
            for (String outcomeKey : outcomes.keySet()) {
                String destinationUUID = outcomes.get(outcomeKey);
                ServiceConfig config = thisConfig.getSubConfig(key);
                config.addSubConfig(outcomeKey, OUTCOME, 0, singletonMap(DESTINATION, asSet(destinationUUID)));
            }
        }


        return thisConfig;
    }

    /**
     * Adds a Node to the tree.
     *
     * @param nodeBuilder a builder representing the Node.
     * @return this builder.
     */
    public TreeBuilder add(NodeBuilder nodeBuilder) {
        this.nodeBuilders.add(nodeBuilder);
        connections.put(nodeBuilder.getId(), new HashMap<>());
        return this;
    }

    /**
     * Adds nodes to the tree.
     *
     * @param nodeBuilders the builder of each of the nodes.
     * @return this builder.
     */
    public TreeBuilder add(NodeBuilder... nodeBuilders) {
        for (NodeBuilder nodeBuilder : nodeBuilders) {
            this.nodeBuilders.add(nodeBuilder);
            connections.put(nodeBuilder.getId(), new HashMap<>());
        }
        return this;
    }

    /**
     * Connects the single outcome of a Node to a destination.
     *
     * @param nodeBuilder the builder representing a Node.
     * @param destination the destination, usually a Node ID.
     * @return this builder.
     */
    public TreeBuilder connect(NodeBuilder nodeBuilder, UUID destination) {
        connections.get(nodeBuilder.getId()).put(OUTCOME, destination.toString());
        return this;
    }

    /**
     * Connects the single outcome of a Node to a destination Node.
     *
     * @param nodeBuilder the builder representing a Node.
     * @param destination the builder representing a destination Node.
     * @return this builder.
     */
    public TreeBuilder connect(NodeBuilder nodeBuilder, NodeBuilder destination) {
        connections.get(nodeBuilder.getId()).put(OUTCOME, destination.getId());
        return this;
    }

    /**
     * Connects a specific outcome of a Node to a destination.
     *
     * @param nodeBuilder the builder representing a Node.
     * @param outcome the outcome of the Node.
     * @param destination the destination, usually a Node ID.
     * @return this builder.
     */
    public TreeBuilder connect(NodeBuilder nodeBuilder, String outcome, UUID destination) {
        connections.get(nodeBuilder.getId()).put(outcome, destination.toString());
        return this;
    }

    /**
     * Connects a specific outcome of a Node to a destination Node.
     *
     * @param nodeBuilder the builder representing a Node.
     * @param outcome the outcome.
     * @param destination the destination Node.
     * @return this builder.
     */
    public TreeBuilder connect(NodeBuilder nodeBuilder, String outcome, NodeBuilder destination) {
        connections.get(nodeBuilder.getId()).put(outcome, destination.getId());
        return this;
    }

    /**
     * Adds node config, using the node name and attributes for that node.
     *
     * @param name the nodes name.
     * @param attributes the attributes for that node.
     */
    void addNodeConfig(String name, Map<String, Set<String>> attributes) throws SSOException, SMSException {
        thisConfig.addSubConfig(name, NODE_ID, 0, attributes);
    }

    /**
     * Returns the name of the service of the supplied node class.
     * @param clazz the node class.
     * @return the name of the service.
     */
    String getNodeServiceName(Class<? extends Node> clazz) {
        return nodeRegistry.getNodeServiceName(clazz);
    }
}
