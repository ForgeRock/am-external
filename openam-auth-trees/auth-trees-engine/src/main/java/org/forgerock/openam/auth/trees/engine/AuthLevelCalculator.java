/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.nodes.framework.ModifyAuthLevelNode;

/**
 * A node visitor that calculates maximum auth tree authentication level.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class AuthLevelCalculator implements NodeVisitor<Optional<Integer>> {

    private final Optional<Integer> failureAuthLevel;

    /**
     * Default constructor for use from the outside looking in. For child trees the result of failures will not be
     * {@link Optional#empty()} because the failure can be wired up to success.
     */
    AuthLevelCalculator() {
        this(Optional.empty());
    }

    private AuthLevelCalculator(Optional<Integer> failureAuthLevel) {
        this.failureAuthLevel = failureAuthLevel;
    }

    @Override
    public Optional<Integer> visit(UUID nodeId, Optional<Class<? extends Node>> nodeType, Optional<String> displayName,
            Supplier<Node> node, Stream<Supplier<Optional<Integer>>> nextVisitings) throws NodeProcessException {
        if (nodeId.equals(TREE_NODE_FAILURE_ID)) {
            return failureAuthLevel;
        } else if (nodeId.equals(TREE_NODE_SUCCESS_ID)) {
            return Optional.of(0);
        }
        int thisNodeLevel = getThisNodeLevel(nodeType.get(), node);
        return nextVisitings
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Integer::compareTo)
                .map(max -> max + thisNodeLevel);
    }

    private int getThisNodeLevel(Class<? extends Node> nodeType, Supplier<Node> node) throws NodeProcessException {
        if (nodeType.equals(ModifyAuthLevelNode.class)) {
            return ((ModifyAuthLevelNode) node.get()).getAuthLevelIncrement();
        } else if (nodeType.equals(InnerTreeEvaluatorNode.class)) {
            AuthLevelCalculator childAuthLevelCalculator = new AuthLevelCalculator(Optional.of(0));
            return ((InnerTreeEvaluatorNode) node.get()).getTree().visitNodes(childAuthLevelCalculator).orElse(0);
        }
        return 0;
    }
}
