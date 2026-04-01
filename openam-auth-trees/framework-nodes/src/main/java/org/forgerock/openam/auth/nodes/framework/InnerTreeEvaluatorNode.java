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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeExecutor;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.am.trees.model.TreeState;
import org.forgerock.am.trees.model.TreeStateFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.PageNodeCompatibility;
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
        tags = {"utilities"},
        pageNodeCompatibility = PageNodeCompatibility.UNSUPPORTED)
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
    private final InputStatesCache inputStatesCache;

    /**
     * Guice constructor.
     *
     * @param treeProvider     An auth tree service instance.
     * @param treeExecutor     An auth tree executor instance.
     * @param realm            The realm.
     * @param config           The configuration of this node.
     * @param treeStateContainers registered containers within node state.
     * @param authSessionHelper The auth session helper.
     * @param inputStatesCache The tree input states cache.
     *
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public InnerTreeEvaluatorNode(TreeProvider treeProvider, TreeExecutor treeExecutor,
            @Assisted Realm realm, @Assisted Config config,
            @Named("treeStateContainers") Set<String> treeStateContainers,
            AuthSessionHelper authSessionHelper,
            InputStatesCache inputStatesCache)
            throws NodeProcessException {
        this.treeExecutor = treeExecutor;
        this.realm = realm;
        tree = treeProvider.getTree(realm, config.tree())
                .filter(Tree::isEnabled)
                .orElseThrow(() -> new NodeProcessException("Configured tree does not exist: " + config.tree()));
        this.treeStateContainers = treeStateContainers;
        this.authSessionHelper = authSessionHelper;
        this.inputStatesCache = inputStatesCache;
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
                : TreeStateFactory.builder()
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
                    .addWebhooks(result.treeState.webhooks())
                    .addSessionHooks(result.treeState.sessionHooks())
                    .withHeader(result.getHeader())
                    .withDescription(result.getDescription())
                    .withStage(result.getStage())
                    .withMaxTreeDuration(result.treeState.maxTreeDuration().orElse(null))
                    .build();
        } else if (result.outcome == EXCEPTION) {
            if (config.displayErrorOutcome()) {
                var failedNode = tree.getNodeForId(result.treeState.currentNodeId());
                result.treeState.sharedState().put("errorNode", json(object(
                        field("id", failedNode.id().toString()),
                        field("displayName", failedNode.displayName()),
                        field("type", failedNode.type())
                )));
                result.treeState.sharedState().put("errorMessage", result.cause.getMessage());
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
                                    .replaceSharedState(result.treeState.sharedState())
                                    .addWebhooks(result.treeState.webhooks())
                                    .addSessionHooks(result.treeState.sessionHooks());
        result.treeState.sessionProperties().properties().forEach(actionBuilder::putSessionProperty);
        result.treeState.sessionProperties().maxSessionTime().ifPresent(actionBuilder::withMaxSessionTime);
        result.treeState.sessionProperties().maxIdleTime().ifPresent(actionBuilder::withMaxIdleTime);
        result.treeState.maxTreeDuration().ifPresent(actionBuilder::withMaxTreeDuration);
        return actionBuilder.build();
    }

    private TreeState treeStateAfterCallbacksSubmitted(TreeContext context) throws NodeProcessException {
        TreeState treeState = TreeStateFactory.fromJson(context.sharedState,
                getCurrentMaxSessionTime(context.request.authId));
        if (treeState.sessionHooks() != null) {
            treeState.sessionHooks().clear();
        }
        if (treeState.webhooks() != null) {
            treeState.webhooks().clear();
        }
        return treeState;
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
        List<InputState> innerInputs = getCachedInputs();
        if (innerInputs.isEmpty()) {
            return transientState;
        }
        for (InputState input : innerInputs) {
            JsonValue val = getTransientState(context, input.name);
            if (val.isNotNull()) {
                transientState.put(input.name, val.getObject());
            }
        }
        for (String container : treeStateContainers) {
            JsonValue containerVal = getTransientState(context, container);
            if (containerVal.isNotNull()) {
                Map<String, Object> attrs = new HashMap<>();
                for (InputState input : innerInputs) {
                    JsonValue subVal = containerVal.get(input.name);
                    if (subVal.isNotNull()) {
                        attrs.put(input.name, subVal.getObject());
                    }
                }
                if (!attrs.isEmpty()) {
                    transientState.put(container, attrs);
                }
            }
        }
        return transientState;
    }

    private List<InputState> getCachedInputs() throws NodeProcessException {
        try {
            return inputStatesCache.get(tree);
        } catch (ExecutionException e) {
            String message = "Error loading tree inputs into cache";
            if (e.getCause() instanceof NodeProcessException) {
                logger.error(message, e.getCause());
                throw (NodeProcessException) e.getCause();
            }
            logger.error(message, e);
            throw new NodeProcessException(message, e);
        }
    }

    private JsonValue getTransientState(final TreeContext context, final String name) {
        return context.transientState.get(name).defaultTo(context.getSecureState(name));
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
            return getCachedInputs().toArray(new InputState[0]);
        } catch (NodeProcessException e) {
            logger.error("Exception when gathering inner tree inputs", e);
            return new InputState[] {};
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
