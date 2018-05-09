/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.json.JsonValue.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.util.Pair;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.IdentifiableCallback;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;

import io.vavr.control.Either;

/**
 * A node that allows multiple nodes to be composed into a single page for returning to the user.
 */
@Node.Metadata(outcomeProvider = PageNode.PageDecisionOutcomeProvider.class,
        configClass = PageNode.Config.class, configValidator = PageNode.MultipleCallbacksOnlyInLastNode.class)
public class PageNode implements Node {

    static final String PAGE_NODE_CALLBACKS = "pageNodeCallbacks";

    interface Config {
        @Attribute(order = 100)
        List<String> nodes();
    }

    private static final List<String> PROHIBITED_CHILD_NODE_TYPES = Arrays.asList(
            PageNode.class.getSimpleName(),
            InnerTreeEvaluatorNode.class.getSimpleName()
    );

    private final Config config;
    private final Realm realm;
    private final AuthTree tree;
    private final NodeFactory nodeFactory;

    /**
     * DI constructor for a page node.
     *
     * @param config The configuration for the node.
     * @param realm The realm the node is in.
     * @param tree The tree the node is in.
     * @param nodeFactory A node factory instance that will be used to obtain instances of child nodes.
     */
    @Inject
    public PageNode(@Assisted Config config, @Assisted Realm realm, @Assisted AuthTree tree, NodeFactory nodeFactory) {
        this.config = config;
        this.realm = realm;
        this.tree = tree;
        this.nodeFactory = nodeFactory;
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

        Pair<TreeContext, Either<List<IdentifiableCallback>, String>> result = processNodes(context, receivedCallbacks,
                callbackToNodes, firstPageExecution, sessionProperties);

        Action.ActionBuilder actionBuilder = result.getSecond().fold(
            callbacks -> mapCallbacks(context, callbackToNodes, callbacks),
            s -> mapOutcome(context, result.getSecond().get()));
        sessionProperties.forEach(actionBuilder::putSessionProperty);
        return actionBuilder
                .replaceSharedState(result.getFirst().sharedState)
                .replaceTransientState(result.getFirst().transientState)
                .build();
    }

