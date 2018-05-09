/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework;

import static org.forgerock.openam.auth.node.api.Action.send;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.typeadapters.InnerTree;
import org.forgerock.openam.auth.nodes.framework.validators.InnerTreeConfigValidator;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeExecutor;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.auth.trees.engine.Outcome;
import org.forgerock.openam.auth.trees.engine.TreeResult;
import org.forgerock.openam.auth.trees.engine.TreeState;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that evaluates another tree. The tree to evaluate is configurable.
 *
 * <p>When evaluating the inner tree, it is initially given the shared state passed to this node.
 * When the inner tree has finished evaluating, it's shared state is output from this node.</p>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = InnerTreeEvaluatorNode.Config.class,
        configValidator = InnerTreeConfigValidator.class)
public class InnerTreeEvaluatorNode extends AbstractDecisionNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The ID of an authentication tree. Must not be a tree that creates a recursive loop.
         * @return the ID.
         */
        @Attribute(order = 100)
        @InnerTree
        String tree();
    }

    /** The tree's shared state. **/
    public static final String SHARED_STATE = "sharedState";

    private final AuthTree tree;
    private final AuthTreeExecutor authTreeExecutor;
    private final Realm realm;
    private final Logger logger = LoggerFactory.getLogger("amAuth");


    /**
     * Guice constructor.
     *
     * @param authTreeService An auth tree service instance.
     * @param authTreeExecutor An auth tree executor instance.
     * @param realm The realm.
     * @param config The configuration of this node.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public InnerTreeEvaluatorNode(AuthTreeService authTreeService, AuthTreeExecutor authTreeExecutor,
                                  @Assisted Realm realm, @Assisted Config config) throws NodeProcessException {
        this.authTreeExecutor = authTreeExecutor;
        this.realm = realm;
        tree = authTreeService.getTree(realm, config.tree())
                .orElseThrow(() -> new NodeProcessException("Configured tree does not exist: " + config.tree()));
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("InnerTreeEvaluatorNode started");
        boolean resumingSubtree = context.hasCallbacks();
        logger.debug("resumingSubtree {}", resumingSubtree);
        JsonValue sharedState = context.sharedState;
        TreeState innerTreeState = resumingSubtree
                ? treeStateAfterCallbacksSubmitted(context)
                : new TreeState(sharedState, null);
        TreeResult result = authTreeExecutor.process(realm, tree, innerTreeState, context.getAllCallbacks(),
                context.request);
        if (result.outcome == Outcome.NEED_INPUT) {
            logger.debug("innerTree need input");
            JsonValue newSharedState = result.treeState.toJson();
            return send(result.callbacks)
                    .replaceSharedState(newSharedState)
                    .addWebhooks(result.treeState.webhooks)
                    .addSessionHooks(result.treeState.sessionHooks)
                    .build();
        } else {
            logger.debug("innerTree outcome {}", result.outcome);
            Action.ActionBuilder actionBuilder =
                    goTo(result.outcome == Outcome.TRUE)
                            .replaceSharedState(result.treeState.sharedState)
                            .addWebhooks(result.treeState.webhooks)
                            .addSessionHooks(result.treeState.sessionHooks);
            result.treeState.sessionProperties.forEach((k, v) -> {
                if (v == null) {
                    actionBuilder.removeSessionProperty(k);
                } else {
                    actionBuilder.putSessionProperty(k, v);
                }
            });
            return actionBuilder.build();
        }
    }

    private TreeState treeStateAfterCallbacksSubmitted(TreeContext context) {
        return TreeState.fromJson(context.sharedState);
    }

    /**
     * Returns the inner tree to evaluate.
     *
     * @return the inner tree.
     */
    public AuthTree getTree() {
        return tree;
    }
}
