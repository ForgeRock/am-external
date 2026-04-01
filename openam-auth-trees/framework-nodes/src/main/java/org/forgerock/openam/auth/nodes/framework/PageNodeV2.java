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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.auth.node.api.PageNodeCompatibility.STANDALONE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.forgerock.openam.auth.nodes.SelectIdPNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidatorV2;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.IdentifiableCallback;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;

/**
 * Composes multiple child nodes into a single page returned to the user.
 * <p>
 * V2 adds:
 * - Standalone nodes: must be at the end; their outcomes are returned directly by the page node
 *   so they can control tree branching (e.g. SelectIdPNode, WebAuthnAuthenticationNode).
 * - Groups: nodes are processed in groups; the page outcome is taken from the last node of the
 *   last processed group.
 */
@Node.Metadata(outcomeProvider = PageNodeV2.PageDecisionV2OutcomeProvider.class,
        configClass = PageNodeV2.ConfigV2.class,
        configValidator = PageNodeV2.PageNodeV2Validator.class,
        tags = {"utilities"})
@Node.VersionMetadata(name = "PageNode", version = 2)
public class PageNodeV2 extends AbstractPageNode {

    private final NodeRegistry nodeRegistry;

    public interface ConfigV2 extends AbstractPageNode.Config {
    }

    /**
     * DI constructor for a page node.
     *
     * @param config                    The configuration for the node.
     * @param realm                     The realm the node is in.
     * @param tree                      The tree the node is in.
     * @param nodeFactory               A node factory instance that will be used to obtain instances of child nodes.
     * @param auditor                   The node auditor.
     * @param localizationHelperFactory The localization helper factory.
     */
    @Inject
    PageNodeV2(@Assisted ConfigV2 config, @Assisted Realm realm, @Assisted Tree tree, NodeFactory nodeFactory,
               NodeEventAuditor auditor, LocalizedMessageProviderFactory localizationHelperFactory,
               NodeRegistry nodeRegistry) {
        super(config, realm, tree, nodeFactory, auditor, localizationHelperFactory);
        this.nodeRegistry = nodeRegistry;
    }

    @Override
    protected NodeProcessState processNodes(NodeProcessState initialState, List<NodeProcessingData> nodeData,
                                            boolean firstPageExecution)
            throws NodeProcessException {
        Map<ChildNodeConfig, NodeProcessingData> dataMap = nodeData.stream()
                .collect(Collectors.toMap(NodeProcessingData::nodeConfig, data -> data));
        List<List<ChildNodeConfig>> groupedConfigs = group(nodeData.stream()
                .map(NodeProcessingData::nodeConfig).toList(), nodeRegistry);
        NodeProcessState currentState = initialState;

        for (int i = groupedConfigs.size() - 1; i >= 0; i--) {
            boolean isStandalone = groupedConfigs.get(i).size() == 1
                    && isStandaloneNode(groupedConfigs.get(i).get(0), nodeRegistry);
            currentState = processNodeSubList(currentState,
                    groupedConfigs.get(i).stream().map(dataMap::get).toList(), firstPageExecution);

            // if the result of the standalone node is localAuthentication, we ignore it and continue processing
            // this is due to existing SelectIdPNode behaviour to avoid automatically completing when there is only
            // one IdP
            if (!currentState.hasUnresolvedCallbacks() && currentState.outcome() != null
                    && !(isStandalone && currentState.outcome().equals("localAuthentication"))) {

                return new NodeProcessState(
                        currentState.context(), currentState.hasUnresolvedCallbacks(), currentState.nodeCallbacks(),
                        groupedConfigs.get(i).get(groupedConfigs.get(i).size() - 1).nodeId()
                                + "-" + currentState.outcome(),
                        currentState.callbackToNodes(), currentState.sessionProperties());
            }
        }
        return currentState;
    }

