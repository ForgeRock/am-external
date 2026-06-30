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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
import org.forgerock.openam.auth.node.api.PageNodeCompatibility;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.IdentifiableCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.sm.SMSException;

/**
 * Common base class for page node implementations.
 */
public abstract class AbstractPageNode implements Node {

    private final Logger logger = LoggerFactory.getLogger(AbstractPageNode.class);

    static final String PAGE_NODE_CALLBACKS = "pageNodeCallbacks";

    private static final String NO_DEFAULT_MESSAGE = "";

    /**
     * Node Config Declaration.
     */
    public interface Config {
        /**
         * The UI-displayable header for a node.  Defaults to an empty string in the event the node doesn't care to
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

    /**
     * The configuration for the page node.
     */
    protected final Config config;
    /**
     * The realm the page node is in.
     */
    protected final Realm realm;
    /**
     * The tree the page node is in.
     */
    protected final Tree tree;
    /**
     * A node factory instance that will be used to obtain instances of child nodes.
     */
    protected final NodeFactory nodeFactory;
    private final JsonValue nodeExtraLogging = json(array());
    private final LocalizedMessageProvider localizationHelper;
    private final NodeEventAuditor auditor;

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
    public AbstractPageNode(Config config, Realm realm, Tree tree, NodeFactory nodeFactory,
            NodeEventAuditor auditor, LocalizedMessageProviderFactory localizationHelperFactory) {
        this.config = config;
        this.realm = realm;
        this.tree = tree;
        this.nodeFactory = nodeFactory;
        this.auditor = auditor;
        this.localizationHelper = localizationHelperFactory.create(realm);
    }

