/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework.validators;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.auth.trees.engine.NodeVisitor;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * A validator to check that a node in a tree will not result in a loop.
 */
public class NodeConfigValidator implements ServiceConfigValidator {

    private final NodeFactory nodeFactory;
    private final NodeRegistry nodeRegistry;
    private final AuthTreeService authTreeService;
    private final Debug debug;

    /**
     * Guice constructor.
     *  @param nodeFactory A node factory instance.
     * @param nodeRegistry The registry of node services.
     * @param authTreeService An auth tree service instance.
     * @param debug The auth debug instance.
     */
    @Inject
    public NodeConfigValidator(NodeFactory nodeFactory, NodeRegistry nodeRegistry,
            AuthTreeService authTreeService, @Named("amAuth") Debug debug) {
        this.nodeFactory = nodeFactory;
        this.nodeRegistry = nodeRegistry;
        this.authTreeService = authTreeService;
        this.debug = debug;
    }

    @Override
    public void validate(Realm realm, List<String> configPath,
            Map<String, Set<String>> attributes) throws ServiceConfigException, ServiceErrorException {
        String innerTreeNodeType = nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class);
        if (innerTreeNodeType.equals(CollectionHelper.getMapAttr(attributes, "nodeType"))) {
            String treeName = configPath.get(1);
            UUID nodeId = UUID.fromString(configPath.get(2));
            try {
                AuthTree outerTree = authTreeService.getTree(realm, treeName)
                        .orElseThrow(() -> new ServiceErrorException("Parent does not exist?! " + configPath));
                InnerTreeEvaluatorNode node = (InnerTreeEvaluatorNode) nodeFactory.createNode(innerTreeNodeType, nodeId,
                        realm, outerTree);
                AuthTree tree = node.getTree();
                if (tree.getName().equals(treeName) || tree.visitNodes(new IsTreeReachable(treeName))) {
                    throw new ServiceConfigException("Node would result in recursive loop: "
                            + CollectionHelper.getMapAttr(attributes, "displayName"));
                }
            } catch (NodeProcessException e) {
                debug.warning("Could not evaluate trees", e);
                throw new ServiceErrorException("Could not evaluate tree");
            }
        }
    }

    private static final class IsTreeReachable implements NodeVisitor<Boolean> {

        private final String treeName;

        private IsTreeReachable(String treeName) {
            this.treeName = treeName;
        }

        @Override
        public Boolean visit(UUID nodeId, Optional<Class<? extends Node>> nodeType, Optional<String> displayName,
                Supplier<Node> nodeSupplier, Stream<Supplier<Boolean>> visitings) throws NodeProcessException {
            if (InnerTreeEvaluatorNode.class.equals(nodeType.orElse(null))) {
                InnerTreeEvaluatorNode node = (InnerTreeEvaluatorNode) nodeSupplier.get();
                if (node.getTree().getName().equals(treeName) || node.getTree().visitNodes(this)) {
                    return true;
                }
            }
            return visitings.anyMatch(Supplier::get);
        }
    }


}
