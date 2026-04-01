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
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeEventAuditor;
import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.PageNodeCompatibility;
import org.forgerock.openam.auth.nodes.SelectIdPNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.IdentifiableCallback;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;

/**
 * A node that allows multiple nodes to be composed into a single page for returning to the user.
 */
@Node.Metadata(outcomeProvider = PageNode.PageDecisionOutcomeProvider.class,
        configClass = PageNode.Config.class, configValidator = PageNode.MultipleCallbacksOnlyInLastNode.class,
        tags = {"utilities"}, pageNodeCompatibility = PageNodeCompatibility.UNSUPPORTED)
@Node.VersionMetadata(name = "PageNode", version = 1)
public class PageNode extends AbstractPageNode {

    /**
     * Node Config Declaration.
     */
    public interface Config extends AbstractPageNode.Config {

    }

    /**
     * DI constructor for a page node.
     *
     * @param config The configuration for the node.
     * @param realm The realm the node is in.
     * @param tree The tree the node is in.
     * @param nodeFactory A node factory instance that will be used to obtain instances of child nodes.
     * @param auditor The node auditor.
     * @param localizationHelperFactory The localization helper factory.
     */
    @Inject
    public PageNode(@Assisted Config config, @Assisted Realm realm, @Assisted Tree tree, NodeFactory nodeFactory,
            NodeEventAuditor auditor,  LocalizedMessageProviderFactory localizationHelperFactory) {
        super(config, realm, tree, nodeFactory, auditor, localizationHelperFactory);
    }

    @Override
    protected NodeProcessState processNodes(NodeProcessState initialState, List<NodeProcessingData> nodeData,
            boolean firstPageExecution)
            throws NodeProcessException {
        int lastIndex = config.nodes().size() - 1;
        if (nodeIsSelectIdP(nodeData.get(lastIndex))) {
            NodeProcessState selectIdPState = processNodeSubList(initialState,
                    nodeData.subList(lastIndex, config.nodes().size()), firstPageExecution);
            if (selectIdPState.hasUnresolvedCallbacks() || "localAuthentication".equals(selectIdPState.outcome())) {
                NodeProcessState additionalNodeState = processNodeSubList(selectIdPState,
                        nodeData.subList(0, lastIndex), firstPageExecution);
                return new NodeProcessState(additionalNodeState.context(),
                        additionalNodeState.hasUnresolvedCallbacks() || selectIdPState.hasUnresolvedCallbacks(),
                        additionalNodeState.nodeCallbacks(),
                        additionalNodeState.hasUnresolvedCallbacks() ? null : selectIdPState.outcome(),
                        additionalNodeState.callbackToNodes(),
                        additionalNodeState.sessionProperties());
            } else {
                return selectIdPState;
            }
        }

        return processNodeSubList(initialState, nodeData, firstPageExecution);
    }

    @Override
    protected List<IdentifiableCallback> mergeCallbacks(List<IdentifiableCallback> existingCallbacks,
            List<IdentifiableCallback> newCallbacks) {
        List<IdentifiableCallback> resultCallbacks = new ArrayList<>(existingCallbacks);
        resultCallbacks.addAll(newCallbacks);
        return resultCallbacks;
    }

    private boolean nodeIsSelectIdP(NodeProcessingData nodeData) {
        return nodeData.nodeConfig().type().equals(SelectIdPNode.class.getSimpleName());
    }

    /**
     * Outcome provider for the PageNode.
     */
    static class PageDecisionOutcomeProvider extends AbstractPageDecisionOutcomeProvider {

        @Inject
        PageDecisionOutcomeProvider(@Assisted Realm realm, NodeFactory nodeFactory, NodeRegistry nodeRegistry) {
            super(realm, nodeFactory, nodeRegistry);
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            JsonValue nodesJson = nodeAttributes.get("nodes");
            if (nodesJson == null || nodesJson.size() == 0) {
                return emptyList();
            }
            ChildNodeConfig node;
            JsonValue nodeJson = nodesJson.get(nodesJson.size() - 1);
            if (nodeJson.isString()) {
                node = ChildNodeConfig.create(nodeJson.asString());
            } else {
                node = ChildNodeConfig.create(nodeJson);
            }
            return getNodeOutcomes(node, locales, nodeJson);
        }
    }

    /**
     * A config validator that checks that all the nodes in a page have one outcome, and that only the last node has
     * one or more.
     */
    public static class MultipleCallbacksOnlyInLastNode implements ServiceConfigValidator {
        private final AnnotatedServiceRegistry serviceRegistry;
        private final NodeRegistry nodeRegistry;
        private final NodeFactory nodeFactory;

        /**
         * DI constructor.
         *
         * @param serviceRegistry An instance of the service registry for checking for saved node content.
         * @param nodeRegistry An instance of the node registry to obtain configuration types for node type names.
         * @param nodeFactory A node factory for creating child node instances.
         */
        @Inject
        public MultipleCallbacksOnlyInLastNode(AnnotatedServiceRegistry serviceRegistry, NodeRegistry nodeRegistry,
                NodeFactory nodeFactory) {
            this.serviceRegistry = serviceRegistry;
            this.nodeRegistry = nodeRegistry;
            this.nodeFactory = nodeFactory;
        }

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            List<String> nodes = CollectionHelper.toValueList(attributes.get("nodes"));
            int i = 0;
            for (String nodeString : nodes) {
                ChildNodeConfig node = ChildNodeConfig.create(nodeString);
                if (getPageNodeCompatibility(node, nodeRegistry) == PageNodeCompatibility.UNSUPPORTED) {
                    throw new ServiceConfigException("Illegal child node type: " + node.type());
                }
                try {
                    Optional<?> config = serviceRegistry.getRealmInstance(nodeRegistry.getConfigType(node.type(),
                                    node.version()), realm, node.nodeId());
                    if (config.isPresent()) {
                        List<OutcomeProvider.Outcome> outcomes = nodeFactory.getNodeOutcomes(new PreferredLocales(),
                                realm, node.nodeId(), node.type(), node.version());
                        if (outcomes.isEmpty()) {
                            throw new ServiceConfigException("Node does not have any outcomes: " + node.type());
                        }
                        if (++i != nodes.size() && outcomes.size() != 1) {
                            throw new ServiceConfigException("Only the last node in a page can have more than one "
                                    + "outcome");
                        }
                    } else {
                        throw new ServiceConfigException("Node does not exist: " + node.nodeId());
                    }
                } catch (ServiceConfigException e) {
                    throw e;
                } catch (SSOException | SMSException e) {
                    throw new ServiceErrorException("Could not load child node: " + nodeString, e);
                } catch (NodeProcessException e) {
                    throw new ServiceErrorException("Could not obtain outcomes for node: " + nodeString, e);
                }
            }
        }
    }
}
