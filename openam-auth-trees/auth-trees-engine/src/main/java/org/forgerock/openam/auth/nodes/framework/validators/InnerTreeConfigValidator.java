/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
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
import org.forgerock.openam.auth.trees.engine.NodeVisitor;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.utils.CollectionUtils;

import com.sun.identity.shared.debug.Debug;

/**
 * Validate inner tree configuration does not contain a recursive loop.
 */
public class InnerTreeConfigValidator implements ServiceConfigValidator {
    private final AuthTreeService authTreeService;
    private final Debug debug;

    /**
     * DI Constructor.
     * @param authTreeService The auth tree service.
     * @param debug The auth debug instance.
     */
    @Inject
    public InnerTreeConfigValidator(AuthTreeService authTreeService, @Named("amAuth") Debug debug) {
        this.authTreeService = authTreeService;
        this.debug = debug;
    }

    @Override
    public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
            throws ServiceConfigException, ServiceErrorException {
        String treeName = CollectionUtils.getFirstItem(attributes.get("tree"));
        try {
            UUID innerTreeNodeId = UUID.fromString(configPath.get(0));
            AuthTree tree = authTreeService.getTree(realm, treeName)
                    .orElseThrow(() -> new ServiceConfigException("Configured tree does not exist"));
            if (tree.containsNode(innerTreeNodeId) || tree.visitNodes(new IsNodeReachable(innerTreeNodeId))) {
                throw new ServiceConfigException("Tree would result in recursive loop: " + treeName);
            }
        } catch (NodeProcessException e) {
            debug.warning("Could not evaluate trees", e);
            throw new ServiceErrorException("Could not evaluate tree");
        }
    }

    private static class IsNodeReachable implements NodeVisitor<Boolean> {

        private final UUID innerTreeNodeId;

        IsNodeReachable(UUID innerTreeNodeId) {
            this.innerTreeNodeId = innerTreeNodeId;
        }

        @Override
        public Boolean visit(UUID nodeId, Optional<Class<? extends Node>> nodeType, Optional<String> displayName,
                Supplier<Node> node, Stream<Supplier<Boolean>> visitings) throws NodeProcessException {
            return nodeId.equals(innerTreeNodeId)
                    || (InnerTreeEvaluatorNode.class.equals(nodeType.orElse(null))
                            && ((InnerTreeEvaluatorNode) node.get()).getTree().visitNodes(this))
                    || visitings.anyMatch(Supplier::get);
        }
    }

}
