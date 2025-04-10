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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.am.trees.api.NodeVisitor;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate inner tree configuration does not contain a recursive loop.
 */
public class InnerTreeConfigValidator implements ServiceConfigValidator {
    private final TreeProvider treeProvider;
    private final Logger debug = LoggerFactory.getLogger(InnerTreeConfigValidator.class);

    /**
     * DI Constructor.
     * @param treeProvider The tree provider.
     */
    @Inject
    InnerTreeConfigValidator(TreeProvider treeProvider) {
        this.treeProvider = treeProvider;
    }

    @Override
    public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
            throws ServiceConfigException, ServiceErrorException {
        String treeName = CollectionUtils.getFirstItem(attributes.get("tree"));
        try {
            UUID innerTreeNodeId = UUID.fromString(configPath.get(0));
            Tree tree = treeProvider.getTree(realm, treeName)
                    .orElseThrow(() -> new ServiceConfigException("Configured tree does not exist"));
            if (tree.containsNode(innerTreeNodeId) || tree.visitNodes(new IsNodeReachable(innerTreeNodeId))) {
                throw new ServiceConfigException("Tree would result in recursive loop: " + treeName);
            }
        } catch (NodeProcessException e) {
            debug.warn("Could not evaluate trees", e);
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
