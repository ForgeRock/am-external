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
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.am.trees.api.NodeRegistry.DEFAULT_VERSION;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

import org.forgerock.am.trees.api.NodeEventAuditor;
import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.SelectIdPNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemPropertiesWrapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.IdentifiableCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;

/**
 * A node that allows multiple nodes to be composed into a single page for returning to the user.
 */
@Node.Metadata(outcomeProvider = PageNode.PageDecisionOutcomeProvider.class,
        configClass = PageNode.Config.class, configValidator = PageNode.MultipleCallbacksOnlyInLastNode.class,
        tags = {"utilities"})
public class PageNode implements Node {

    private final Logger logger = LoggerFactory.getLogger(PageNode.class);

    static final String PAGE_NODE_CALLBACKS = "pageNodeCallbacks";

    private static final String NO_DEFAULT_MESSAGE = "";

    /**
     * Node Config Declaration.
     */
    public interface Config {
        /**
         * The UI-displayable header for a node.  Defaults to an empty string in the even the node doesn't care to
         * apply a header -- a null header is not allowed.
         *
         * @return collection of locaized node headers
         */
        @Attribute(order = 100)
        default Map<Locale, String> pageHeader() {
            return Collections.emptyMap();
        }

        /**
         * The UI-displayable description for a node.  Defaults to an empty string in the event the node doesn't care
         * to apply a description -- a null description is not allowed.
         *
         * @return the node description
         */
        @Attribute(order = 200)
        default Map<Locale, String> pageDescription() {
            return Collections.emptyMap();
        }

        /**
         * An optional stage property to pass to the client to aid in rendering.
         *
         * @return an optional stage property
         */
        @Attribute(order = 300)
        String stage();

        /**
         * The list of nodes.
         *
         * @return The nodes.
         */
        @Attribute(order = 500)
        List<String> nodes();
    }

    private static final List<String> PROHIBITED_CHILD_NODE_TYPES = Arrays.asList(
            PageNode.class.getSimpleName(),
            InnerTreeEvaluatorNode.class.getSimpleName()
    );

    private final Config config;
    private final Realm realm;
    private final Tree tree;
    private final NodeFactory nodeFactory;
    private final JsonValue nodeExtraLogging = json(array());
    private final LocalizedMessageProvider localizationHelper;
    private final NodeEventAuditor auditor;
    private final SystemPropertiesWrapper systemProperties;

    /**
     * DI constructor for a page node.
     *
     * @param config The configuration for the node.
     * @param realm The realm the node is in.
     * @param tree The tree the node is in.
     * @param nodeFactory A node factory instance that will be used to obtain instances of child nodes.
     * @param auditor The node auditor.
     * @param localizationHelperFactory The localization helper factory.
     * @param systemProperties The system properties wrapper.
     */
    @Inject
    public PageNode(@Assisted Config config, @Assisted Realm realm, @Assisted Tree tree, NodeFactory nodeFactory,
            NodeEventAuditor auditor,  LocalizedMessageProviderFactory localizationHelperFactory,
                    SystemPropertiesWrapper systemProperties) {
        this.config = config;
        this.realm = realm;
        this.tree = tree;
        this.nodeFactory = nodeFactory;
        this.auditor = auditor;
        this.localizationHelper = localizationHelperFactory.create(realm);
        this.systemProperties = systemProperties;
    }

    /**
     * Get the children of the page node.
     * @return The children of the page node.
     */
    public List<ChildNodeConfig> getChildren() {
        List<ChildNodeConfig> children = new ArrayList<>();
        for (String nodeConfigString : config.nodes()) {
            children.add(ChildNodeConfig.create(nodeConfigString, systemProperties));
        }
        return children;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (config.nodes().isEmpty()) {
            throw new NodeProcessException("This page has no nodes in it, so cannot proceed");
        }

        List<IdentifiableCallback> receivedCallbacks = new ArrayList<>();
        Optional<Map<Integer, Integer>> callbacksFromLastExecution = getCallbacksToNodes(context);
        boolean firstPageExecution = !callbacksFromLastExecution.isPresent();
        Map<Integer, Integer> callbackToNodes = callbacksFromLastExecution.orElse(new HashMap<>());
        if (!firstPageExecution) {
            for (Callback callback : context.getAllCallbacks()) {
                if (callback instanceof IdentifiableCallback) {
                    receivedCallbacks.add((IdentifiableCallback) callback);
                }
            }
        }

        Map<String, String> sessionProperties = new HashMap<>();
        nodeExtraLogging.clear();

        NodeProcessState result = processNodes(context, receivedCallbacks, callbackToNodes, firstPageExecution,
                sessionProperties);

        Action.ActionBuilder actionBuilder = result.hasUnresolvedCallbacks
                ? mapCallbacks(context.sharedState, callbackToNodes, result.nodeCallbacks)
                : mapOutcome(context, result.outcome);
        sessionProperties.forEach(actionBuilder::putSessionProperty);

        return actionBuilder
                .withHeader(localizationHelper.getLocalizedMessage(context, PageNode.class, config.pageHeader(),
                        NO_DEFAULT_MESSAGE))
                .withDescription(localizationHelper.getLocalizedMessage(context, PageNode.class,
                        config.pageDescription(), NO_DEFAULT_MESSAGE))
                .withStage(config.stage())
                .replaceSharedState(result.context.sharedState)
                .replaceTransientState(result.context.transientState)
                .build();
    }

