/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;

/**
 * A visitor for visiting nodes in an {@link AuthTree}.
 *
 * @param <R> The return type of the visitor.
 * @see AuthTree#visitNodes(NodeVisitor)
 * @see AuthTree#visitNodes(UUID, NodeVisitor)
 */
@FunctionalInterface
public interface NodeVisitor<R> {

    /**
     * The visit method for the visitor. This method will be called for the node that the visiting is starting from
     * (depending on which of the {@code AuthTree#visitNodes} methods was called). The {@code nextVisitings} suppliers
     * can then be invoked in order to carry on down the tree to the next nodes reachable from this one.
     * <p>
     *     Nodes will not be visited more than once - if a tree loops back to an earlier node, the node will not be
     *     visited again, and the {@code nextVisitings} will not include the outcomes that lead to the earlier node.
     * </p>
     *
     * @param nodeId The id of the node being visited.
     * @param nodeType An optional of the type of the node being visited, or empty if it's the success or failure node.
     * @param displayName An optional of the name of the node being visited, or empty if it's the success or failure
     *                   node.
     * @param node A supplier that can be called to obtain the node instance, which will be {@code null} if it's the
     *             success or failure node.
     * @param nextVisitings A stream of suppliers that can are called to invoke the visitor on the next nodes in the
     *                      tree that are reachable from the current node, which will be empty if it's the success or
     *                      failure node, or if all outcomes lead to an already visited node.
     * @return The return value of the visitor.
     * @throws NodeProcessException If there was an error processing the nodes.
     */
    R visit(UUID nodeId, Optional<Class<? extends Node>> nodeType, Optional<String> displayName,
            Supplier<Node> node, Stream<Supplier<R>> nextVisitings) throws NodeProcessException;

}