    private Action.ActionBuilder mapCallbacks(TreeContext context, Map<Integer, Integer> callbackToNodes,
            List<IdentifiableCallback> callbacks) {
        context.sharedState.put(PAGE_NODE_CALLBACKS, callbackToNodes.entrySet().stream()
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
                .map(map -> map.entrySet().stream().collect(toMap(e -> new Integer(e.getKey()), Map.Entry::getValue)));
    }

    private Pair<TreeContext, Either<List<IdentifiableCallback>, String>> processNodes(TreeContext context,
            List<IdentifiableCallback> receivedCallbacks, Map<Integer, Integer> callbackToNodes,
            boolean firstPageExecution, Map<String, String> sessionProperties) throws NodeProcessException {
        boolean hasUnresolvedCallbacks = false;
        JsonValue newSharedState;
        JsonValue newTransientState;

        int nodeIndex = 0;
        List<IdentifiableCallback> nodeCallbacks = getCallbacks(receivedCallbacks, callbackToNodes, nodeIndex);
        TreeContext sharedContext = firstPageExecution
                ? context
                : new TreeContext(context.sharedState, context.transientState, context.request,
                        nodeCallbacks.stream().map(cb -> cb.callback).collect(toList()));
        Action action = null;
        List<IdentifiableCallback> resultCallbacks = new ArrayList<>();
        for (String nodeConfigString : config.nodes()) {
            ChildNodeConfig nodeConfig = new ChildNodeConfig(nodeConfigString);
            Node node = nodeFactory.createNode(nodeConfig.nodeType, UUID.fromString(nodeConfig.nodeId), realm, tree);
            action = node.process(sharedContext);

            // if node has callbacks, update this node's callbacks, otherwise leave them there to be returned to the ui.
            if (action.sendingCallbacks()) {
                hasUnresolvedCallbacks = true;
                addCallbacks(resultCallbacks, action.callbacks, callbackToNodes, nodeIndex);
            } else {
                resultCallbacks.addAll(nodeCallbacks);
            }

            action.sessionProperties.forEach(sessionProperties::put);
            newSharedState = action.sharedState != null ? action.sharedState : sharedContext.sharedState;
            newTransientState = action.transientState != null ? action.transientState : sharedContext.transientState;
            nodeCallbacks = getCallbacks(receivedCallbacks, callbackToNodes, ++nodeIndex);
            sharedContext = new TreeContext(newSharedState, newTransientState, context.request,
                    firstPageExecution
                            ? context.getAllCallbacks()
                            : nodeCallbacks.stream().map(cb -> cb.callback).collect(toList()));
        }
        return Pair.of(sharedContext, hasUnresolvedCallbacks ? left(resultCallbacks) : right(action.outcome));
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

    static class PageDecisionOutcomeProvider implements OutcomeProvider {
        public static final String PROPERTIES_FIELD = "_properties";
        private final Realm realm;
        private final NodeFactory nodeFactory;

        @Inject
        public PageDecisionOutcomeProvider(@Assisted Realm realm, NodeFactory nodeFactory) {
            this.realm = realm;
            this.nodeFactory = nodeFactory;
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            JsonValue nodesJson = nodeAttributes.get("nodes");
            if (nodesJson == null || nodesJson.size() == 0) {
                return Collections.emptyList();
            }
            ChildNodeConfig node;
            JsonValue nodeJson = nodesJson.get(nodesJson.size() - 1);
            if (nodeJson.isString()) {
                node = new ChildNodeConfig(nodeJson.asString());
            } else {
                node = new ChildNodeConfig(nodeJson);
            }
            JsonValue properties = nodeJson.get(PROPERTIES_FIELD);
            return nodeFactory.getOutcomeProvider(realm, node.nodeType)
                    .getOutcomes(locales, properties.isNull() ? json(object()) : properties);
        }
    }

    /**
     * Object view of the String representation of a child node that is stored in SMS.
     */
    public static class ChildNodeConfig {
        static final String ID_FIELD = "_id";
        static final String DISPLAY_NAME_FIELD = "displayName";
        static final String NODE_TYPE_FIELD = "nodeType";

        private final String nodeType;
        private final String nodeId;
        private final String displayName;

        @VisibleForTesting
        ChildNodeConfig(String nodeId, String nodeType, String displayName) {
            this.nodeId = nodeId;
            this.nodeType = nodeType;
            this.displayName = displayName;
        }

        /**
         * Construct child node config from an SMS storage representation.
         *
         * @param nodeConfig The string representation.
         */
        public ChildNodeConfig(String nodeConfig) {
            String[] split = nodeConfig.split(":");
            nodeId = split[0];
            nodeType = split[1];
            displayName = split[2];
        }

        /**
         * Construct child node config from the JSON representation.
         *
         * @param nodeJson The JSON representation.
         */
        public ChildNodeConfig(JsonValue nodeJson) {
            nodeId = nodeJson.get(ID_FIELD).asString();
            nodeType = nodeJson.get(NODE_TYPE_FIELD).asString();
            displayName = nodeJson.get(DISPLAY_NAME_FIELD).asString();
        }

        @Override
        public String toString() {
            return nodeId + ":" + nodeType + ":" + displayName;
        }

        /**
         * Get the child node configuration in JSON form.
         *
         * @return The JSON representation.
         */
        public JsonValue asJson() {
            return json(object(
                    field(ID_FIELD, nodeId),
                    field(NODE_TYPE_FIELD, nodeType),
                    field(DISPLAY_NAME_FIELD, displayName)
            ));
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
                ChildNodeConfig node = new ChildNodeConfig(nodeString);
                if (PROHIBITED_CHILD_NODE_TYPES.contains(node.nodeType)) {
                    throw new ServiceConfigException("Illegal child node type: " + node.nodeType);
                }
                try {
                    Optional<?> config = serviceRegistry.getRealmInstance(nodeRegistry.getConfigType(node.nodeType),
                            realm, node.nodeId);
                    if (config.isPresent()) {
                        List<OutcomeProvider.Outcome> outcomes = nodeFactory.getNodeOutcomes(new PreferredLocales(),
                                realm, node.nodeId, node.nodeType);
                        if (outcomes.size() == 0) {
                            throw new ServiceConfigException("Node does not have any outcomes: " + node.nodeType);
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
}
