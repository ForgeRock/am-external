/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import static io.vavr.collection.List.empty;
import static org.forgerock.util.LambdaExceptionUtils.rethrowFunction;
import static org.forgerock.util.LambdaExceptionUtils.rethrowSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.collect.ImmutableMultimap;
import org.forgerock.guava.common.collect.Multimap;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.Pair;
import org.forgerock.util.Reject;

/**
 * Immutable model of an authentication tree.
 *
 * To create a new AuthTree object use the static {@link AuthTree#builder(NodeFactory, NodeRegistry)}.
 *
 * @since AM 5.5.0
 */
public final class AuthTree {
    private final UUID entryNodeId;
    private final String name;
    private final Map<UUID, Node> nodes;
    private final Map<Pair<UUID, String>, Link> outcomeLinks;
    private final Multimap<UUID, Link> links;
    private final NodeFactory nodeFactory;
    private final NodeRegistry nodeRegistry;
    private final Realm realm;

    private AuthTree(Builder builder) {
        entryNodeId = builder.entryNodeId;
        this.nodeFactory = builder.nodeFactory;
        this.nodeRegistry = builder.nodeRegistry;
        this.realm = Reject.checkNotNull(builder.realm);
        name = Reject.checkNotNull(builder.name);
        ImmutableMap.Builder<UUID, Node> nodesBuilder = ImmutableMap.builder();
        for (Node node : builder.nodes) {
            nodesBuilder.put(node.id, node);
        }
        nodes = nodesBuilder.build();
        ImmutableMap.Builder<Pair<UUID, String>, Link> outcomeLinksBuilder = ImmutableMap.builder();
        ImmutableMultimap.Builder<UUID, Link> linksBuilder = ImmutableMultimap.builder();
        for (Link link : builder.links) {
            outcomeLinksBuilder.put(Pair.of(link.sourceNodeId, link.outcome), link);
            linksBuilder.put(link.sourceNodeId, link);
        }
        outcomeLinks = outcomeLinksBuilder.build();
        links = linksBuilder.build();
    }

    /**
     * Calculate maximum auth level which this tree can give from the entry node.
     *
     * @return Maximum auth level which this tree can give.
     * @throws NodeProcessException If the tree node can't be created for some SMS reason.
     */
    public Optional<Integer> getMaxAuthLevel() throws NodeProcessException {
        return getMaxAuthLevel(entryNodeId);
    }

    /**
     * Calculate maximum auth level which this tree can give from the specified node.
     *
     * @param nodeId the node to start from
     * @return Maximum auth level which this tree can give.
     * @throws NodeProcessException If the tree node can't be created for some SMS reason.
     */
    public Optional<Integer> getMaxAuthLevel(UUID nodeId) throws NodeProcessException {
        return visitNodes(nodeId, new AuthLevelCalculator());
    }

    /**
     * Visit the nodes in the tree starting at the entry node.
     * @param visitor The node visitor.
     * @param <R> The type of the return object.
     * @return The result of the visiting.
     * @throws NodeProcessException If the visiting results in an exception.
     */
    public <R> R visitNodes(NodeVisitor<R> visitor) throws NodeProcessException {
        return visitNodes(entryNodeId, visitor);
    }

    /**
     * Visit the nodes in the tree starting at the specified node. Each node will be visited only once, so a node will
     * not be revisited if an outcome path goes back to a previous node.
     *
     * @param start The node to start from.
     * @param visitor The node visitor.
     * @param <R> The type of the return object.
     * @return The result of the visiting.
     * @throws NodeProcessException If the visiting results in an exception.
     */
    public <R> R visitNodes(UUID start, NodeVisitor<R> visitor) throws NodeProcessException {
        return visitNodes(start, empty(), visitor);
    }

    private <R> R visitNodes(UUID nodeId, io.vavr.collection.List<UUID> visited, NodeVisitor<R> visitor)
            throws NodeProcessException {
        Optional<Node> node = Optional.ofNullable(nodes.get(nodeId));
        Optional<String> type = node.map(n -> n.type);
        return visitor.visit(nodeId,
                type.map(nodeRegistry::getNodeType), node.map(n -> n.displayName),
                type.map(rethrowFunction(t -> nodeSupplier(t, nodeId))).orElse(() -> null),
                node.isPresent() ? links.get(nodeId).stream()
                        .map(link -> link.destinationNodeId)
                        .filter(nextNodeId -> !visited.exists(nextNodeId::equals))
                        .map(nodeIdToVisitSupplier(nodeId, visited, visitor)) : Stream.empty());
    }

    private Supplier<org.forgerock.openam.auth.node.api.Node> nodeSupplier(String type, UUID nodeId)
            throws NodeProcessException {
        return rethrowSupplier(() -> nodeFactory.createNode(type, nodeId, realm, this));
    }

    private <R> Function<UUID, Supplier<R>> nodeIdToVisitSupplier(UUID from, io.vavr.collection.List<UUID> visited,
            NodeVisitor<R> visitor) throws NodeProcessException {
        return rethrowFunction(nodeId -> createNodeVisitor(nodeId, visited.prepend(from), visitor));
    }

    private <R> Supplier<R> createNodeVisitor(UUID nodeId, io.vavr.collection.List<UUID> visited,
            NodeVisitor<R> visitor) throws NodeProcessException {
        return rethrowSupplier(() -> visitNodes(nodeId, visited, visitor));
    }