    /**
     * Get the children of the page node.
     * @return The children of the page node.
     */
    public List<ChildNodeConfig> getChildren() {
        List<ChildNodeConfig> children = new ArrayList<>();
        for (String nodeConfigString : config.nodes()) {
            children.add(ChildNodeConfig.create(nodeConfigString));
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
        boolean firstPageExecution = callbacksFromLastExecution.isEmpty();
        Map<Integer, Integer> callbackToNodes = callbacksFromLastExecution.orElse(new HashMap<>());
        if (!firstPageExecution) {
            for (Callback callback : context.getAllCallbacks()) {
                if (callback instanceof IdentifiableCallback) {
                    receivedCallbacks.add((IdentifiableCallback) callback);
                }
            }
        }
        List<NodeProcessingData> nodeProcessingData = new ArrayList<>();
        for (int nodeIndex = 0; nodeIndex < config.nodes().size(); nodeIndex++) {
            ChildNodeConfig nodeConfig = ChildNodeConfig.create(config.nodes().get(nodeIndex));
            List<IdentifiableCallback> nodeCallbacks = getCallbacks(receivedCallbacks, callbackToNodes, nodeIndex);
            nodeProcessingData.add(new NodeProcessingData(nodeIndex, nodeConfig, nodeCallbacks));
        }

        nodeExtraLogging.clear();

        NodeProcessState initialState = new NodeProcessState(context, callbackToNodes);
        NodeProcessState result = processNodes(initialState, nodeProcessingData, firstPageExecution);

        Action.ActionBuilder actionBuilder = result.hasUnresolvedCallbacks
                ? mapCallbacks(context.sharedState, callbackToNodes, result.nodeCallbacks)
                : mapOutcome(context, result.outcome);
        result.sessionProperties.forEach(actionBuilder::putSessionProperty);

        return actionBuilder
                .withHeader(localizationHelper.getLocalizedMessage(context, AbstractPageNode.class, config.pageHeader(),
                        NO_DEFAULT_MESSAGE))
                .withDescription(localizationHelper.getLocalizedMessage(context, AbstractPageNode.class,
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
                ChildNodeConfig nodeConfig = ChildNodeConfig.create(nodeConfigString);
                Node node = nodeFactory.createNode(nodeConfig.type, nodeConfig.version,
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
                ChildNodeConfig nodeConfig = ChildNodeConfig.create(nodeConfigString);
                Node node = nodeFactory.createNode(nodeConfig.type, nodeConfig.version,
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
                .map(map -> map.entrySet().stream()
                        .collect(toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue)));
    }

    /**
     * Process the nodes in the page.
     * @param initialState The initial state of the node processing
     * @param nodeData The data for each node to be processed
     * @param firstPageExecution Whether this is the first execution of the page node
     * @return The state after processing the nodes
     * @throws NodeProcessException If there was a problem processing the nodes
     */
    protected abstract NodeProcessState processNodes(NodeProcessState initialState, List<NodeProcessingData> nodeData,
            boolean firstPageExecution) throws NodeProcessException;

    /**
     * Combine the new callbacks from processing the nodes with the existing callbacks.
     * @param existingCallbacks The existing callbacks
     * @param newCallbacks The new callbacks from processing a node
     * @return The combined list of callbacks
     */
    protected abstract List<IdentifiableCallback> mergeCallbacks(List<IdentifiableCallback> existingCallbacks,
            List<IdentifiableCallback> newCallbacks);

    /**
     * Processes a sub-list of nodes.
     * @param initialState the initial state
     * @param nodeData the node data
     * @param firstPageExecution whether it is the first page execution
     * @return the node process state
     * @throws NodeProcessException if an error occurs during processing
     */
    protected final NodeProcessState processNodeSubList(NodeProcessState initialState,
            List<NodeProcessingData> nodeData, boolean firstPageExecution) throws NodeProcessException {
        if (nodeData.isEmpty()) {
            return initialState;
        }
        boolean hasUnresolvedCallbacks = false;
        Map<Integer, Integer> callbackToNodes = initialState.callbackToNodes;
        Map<String, String> sessionProperties = initialState.sessionProperties;

        TreeContext initialContext = initialState.context;
        TreeContext currentContext = initialContext;
        JsonValue newSharedState = currentContext.sharedState;
        JsonValue newTransientState = currentContext.transientState;
        Action action = null;
        List<IdentifiableCallback> resultCallbacks = new ArrayList<>();
        for (NodeProcessingData nodeProcessingData : nodeData) {
            List<IdentifiableCallback> nodeCallbacks = nodeProcessingData.callbacks;

            currentContext = currentContext.copyWithCallbacksAndState(newSharedState, newTransientState, null,
                    firstPageExecution ? initialContext.getAllCallbacks() : nodeCallbacks.stream()
                            .map(cb -> cb.callback).collect(toList()));
            ChildNodeConfig nodeConfig = nodeProcessingData.nodeConfig;
            Node node = nodeFactory.createNode(nodeConfig.type, nodeConfig.version,
                    UUID.fromString(nodeConfig.nodeId), realm, tree);
            action = node.process(currentContext);

            // if node has callbacks, update this node's callbacks, otherwise leave them there to be returned to the ui.
            if (action.sendingCallbacks()) {
                if (action.callbacks.stream().anyMatch(cb -> !(cb instanceof MetadataCallback))) {
                    hasUnresolvedCallbacks = true;
                }
                addCallbacks(resultCallbacks, action.callbacks, callbackToNodes, nodeProcessingData.nodeIndex);
                mapCallbacks(action.sharedState != null ? action.sharedState : currentContext.sharedState,
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
            newSharedState = action.sharedState != null ? action.sharedState : currentContext.sharedState;
            auditor.logAuditEvent(tree, action, nodeConfig.type, nodeConfig.version, UUID.fromString(nodeConfig.nodeId),
                    nodeConfig.displayName, newSharedState, node.getAuditEntryDetail());
            newTransientState = action.transientState != null ? action.transientState : currentContext.transientState;
        }
        if (!hasUnresolvedCallbacks && action.sendingCallbacks()) {
            throw new NodeProcessException("No outcome and only metadata callbacks found");
        }
        currentContext = currentContext.copyWithCallbacksAndState(newSharedState, newTransientState,
                null, initialContext.getAllCallbacks());
        // note that we do not use the initialState hasUnresolvedCallbacks as this is a fresh group
        return new NodeProcessState(currentContext, hasUnresolvedCallbacks,
                mergeCallbacks(initialState.nodeCallbacks, resultCallbacks), action.outcome, callbackToNodes,
                sessionProperties);
    }

    /**
     * Data required for processing a node.
     * @param nodeIndex the index of the node in the page node configuration
     * @param nodeConfig the configuration of the child node
     * @param callbacks the callbacks associated with the node
     */
    protected record NodeProcessingData(int nodeIndex, ChildNodeConfig nodeConfig,
            List<IdentifiableCallback> callbacks) { }

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

    /**
     * Gets the page node compatibility for a node config.
     * @param nodeConfig the configuration of the child node
     * @param authNodeRegistry the authentication node registry
     * @return the page node compatibility
     */
    protected static PageNodeCompatibility getPageNodeCompatibility(ChildNodeConfig nodeConfig,
            NodeRegistry authNodeRegistry) {
        Class<? extends Node> nodeType = authNodeRegistry.getNodeType(nodeConfig.type(), nodeConfig.version());
        if (nodeType == null) {
            throw new IllegalStateException("Unknown node type: " + nodeConfig.type());
        }
        Metadata metadata = nodeType.getAnnotation(Metadata.class);
        return metadata != null ? metadata.pageNodeCompatibility() : PageNodeCompatibility.UNKNOWN;
    }

    /**
     * The node process state.
     * @param context the tree context
     * @param hasUnresolvedCallbacks whether there are unresolved callbacks
     * @param nodeCallbacks the callbacks associated with the node
     * @param outcome the outcome of the node
     * @param callbackToNodes a map of callback IDs to node indices
     * @param sessionProperties a map of session properties
     */
    protected record NodeProcessState(TreeContext context, boolean hasUnresolvedCallbacks,
            List<IdentifiableCallback> nodeCallbacks, String outcome, Map<Integer, Integer> callbackToNodes,
            Map<String, String> sessionProperties) {

        /**
         * Create an initial state for processing a node.
         * @param context the tree context
         * @param callbackToNodes a map of callback IDs to node indices
         */
        NodeProcessState(TreeContext context, Map<Integer, Integer> callbackToNodes) {
            this(context, false, emptyList(), null, callbackToNodes, new HashMap<>());
        }
    }

    /**
     * Object view of the String representation of a child node that is stored in SMS.
     * @param nodeId The ID of the Child Node.
     * @param type The Node Type of the Child Node.
     * @param version The Node Version of the Child Node.
     * @param displayName The display name of the Child Node.
     */
    public record ChildNodeConfig(String nodeId, String type, Integer version, String displayName)
            implements Tree.Node {
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

        /**
         * Create a new child node config from an SMS storage representation.
         *
         * @param nodeConfig The string representation.
         * @return The child node config.
         */
        public static ChildNodeConfig create(String nodeConfig) {
            String[] split = nodeConfig.split(":");
            if (split.length == 3) {
                return new ChildNodeConfig(split[0], split[1], 1, split[2]);
            } else {
                return new ChildNodeConfig(split[0], split[1], Integer.parseInt(split[2]), split[3]);
            }
        }

        /**
         * Create a new child node config from the JSON representation.
         *
         * @param nodeJson The JSON representation.
         * @return The child node config.
         */
        public static ChildNodeConfig create(JsonValue nodeJson) {
            JsonValue versionValue = nodeJson.get(NODE_VERSION_FIELD)
                                             .defaultTo(nodeJson.get("version"))
                                             .defaultTo(1);
            Integer version = versionValue.isNumber()
                    ? versionValue.asInteger()
                    : Integer.parseInt(versionValue.asString().split("\\.")[0]);
            return new ChildNodeConfig(nodeJson.get(ID_FIELD).asString(),
                    nodeJson.get(NODE_TYPE_FIELD).asString(),
                    version,
                    nodeJson.get(DISPLAY_NAME_FIELD).asString());
        }

        @Override
        public String toString() {
            return nodeId + ":" + type + ":" + version + ":" + displayName;
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
                    field(NODE_VERSION_FIELD, "%d.0".formatted(version)),
                    field(DISPLAY_NAME_FIELD, displayName)
            ));
        }

        @Override
        public UUID id() {
            return UUID.fromString(nodeId);
        }

        @Override
        public Integer version() {
            return version;
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
     * Provider for the outcomes of a PageNode decision.
     */
    abstract static class AbstractPageDecisionOutcomeProvider implements OutcomeProvider {
        public static final String PROPERTIES_FIELD = "_properties";
        private final Realm realm;
        private final NodeFactory nodeFactory;
        protected final NodeRegistry nodeRegistry;

        AbstractPageDecisionOutcomeProvider(Realm realm, NodeFactory nodeFactory, NodeRegistry nodeRegistry) {
            this.realm = realm;
            this.nodeFactory = nodeFactory;
            this.nodeRegistry = nodeRegistry;
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
        protected List<Outcome> getNodeOutcomes(ChildNodeConfig node, PreferredLocales locales, JsonValue nodeJson)
                throws NodeProcessException {
            if (!nodeJson.isDefined(PROPERTIES_FIELD)
                    && getPageNodeCompatibility(node, nodeRegistry) != PageNodeCompatibility.UNSUPPORTED) {
                try {
                    return nodeFactory.getNodeOutcomes(locales, realm, node.nodeId, node.type, node.version);
                } catch (SSOException | SMSException | RuntimeException e) {
                    throw new NodeProcessException("Node properties cannot be fetched", e);
                }
            } else {
                JsonValue properties = nodeJson.get(PROPERTIES_FIELD);
                return nodeFactory.getOutcomeProvider(realm, node.type, node.version).getOutcomes(locales, properties);
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