    @Override
    public InputState[] getInputs() {
        List<InputState> inputs = new ArrayList<>();

        for (String nodeConfigString : config.nodes()) {
            try {
                ChildNodeConfig nodeConfig = ChildNodeConfig.create(nodeConfigString, systemProperties);
                // TODO AME-28921 : Upgrade page node to handle node versioning
                Node node = nodeFactory.createNode(nodeConfig.type, DEFAULT_VERSION,
                        UUID.fromString(nodeConfig.nodeId), realm, tree);
                inputs.addAll(List.of(node.getInputs()));
            } catch (NodeProcessException e) {
                logger.error("Failed to collect inputs of contained node: {}", nodeConfigString, e);
            }
        }

        return inputs.stream().distinct().toArray(InputState[]::new);
    }

    @Override
    public OutputState[] getOutputs() {
        List<OutputState> outputs = new ArrayList<>();

        for (String nodeConfigString : config.nodes()) {
            try {
                ChildNodeConfig nodeConfig = ChildNodeConfig.create(nodeConfigString, systemProperties);
                Node node = nodeFactory.createNode(nodeConfig.type, DEFAULT_VERSION,
                        UUID.fromString(nodeConfig.nodeId), realm, tree);
                outputs.addAll(List.of(node.getOutputs()));
            } catch (NodeProcessException e) {
                logger.error("Failed to collect outputs of contained node: {}", nodeConfigString, e);
            }
        }

        if (outputs.stream().anyMatch(outputState -> outputState.name.equals("*"))) {
            return new OutputState[]{new OutputState("*")};
        } else {
            return outputs.stream().distinct().toArray(OutputState[]::new);
        }
    }