    /**
     * Returns the name of the tree.
     * @return tree name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Creates a new builder for creating a new authentication tree.
     * @param nodeFactory The node factory.
     * @param nodeRegistry The registry of node services.
     * @return Builder a builder.
     */
    public static Builder builder(NodeFactory nodeFactory, NodeRegistry nodeRegistry) {
        return new Builder(nodeFactory, nodeRegistry);
    }

    /**
     * Check if the tree contains the specified node.
     * @param nodeId The node to check for.
     * @return Whether the node is contained in the tree.
     */
    public boolean containsNode(UUID nodeId) {
        return nodes.containsKey(nodeId);
    }

    /**
     * Builder for creating new {@link AuthTree} instances.
     */
    public static final class Builder {
        private final NodeFactory nodeFactory;
        private UUID entryNodeId;
        private final List<Node> nodes = new ArrayList<>();
        private final List<Link> links = new ArrayList<>();
        private String name;
        private Realm realm;
        private final NodeRegistry nodeRegistry;

        private Builder(NodeFactory nodeFactory, NodeRegistry nodeRegistry) {
            this.nodeFactory = nodeFactory;
            this.nodeRegistry = nodeRegistry;
        }

        /**
         * Sets the ID of the entry node of the tree.
         * @param entryNodeId ID of the fist node to process.
         * @return This builder instance
         */
        public Builder entryNodeId(UUID entryNodeId) {
            Reject.ifNull(entryNodeId);
            this.entryNodeId = entryNodeId;
            return this;
        }

        /**
         * Sets the name of the tree.
         * @param name the name.
         * @return This builder instance.
         */
        public Builder name(String name) {
            Reject.ifNull(name);
            this.name = name;
            return this;
        }

        /**
         * Sets the realm of the tree.
         * @param realm the realm.
         * @return This builder instance.
         */
        public Builder realm(Realm realm) {
            Reject.ifNull(realm);
            this.realm = realm;
            return this;
        }

        /**
         * Adds a node to the tree with its connections to other nodes.
         * @param id ID of the node
         * @param type Node type
         * @param displayName Human readable node name
         * @return This builder instance
         */
        public ConnectionBuilder node(UUID id, String type, String displayName) {
            Reject.ifNull(id, type, displayName);
            nodes.add(new Node(id, type, displayName));
            return new ConnectionBuilder(this, id);
        }

        /**
         * Builds a new {@link AuthTree} instance.
         * @return a new AuthTree instance
         */
        public AuthTree build() {
            Reject.ifNull(entryNodeId);
            return new AuthTree(this);
        }
    }

    /**
     * Builder for creating connections between two nodes in the tree.
     */
    public static final class ConnectionBuilder {
        private final Builder builder;
        private final UUID sourceNodeId;

        private ConnectionBuilder(Builder builder, UUID sourceNodeId) {
            this.builder = builder;
            this.sourceNodeId = sourceNodeId;
        }

        /**
         * Connects this node to another node.
         *
         * @param outcome the outcome identifier
         * @param destinationNodeId the id of the destination node
         * @return This builder instance
         */
        public ConnectionBuilder connect(String outcome, UUID destinationNodeId) {
            Reject.ifNull(outcome, destinationNodeId);
            builder.links.add(new Link(sourceNodeId, destinationNodeId, outcome));
            return this;
        }

        /**
         * Complete the connection.
         * @return The parent {@link Builder}.
         */
        public Builder done() {
            return builder;
        }
    }

    /**
     * Returns the starting node's ID.
     * @return the ID of the entry node.
     */
    public UUID getEntryNodeId() {
        return entryNodeId;
    }

    /**
     * Returns the node type of the node with the given id.
     * @param id the ID of the node.
     * @return the type of the given node.
     */
    public String getNodeTypeForId(UUID id) {
        return nodes.get(id).type;
    }

    /**
     * Returns the node display name of the node with the given id.
     * @param id the ID of the node.
     * @return the display name of the given node.
     */
    public String getDisplayNameForId(UUID id) {
        return nodes.get(id).displayName;
    }

    /**
     * Returns the ID of the next node to process given the ID and outcome of a node in the tree.
     * @param id the ID of the current node.
     * @param outcome the outcome of the current node.
     * @return the ID of the next node.
     */
    public UUID getNextNodeId(UUID id, String outcome) {
        return outcomeLinks.get(Pair.of(id, outcome)).destinationNodeId;
    }

    /**
     * Returns the realm for this tree.
     *
     * @return The {@link Realm} for this tree.
     */
    public Realm getRealm() {
        return realm;
    }

    private static final class Node {
        final UUID id;
        final String type;
        final String displayName;

        private Node(UUID id, String type, String displayName) {
            this.id = id;
            this.type = type;
            this.displayName = displayName;
        }
    }

    private static final class Link {
        final UUID sourceNodeId;
        final UUID destinationNodeId;
        final String outcome;

        private Link(UUID sourceNodeId, UUID destinationNodeId, String outcome) {
            this.sourceNodeId = sourceNodeId;
            this.destinationNodeId = destinationNodeId;
            this.outcome = outcome;
        }
    }
}