    @Override
    protected List<IdentifiableCallback> mergeCallbacks(List<IdentifiableCallback> existingCallbacks,
            List<IdentifiableCallback> newCallbacks) {
        List<IdentifiableCallback> resultCallbacks = new ArrayList<>(existingCallbacks);
        resultCallbacks.addAll(0, newCallbacks);
        return resultCallbacks;
    }

    private static boolean isStandaloneNode(ChildNodeConfig nodeConfig, NodeRegistry authNodeRegistry) {
        return STANDALONE == getPageNodeCompatibility(nodeConfig, authNodeRegistry);
    }

    /**
     * Outcome provider for the PageNode.
     */
    static class PageDecisionV2OutcomeProvider extends AbstractPageDecisionOutcomeProvider {

        @Inject
        PageDecisionV2OutcomeProvider(@Assisted Realm realm, NodeFactory nodeFactory, NodeRegistry nodeRegistry) {
            super(realm, nodeFactory, nodeRegistry);
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            JsonValue nodesJson = nodeAttributes.get("nodes");
            if (nodesJson == null || nodesJson.size() == 0) {
                return emptyList();
            }
            Map<ChildNodeConfig, JsonValue> configMap = new HashMap<>();
            List<ChildNodeConfig> configList = nodesJson.stream()
                    .map(nodeJson -> {
                        ChildNodeConfig config;
                        if (nodeJson.isString()) {
                            config = ChildNodeConfig.create(nodeJson.asString());
                        } else {
                            config = ChildNodeConfig.create(nodeJson);
                        }
                        configMap.put(config, nodeJson);
                        return config;
                    })
                    .toList();
            List<List<ChildNodeConfig>> groupedConfigs = group(configList, nodeRegistry);

            List<Outcome> outcomes = new ArrayList<>();

            for (List<ChildNodeConfig> group : groupedConfigs) {
                ChildNodeConfig lastConfig = group.get(group.size() - 1);
                JsonValue lastNodeJson = configMap.get(lastConfig);
                boolean isStandalone = isStandaloneNode(lastConfig, nodeRegistry);
                outcomes.addAll(getNodeOutcomes(lastConfig, locales, lastNodeJson).stream()
                        .filter(outcome -> !(isStandalone && "localAuthentication".equals(outcome.id)))
                        .map(outcome -> new Outcome(lastConfig.nodeId() + "-" + outcome.id, outcome.displayName))
                        .toList());
            }

            return outcomes;
        }
    }

    private static List<List<ChildNodeConfig>> group(List<ChildNodeConfig> childNodeConfigs,
            NodeRegistry nodeRegistry) {
        boolean encounteredStandaloneNode = false;
        List<ChildNodeConfig> nonStandaloneNodes = new ArrayList<>();
        List<List<ChildNodeConfig>> result = new ArrayList<>();
        for (ChildNodeConfig childNodeConfig : childNodeConfigs) {
            if (isStandaloneNode(childNodeConfig, nodeRegistry)) {
                encounteredStandaloneNode = true;
                result.add(List.of(childNodeConfig));
            } else if (encounteredStandaloneNode) {
                throw new IllegalStateException("Standalone nodes must be at the end of the node list");
            } else {
                nonStandaloneNodes.add(childNodeConfig);
            }
        }
        if (!nonStandaloneNodes.isEmpty()) {
            result.add(0, nonStandaloneNodes);
        }
        return result;
    }

    /**
     * Validator for PageNodeV2.
     */
    public static class PageNodeV2Validator implements ServiceConfigValidatorV2 {
        private static final List<String> PROHIBITED_CHILD_NODE_TYPES = Arrays.asList(
                PageNodeV2.class.getSimpleName(),
                InnerTreeEvaluatorNode.class.getSimpleName()
        );

        private final AnnotatedServiceRegistry serviceRegistry;
        private final NodeRegistry nodeRegistry;
        private final NodeFactory nodeFactory;

