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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.emptyList;
import static org.forgerock.am.trees.api.Outcome.EXCEPTION;
import static org.forgerock.am.trees.api.Outcome.NEED_INPUT;
import static org.forgerock.am.trees.api.Outcome.SUSPENDED;
import static org.forgerock.am.trees.api.Outcome.TRUE;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.Action.suspend;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.CURRENT_NODE_ID;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.am.trees.api.NodeVisitor;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeExecutor;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.am.trees.model.TreeState;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.typeadapters.InnerTree;
import org.forgerock.openam.auth.nodes.framework.validators.InnerTreeConfigValidator;
import org.forgerock.openam.auth.nodes.helpers.AuthSessionHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;

/**
 * A node that evaluates another tree. The tree to evaluate is configurable.
 *
 * <p>When evaluating the inner tree, it is initially given the shared state passed to this node.
 * When the inner tree has finished evaluating, it's shared state is output from this node.</p>
 */
@Node.Metadata(outcomeProvider = InnerTreeEvaluatorNode.OutcomeProvider.class,
        configClass = InnerTreeEvaluatorNode.Config.class,
        configValidator = InnerTreeConfigValidator.class,
        tags = {"utilities"})
public class InnerTreeEvaluatorNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The ID of an authentication tree. Must not be a tree that creates a recursive loop.
         *
         * @return the ID.
         */
        @Attribute(order = 100)
        @InnerTree
        String tree();

        /**
         * Whether to return the error outcome in addition to the true and false outcomes.
         * @return whether to return the error outcome.
         */
        @Attribute(order = 200)
        default boolean displayErrorOutcome() {
            return false;
        }

    }

    private final Tree tree;
    private final TreeExecutor treeExecutor;
    private final Realm realm;
    private final Set<String> treeStateContainers;
    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(InnerTreeEvaluatorNode.class);
    private final AuthSessionHelper authSessionHelper;

    /**
     * Guice constructor.
     *
     * @param treeProvider     An auth tree service instance.
     * @param treeExecutor     An auth tree executor instance.
     * @param realm            The realm.
     * @param config           The configuration of this node.
     * @param treeStateContainers registered containers within node state.
     * @param authSessionHelper The auth session helper.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public InnerTreeEvaluatorNode(TreeProvider treeProvider, TreeExecutor treeExecutor,
            @Assisted Realm realm, @Assisted Config config,
            @Named("treeStateContainers") Set<String> treeStateContainers,
            AuthSessionHelper authSessionHelper)
            throws NodeProcessException {
        this.treeExecutor = treeExecutor;
        this.realm = realm;
        tree = treeProvider.getTree(realm, config.tree())
                .filter(Tree::isEnabled)
                .orElseThrow(() -> new NodeProcessException("Configured tree does not exist: " + config.tree()));
        this.treeStateContainers = treeStateContainers;
        this.authSessionHelper = authSessionHelper;
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("InnerTreeEvaluatorNode started");
        JsonValue sharedState = context.sharedState;
        JsonValue currentNodeId = sharedState.get(CURRENT_NODE_ID);
        boolean resumingInnerTree = context.hasResumedFromSuspend()
                || (currentNodeId.isNotNull() && context.hasCallbacks())
                || (currentNodeId.isNotNull() && UUID.fromString(currentNodeId.asString()) != null);
        logger.debug("resumingInnerTree {}", resumingInnerTree);

        TreeState innerTreeState = resumingInnerTree
                ? treeStateAfterCallbacksSubmitted(context)
                : TreeState.builder()
                        .withSharedState(sharedState)
                        .withTransientState(populateInnerTransientState(context))
                        .withUniversalId(context.universalId.orElse(null))
                        .build();

        TreeResult result = treeExecutor.process(realm, tree, innerTreeState,
                context.getAllCallbacks(), context.hasResumedFromSuspend(), context.request);

        if (result.outcome == NEED_INPUT || result.outcome == SUSPENDED) {
            logger.debug("innerTree need input");
            Action.ActionBuilder builder = (result.outcome == NEED_INPUT)
                    ? send(result.callbacks) : suspend(result.suspensionHandler, result.suspensionDuration);

            JsonValue newSharedState = result.treeState.toJson();
            return builder.replaceSharedState(newSharedState)
                    .addWebhooks(result.treeState.webhooks)
                    .addSessionHooks(result.treeState.sessionHooks)
                    .withHeader(result.getHeader())
                    .withDescription(result.getDescription())
                    .withStage(result.getStage())
                    .withMaxTreeDuration(result.treeState.maxTreeDuration.orElse(null))
                    .build();
        } else if (result.outcome == EXCEPTION) {
            if (config.displayErrorOutcome()) {
                var failedNode = tree.getNodeForId(result.treeState.currentNodeId);
                result.treeState.sharedState.put("errorNode", json(object(
                        field("id", failedNode.id().toString()),
                        field("displayName", failedNode.displayName()),
                        field("type", failedNode.type())
                )));
                result.treeState.sharedState.put("errorMessage", result.cause.getMessage());
                logger.debug("Inner tree failed with exception", result.cause);
                return goToOutcome(Outcome.ERROR, result);
            } else {
                throw result.cause;
            }
        } else {
            return goToOutcome(result.outcome == TRUE ? Outcome.TRUE : Outcome.FALSE,
                    result);
        }
    }

    private Action goToOutcome(Outcome outcome, TreeResult result) {
        logger.debug("innerTree outcome {}", result.outcome);
        var actionBuilder = Action.goTo(outcome.id)
                                    .replaceSharedState(result.treeState.sharedState)
                                    .addWebhooks(result.treeState.webhooks)
                                    .addSessionHooks(result.treeState.sessionHooks);
        result.treeState.sessionProperties.properties().forEach(actionBuilder::putSessionProperty);
        result.treeState.sessionProperties.maxSessionTime().ifPresent(actionBuilder::withMaxSessionTime);
        result.treeState.sessionProperties.maxIdleTime().ifPresent(actionBuilder::withMaxIdleTime);
        result.treeState.maxTreeDuration.ifPresent(actionBuilder::withMaxTreeDuration);
        return actionBuilder.build();
    }

    private TreeState treeStateAfterCallbacksSubmitted(TreeContext context) throws NodeProcessException {
        return TreeState.fromJson(context.sharedState, getCurrentMaxSessionTime(context.request.authId));
    }

    private Duration getCurrentMaxSessionTime(String authId) throws NodeProcessException {
        if (isBlank(authId)) {
            return null;
        }
        try {
            return Duration.ofMinutes(authSessionHelper.getAuthSession(authId).getMaxSessionTime());
        } catch (SessionException e) {
            throw new NodeProcessException("Unable to update the journey timeout", e);
        }
    }

    private JsonValue populateInnerTransientState(TreeContext context) throws NodeProcessException {
        JsonValue transientState = json(object());
        List<InputState> innerInputs = tree.visitNodes(new InputCollector());
        innerInputs.stream()
                .filter(input -> getTransientState(context, input.name).isNotNull())
                .forEach(input -> transientState.put(input.name, getTransientState(context, input.name).getObject()));

        treeStateContainers.forEach(container -> {
            if (getTransientState(context, container) != null) {
                Map<String, Object> objectAttributes = object();
                innerInputs.stream()
                        .filter(input -> getTransientState(context, container).get(input.name).isNotNull())
                        .forEach(input -> objectAttributes.put(input.name, getTransientState(context, container)
                                                                                   .get(input.name).getObject()));
                if (!objectAttributes.isEmpty()) {
                    transientState.put(container, objectAttributes);
                }
            }
        });
        return transientState;
    }

    private JsonValue getTransientState(final TreeContext context, final String name) {
        return context.transientState.get(name)
                .defaultTo(context.getSecureState(name));
    }

    /**
     * Returns the inner tree to evaluate.
     *
     * @return the inner tree.
     */
    public Tree getTree() {
        return tree;
    }

    @Override
    public InputState[] getInputs() {
        try {
            return tree.visitNodes(new InputCollector()).toArray(new InputState[] {});
        } catch (NodeProcessException e) {
            logger.error("Exception when gathering inner tree inputs", e);
            return new InputState[] {};
        }
    }

    /**
     * A visitor that collects the inputs of the inner tree.
     */
    static class InputCollector implements NodeVisitor<List<InputState>> {

        @Override
        public List<InputState> visit(UUID nodeId, Optional<Class<? extends Node>> nodeType,
                Optional<String> displayName, Supplier<Node> node, Stream<Supplier<List<InputState>>> nextVisitings)
                throws NodeProcessException {
            if (node.get() != null) {
                return mergeInputs(
                        Arrays.stream(node.get().getInputs())
                                .collect(Collectors.toList()),
                        nextVisitings
                                .map(Supplier::get)
                                .reduce(new ArrayList<>(), this::mergeInputs));
            } else {
                return emptyList();
            }
        }

        private List<InputState> mergeInputs(List<InputState> left, List<InputState> right) {
            List<InputState> merged = new ArrayList<>();

            // Avoid duplicating inputs by picking only unique names or by selecting a single input from matches.
            // Matching input names are differentiated by requirement or selected from the first list.
            merged.addAll(left.stream()
                    .filter(input1 -> right.stream()
                            .noneMatch(input2 -> input1.name.equals(input2.name)
                                    && !input1.required
                                    && input2.required))
                    .toList());

            merged.addAll(right.stream()
                    .filter(input2 -> merged.stream()
                            .noneMatch(input1 -> input2.name.equals(input1.name)))
                    .toList());
            return merged;
        }
    }

    private enum Outcome {
        TRUE("true", "trueOutcome"),
        FALSE("false", "falseOutcome"),
        ERROR("error", "errorOutcome");

        final String id;
        final String bundleKey;

        Outcome(String id, String bundleKey) {
            this.id = id;
            this.bundleKey = bundleKey;
        }
    }

    /**
     * An outcome provider for the inner tree node.
     */
    public static class OutcomeProvider implements BoundedOutcomeProvider {
        private static final String BUNDLE = InnerTreeEvaluatorNode.class.getName();
        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            final var bundle = locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
            return Arrays.stream(InnerTreeEvaluatorNode.Outcome.values())
                           .map(outcome -> new Outcome(outcome.id, bundle.getString(outcome.bundleKey)))
                           .toList();
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            if (nodeAttributes == null || nodeAttributes.get("displayErrorOutcome").defaultTo(false).asBoolean()) {
                return getAllOutcomes(locales);
            }
            return getAllOutcomes(locales).stream()
                           .filter(outcome -> !outcome.id.equals(InnerTreeEvaluatorNode.Outcome.ERROR.id))
                           .toList();
        }
    }
}