    private Action.ActionBuilder mapCallbacks(JsonValue sharedState, Map<Integer, Integer> callbackToNodes,
            List<IdentifiableCallback> callbacks) {
        sharedState.put(PAGE_NODE_CALLBACKS, callbackToNodes.entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), Map.Entry::getValue)));
        return Action.send(callbacks);
    }

    private Action.ActionBuilder mapOutcome(TreeContext context, String outcome) {
        context.sharedState.remove(PAGE_NODE_CALLBACKS);
        return Action.goTo(outcome);
    }

    private Optional<Map<Integer, Integer>> getCallbacksToNodes(TreeContext context) {
        Map<String, Integer> mapping = context.sharedState.get(PAGE_NODE_CALLBACKS).asMap(Integer.class);
        return Optional.ofNullable(mapping)
                .map(map -> map.entrySet().stream().collect(toMap(e -> Integer.parseInt(e.getKey()),
                        Map.Entry::getValue)));
    }

    private NodeProcessState processNodes(TreeContext context, List<IdentifiableCallback> receivedCallbacks,
            Map<Integer, Integer> callbackToNodes, boolean firstPageExecution, Map<String, String> sessionProperties)
            throws NodeProcessException {
        if (lastNodeIsSelectIdP()) {
            int selectIdPIndex = config.nodes().size() - 1;
            NodeProcessState selectIdPState = processNodeSubList(new NodeProcessState(context),
                    config.nodes().subList(selectIdPIndex, config.nodes().size()), selectIdPIndex, receivedCallbacks,
                    callbackToNodes, firstPageExecution, sessionProperties);
            if (selectIdPState.hasUnresolvedCallbacks || "localAuthentication".equals(selectIdPState.outcome)) {
                NodeProcessState additionalNodeState = processNodeSubList(selectIdPState,
                        config.nodes().subList(0, selectIdPIndex), 0, receivedCallbacks, callbackToNodes,
                        firstPageExecution, sessionProperties);
                return new NodeProcessState(additionalNodeState.context, additionalNodeState.hasUnresolvedCallbacks,
                        additionalNodeState.nodeCallbacks, selectIdPState.outcome);
            } else {
                return selectIdPState;
            }
        }

        return processNodeSubList(new NodeProcessState(context), config.nodes(), 0, receivedCallbacks, callbackToNodes,
                firstPageExecution, sessionProperties);
    }

    private NodeProcessState processNodeSubList(NodeProcessState existingState, List<String> nodes, int nodeIndex,
            List<IdentifiableCallback> receivedCallbacks, Map<Integer, Integer> callbackToNodes,
            boolean firstPageExecution, Map<String, String> sessionProperties) throws NodeProcessException {
        if (nodes.isEmpty()) {
            return existingState;
        }
        boolean hasUnresolvedCallbacks = false;
        JsonValue newSharedState;
        JsonValue newTransientState;

        List<IdentifiableCallback> nodeCallbacks = getCallbacks(receivedCallbacks, callbackToNodes, nodeIndex);
        TreeContext context = existingState.context;
        TreeContext sharedContext = firstPageExecution
                ? context
                : context.copyWithCallbacks(nodeCallbacks.stream().map(cb -> cb.callback).collect(toList()));
        Action action = null;
        List<IdentifiableCallback> resultCallbacks = new ArrayList<>(existingState.nodeCallbacks);
        for (String nodeConfigString : nodes) {
            ChildNodeConfig nodeConfig = ChildNodeConfig.create(nodeConfigString, systemProperties);
            Node node = nodeFactory.createNode(nodeConfig.type, DEFAULT_VERSION,
                    UUID.fromString(nodeConfig.nodeId), realm, tree);
            action = node.process(sharedContext);

            // if node has callbacks, update this node's callbacks, otherwise leave them there to be returned to the ui.
            if (action.sendingCallbacks()) {
                if (action.callbacks.stream().anyMatch(cb -> !(cb instanceof MetadataCallback))) {
                    hasUnresolvedCallbacks = true;
                }
                addCallbacks(resultCallbacks, action.callbacks, callbackToNodes, nodeIndex);
                mapCallbacks(action.sharedState != null ? action.sharedState : sharedContext.sharedState,
                        callbackToNodes, resultCallbacks);
            } else {
                resultCallbacks.addAll(nodeCallbacks);
            }

            nodeExtraLogging.add(json(
                    object(
                            field("nodeType", nodeConfig.type),
                            field("nodeId", nodeConfig.nodeId)
                    )).asMap()
            );

            action.sessionProperties.forEach(sessionProperties::put);
            newSharedState = action.sharedState != null ? action.sharedState : sharedContext.sharedState;
            auditor.logAuditEvent(tree, action, nodeConfig.type, nodeConfig.version, UUID.fromString(nodeConfig.nodeId),
                    nodeConfig.displayName, newSharedState, node.getAuditEntryDetail());
            newTransientState = action.transientState != null ? action.transientState : sharedContext.transientState;
            nodeCallbacks = getCallbacks(receivedCallbacks, callbackToNodes, ++nodeIndex);
            sharedContext = sharedContext.copyWithCallbacksAndState(newSharedState, newTransientState, null,
                    firstPageExecution ? context.getAllCallbacks() : nodeCallbacks.stream()
                            .map(cb -> cb.callback).collect(toList()));
        }
        if (!hasUnresolvedCallbacks && action.sendingCallbacks()) {
            throw new NodeProcessException("No outcome and only metadata callbacks found");
        }
        return new NodeProcessState(sharedContext, existingState.hasUnresolvedCallbacks || hasUnresolvedCallbacks,
                resultCallbacks, action.outcome);
    }

    private void addCallbacks(List<IdentifiableCallback> resultCallbacks, List<? extends Callback> nodeCallbacks,
            Map<Integer, Integer> callbackToNodes, int nodeIndex) {
        nodeCallbacks.stream()
                .map(callback -> new IdentifiableCallback(getAndAddIdentifier(callbackToNodes, nodeIndex), callback))
                .forEach(resultCallbacks::add);
    }

    private List<IdentifiableCallback> getCallbacks(List<IdentifiableCallback> identifiableCallbacks,
            Map<Integer, Integer> callbackToNodes, int nodeIndex) {
        return identifiableCallbacks.stream()
                .filter(callback -> Optional.ofNullable(callbackToNodes.get(callback.id)).orElse(-1) == nodeIndex)
                .collect(toList());
    }

    private Integer getAndAddIdentifier(Map<Integer, Integer> callbackToNodes, int nodeIndex) {
        Integer id = callbackToNodes.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        callbackToNodes.put(id, nodeIndex);
        return id;
    }

    private boolean lastNodeIsSelectIdP() {
        return ChildNodeConfig.create(config.nodes().get(config.nodes().size() - 1), systemProperties)
                .type.equals(SelectIdPNode.class.getSimpleName());
    }

    private class NodeProcessState {
        TreeContext context;
        boolean hasUnresolvedCallbacks;
        List<IdentifiableCallback> nodeCallbacks;
        String outcome;

        NodeProcessState(TreeContext context) {
            this.context = context;
            this.hasUnresolvedCallbacks = false;
            this.nodeCallbacks = emptyList();
            this.outcome = null;
        }

        NodeProcessState(TreeContext context, boolean hasUnresolvedCallbacks, List<IdentifiableCallback> nodeCallbacks,
                String outcome) {
            this.context = context;
            this.hasUnresolvedCallbacks = hasUnresolvedCallbacks;
            this.nodeCallbacks = nodeCallbacks;
            this.outcome = outcome;
        }
    }

    /**
     * Outcome provider for the PageNode.
     */
    static class PageDecisionOutcomeProvider implements OutcomeProvider {
        public static final String PROPERTIES_FIELD = "_properties";
        private final Realm realm;
        private final NodeFactory nodeFactory;
        private final SystemPropertiesWrapper systemProperties;

        @Inject
        PageDecisionOutcomeProvider(@Assisted Realm realm, NodeFactory nodeFactory,
                                    SystemPropertiesWrapper systemProperties) {
            this.realm = realm;
            this.nodeFactory = nodeFactory;
            this.systemProperties = systemProperties;
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
                node = ChildNodeConfig.create(nodeJson.asString(), systemProperties);
            } else {
                node = ChildNodeConfig.create(nodeJson, systemProperties);
            }
            return getNodeOutcomes(node, locales, nodeJson);
        }

        /**
         * Getter for the child node outcomes. In case of the ChoiceCollectorNode it has to be done through the
         * nodeFactory.getNodeOutcomes as the outcomes for that node are dynamic.
         * @param node The ChildNodeConfig object
         * @param locales Locale
         * @param nodeJson Node configuration Json representation
         * @return List of outcomes
         * @throws NodeProcessException If any exception happened using the nodeFactory
         */
        private List<Outcome> getNodeOutcomes(ChildNodeConfig node, PreferredLocales locales, JsonValue nodeJson)
                throws NodeProcessException {
            if (!nodeJson.isDefined(PROPERTIES_FIELD) && !PROHIBITED_CHILD_NODE_TYPES.contains(node.type)) {
                try {
                    return nodeFactory.getNodeOutcomes(locales, realm, node.nodeId, node.type, node.version);
                } catch (SSOException | SMSException | RuntimeException e) {
                    throw new NodeProcessException("Node properties cannot be fetched", e);
                }
            } else {
                JsonValue properties = nodeJson.get(PROPERTIES_FIELD);
                return nodeFactory.getOutcomeProvider(realm, node.type, DEFAULT_VERSION)
                               .getOutcomes(locales, properties);
            }
        }
    }

    /**
     * Object view of the String representation of a child node that is stored in SMS.
     */
    public static final class ChildNodeConfig implements Tree.Node {
        /**
         * The ID of the Child Node.
         */
        public static final String ID_FIELD = "_id";

        /**
         * The display name of the Child Node.
         */
        public static final String DISPLAY_NAME_FIELD = "displayName";

        /**
         * The Node Type of the Child Node.
         */
        public static final String NODE_TYPE_FIELD = "nodeType";

        /**
         * The Node Version of the Child Node.
         */
        public static final String NODE_VERSION_FIELD = "nodeVersion";
        private final String nodeId;
        private final String type;
        private final Integer version;
        private final String displayName;
        private final SystemPropertiesWrapper systemProperties;

        /**
         * Constructor.
         *
         * @param nodeId The ID of the Child Node.
         * @param type The Node Type of the Child Node.
         * @param version The Node Version of the Child Node.
         * @param displayName The display name of the Child Node.
         * @param systemProperties The SystemPropertiesWrapper object.
         */
        public ChildNodeConfig(String nodeId, String type, Integer version, String displayName,
                               SystemPropertiesWrapper systemProperties) {
            this.nodeId = nodeId;
            this.type = type;
            this.version = version;
            this.displayName = displayName;
            this.systemProperties = systemProperties;
        }

        /**
         * Create a new child node config from an SMS storage representation.
         *
         * @param nodeConfig The string representation.
         * @param systemProperties The SystemPropertiesWrapper object.
         * @return The child node config.
         */
        public static ChildNodeConfig create(String nodeConfig, SystemPropertiesWrapper systemProperties) {
            String[] split = nodeConfig.split(":");
            if (split.length == 3) {
                return new ChildNodeConfig(split[0], split[1], 1, split[2], systemProperties);
            } else {
                return new ChildNodeConfig(split[0], split[1], Integer.parseInt(split[2]), split[3], systemProperties);
            }
        }

        /**
         * Create a new child node config from the JSON representation.
         *
         * @param nodeJson The JSON representation.
         * @param systemProperties The SystemPropertiesWrapper object.
         * @return The child node config.
         */
        public static ChildNodeConfig create(JsonValue nodeJson, SystemPropertiesWrapper systemProperties) {
            return new ChildNodeConfig(nodeJson.get(ID_FIELD).asString(),
                    nodeJson.get(NODE_TYPE_FIELD).asString(),
                    nodeJson.get(NODE_VERSION_FIELD).defaultTo(1).asInteger(),
                    nodeJson.get(DISPLAY_NAME_FIELD).asString(),
                    systemProperties);
        }

        @Override
        public String toString() {
            return nodeId + ":" + type + ":" + displayName;
        }

        /**
         * Get the child node configuration in JSON form.
         *
         * @return The JSON representation.
         */
        public JsonValue asJson() {
            return json(object(
                    field(ID_FIELD, nodeId),
                    field(NODE_TYPE_FIELD, type),
                    field(DISPLAY_NAME_FIELD, displayName)));
        }

        @Override
        public UUID id() {
            return UUID.fromString(nodeId);
        }

        @Override
        public Integer version() {
            return DEFAULT_VERSION;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ChildNodeConfig) obj;
            return Objects.equals(this.nodeId, that.nodeId) && Objects.equals(this.type, that.type)
                    && Objects.equals(this.version, that.version) && Objects.equals(this.displayName, that.displayName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, type, version, displayName);
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
        private final SystemPropertiesWrapper systemProperties;

        /**
         * DI constructor.
         *
         * @param serviceRegistry An instance of the service registry for checking for saved node content.
         * @param nodeRegistry An instance of the node registry to obtain configuration types for node type names.
         * @param nodeFactory A node factory for creating child node instances.
         * @param systemProperties A SystemPropertiesWrapper object.
         */
        @Inject
        public MultipleCallbacksOnlyInLastNode(AnnotatedServiceRegistry serviceRegistry, NodeRegistry nodeRegistry,
                NodeFactory nodeFactory, SystemPropertiesWrapper systemProperties) {
            this.serviceRegistry = serviceRegistry;
            this.nodeRegistry = nodeRegistry;
            this.nodeFactory = nodeFactory;
            this.systemProperties = systemProperties;
        }

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            List<String> nodes = CollectionHelper.toValueList(attributes.get("nodes"));
            int i = 0;
            for (String nodeString : nodes) {
                ChildNodeConfig node = ChildNodeConfig.create(nodeString, systemProperties);
                if (PROHIBITED_CHILD_NODE_TYPES.contains(node.type)) {
                    throw new ServiceConfigException("Illegal child node type: " + node.type);
                }
                try {
                    Optional<?> config = serviceRegistry.getRealmInstance(nodeRegistry.getConfigType(node.type,
                                    DEFAULT_VERSION), realm, node.nodeId);
                    if (config.isPresent()) {
                        List<OutcomeProvider.Outcome> outcomes = nodeFactory.getNodeOutcomes(new PreferredLocales(),
                                realm, node.nodeId, node.type, node.version);
                        if (outcomes.size() == 0) {
                            throw new ServiceConfigException("Node does not have any outcomes: " + node.type);
                        }
                        if (++i != nodes.size() && outcomes.size() != 1) {
                            throw new ServiceConfigException("Only the last node in a page can have more than one "
                                    + "outcome");
                        }
                    } else {
                        throw new ServiceConfigException("Node does not exist: " + node.nodeId);
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

    @Override
    public JsonValue getAuditEntryDetail() {
        if (nodeExtraLogging.asList().isEmpty()) {
            return json(object());
        }
        return json(object(
                field("auditInfo", nodeExtraLogging)));
    }
}