        /**
         * DI constructor.
         *
         * @param serviceRegistry An instance of the service registry for checking for saved node content.
         * @param nodeRegistry    An instance of the node registry to obtain configuration types for node type names.
         * @param nodeFactory     A node factory for creating child node instances.
         */
        @Inject
        PageNodeV2Validator(AnnotatedServiceRegistry serviceRegistry, NodeRegistry nodeRegistry,
                            NodeFactory nodeFactory) {
            this.serviceRegistry = serviceRegistry;
            this.nodeRegistry = nodeRegistry;
            this.nodeFactory = nodeFactory;
        }

        @Override
        public void validate(Realm realm, String serviceName, List<String> configPath, Map<String,
                Set<String>> attributes) throws ServiceConfigException, ServiceErrorException {
            List<String> nodes = CollectionHelper.toValueList(attributes.get("nodes"));
            boolean standaloneNodeEncountered = false;

            // List of non-standalone nodes to check for multiple outcomes (all but the last one should have 1 outcome)
            List<ChildNodeConfig> nonStandaloneNodes = new ArrayList<>();

            for (String nodeString : nodes) {
                ChildNodeConfig node = ChildNodeConfig.create(nodeString);

                if (PROHIBITED_CHILD_NODE_TYPES.contains(node.type())) {
                    throw new ServiceConfigException("Illegal child node type: " + node.type());
                }

                if (isStandaloneNode(node, nodeRegistry)) {
                    standaloneNodeEncountered = true;

                    if (node.type().equals(SelectIdPNode.class.getSimpleName())) {
                        checkSelectIdPNodeHasLocalAuth(realm, node);
                    }
                } else {
                    if (standaloneNodeEncountered) {
                        throw new ServiceConfigException("Standalone nodes must be at the end of the node list");
                    }
                    nonStandaloneNodes.add(node);
                }
            }

            validateNonStandaloneNodesOutcomes(realm, nonStandaloneNodes);
        }

        private void checkSelectIdPNodeHasLocalAuth(Realm realm, ChildNodeConfig node)
                throws ServiceConfigException, ServiceErrorException {
            try {
                Object configInstance = serviceRegistry.getRealmInstance(
                                nodeRegistry.getConfigType(node.type(), node.version()), realm, node.nodeId())
                        .orElseThrow(() -> new ServiceConfigException("Node does not exist: " + node.nodeId()));

                if (configInstance instanceof SelectIdPNode.Config config) {
                    if (!config.includeLocalAuthentication()) {
                        throw new ServiceConfigException(
                                "SelectIdPNode must have local authentication enabled when used in PageNodeV2");
                    }
                }
            } catch (ServiceConfigException e) {
                throw e;
            } catch (Exception e) {
                throw new ServiceErrorException("Could not validate SelectIdPNode: " + node.nodeId(), e);
            }
        }

        private void validateNonStandaloneNodesOutcomes(Realm realm, List<ChildNodeConfig> nodes)
                throws ServiceConfigException, ServiceErrorException {
            for (int i = 0; i < nodes.size(); i++) {
                ChildNodeConfig node = nodes.get(i);
                // If it is NOT the last non-standalone node, it must have exactly 1 outcome.
                if (i < nodes.size() - 1) {
                    try {
                        List<OutcomeProvider.Outcome> outcomes = nodeFactory.getNodeOutcomes(new PreferredLocales(),
                                realm, node.nodeId(), node.type(), node.version());
                        if (outcomes.isEmpty()) {
                            throw new ServiceConfigException("Node does not have any outcomes: " + node.type());
                        }
                        if (outcomes.size() != 1) {
                            throw new ServiceConfigException(
                                    "Only the last non-standalone node in a page can have more than one outcome");
                        }
                    } catch (ServiceConfigException e) {
                        throw e;
                    } catch (NodeProcessException | SSOException | SMSException | RuntimeException e) {
                        throw new ServiceErrorException("Could not obtain outcomes for node: " + node.nodeId(), e);
                    }
                }
            }
        }
    }
}
