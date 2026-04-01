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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.ACTION;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.CALLBACKS_BUILDER;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.OUTCOME_IDENTIFIER;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.ScriptedLocalizedMessage;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.cache.service.CacheManagerService;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for scripted decision nodes. The purpose of this class is to couple the implementation
 * of {@link org.forgerock.openam.auth.nodes.ScriptedDecisionNode} with its node designer counterpart
 * {@link org.forgerock.openam.auth.nodes.designer.node.AbstractScriptedNodeDesignerNode} to prevent bugs caused by
 * deviation.
 * @param <C> The type of the node-specific configuration object
 * @param <B> The type of the script bindings, which must extend {@link BaseScriptedDecisionNodeBindings}
 */
public abstract class AbstractScriptedNode<C, B extends BaseScriptedDecisionNodeBindings> implements Node {

    /**
     * Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The realm the node is in.
     */
    protected final Realm realm;
    /**
     * A repository to retrieve user information within a scripting module's script.
     */
    protected final ScriptedIdentityRepository scriptedIdentityRepo;
    /**
     * Utilities for scripted nodes.
     */
    protected final AuthScriptUtilities authScriptUtils;
    /**
     * The tree that contains the node.
     */
    protected final Tree tree;
    /**
     * The cache manager service.
     */
    protected final CacheManagerService cacheManagerService;
    /**
     * Component for evaluating scripts in one of the supported scripting languages.
     */
    protected final ScriptEvaluator<B> scriptEvaluator;
    /**
     * The scripted localized message factory.
     */
    protected final ScriptedLocalizedMessage.Factory scriptedLocalizedMessageFactory;
    /**
     * The node-specific configuration object.
     */
    protected final C config;

    private JsonValue auditEntryDetail;

    private final UUID nodeId;

    /**
     * Guice-injected constructor.
     *
     * @param realm The realm the node is in.
     * @param config The node configuration.
     * @param tree The tree that contains this node.
     * @param scriptEvaluator The script evaluator instance for this node.
     * @param scriptedIdentityRepositoryFactory Factory used to create the scripted identity repository.
     * @param authScriptUtils Utilities for scripted nodes.
     * @param cacheManagerServiceFactory Factory used to create the cache manager service.
     * @param scriptedLocalizedMessageFactory The scripted localized message factory.
     * @param nodeId The unique ID of the node.
     */
    public AbstractScriptedNode(Realm realm, UUID nodeId, C config, Tree tree, ScriptEvaluator<B> scriptEvaluator,
                                ScriptedIdentityRepository.Factory scriptedIdentityRepositoryFactory,
                                AuthScriptUtilities authScriptUtils,
                                CacheManagerService.Factory cacheManagerServiceFactory,
                                ScriptedLocalizedMessage.Factory scriptedLocalizedMessageFactory) {
        this.realm = realm;
        this.config = config;
        this.tree = tree;
        this.scriptEvaluator = scriptEvaluator;
        this.scriptedIdentityRepo = scriptedIdentityRepositoryFactory.create(realm);
        this.authScriptUtils = authScriptUtils;
        this.cacheManagerService = cacheManagerServiceFactory.create(realm);
        this.scriptedLocalizedMessageFactory = scriptedLocalizedMessageFactory;
        this.nodeId = nodeId;
    }

    /**
     * Provides the {@link Script} that this node will execute.
     * @return The {@code Script} instance
     */
    protected abstract Script getScript();

    /**
     * Provides the list of valid outcomes for this node.
     * @return A list of valid outcome strings
     */
    protected abstract List<String> getOutcomes();

    /**
     * Creates and populates the specific script bindings object required by the script.
     * @param context The current {@link TreeContext}
     * @return The populated script bindings object of type {@code B}
     * @throws NodeProcessException If the bindings cannot be created
     */
    protected abstract B createScriptBindings(TreeContext context) throws NodeProcessException;

    /**
     * Provides the {@link EvaluatorVersion} of the script.
     * @return The script's {@code EvaluatorVersion}
     */
    protected abstract EvaluatorVersion getEvaluatorVersion();

    /**
     * A hook method called after the script has been successfully evaluated, but before the outcome is processed.
     * @param context The current {@link TreeContext}
     * @param bindings The script bindings <i>after</i> script execution
     * @throws NodeProcessException If an error occurs during post-processing
     */
    protected void afterScriptEvaluation(TreeContext context, Bindings bindings) throws NodeProcessException { }

    /**
     * Handles exceptions thrown during script evaluation.
     * @param e The {@link ScriptException} caught during evaluation
     * @return An {@link Action} to take if the exception is handled gracefully
     * @throws NodeProcessException By default, or if the exception cannot be handled
     */
    protected Action handleScriptException(ScriptException e) throws NodeProcessException {
        logger.warn("error evaluating the script", e);
        throw new NodeProcessException(e);
    }

    /**
     * Handles exceptions thrown during node processing.
     * @param e The {@link NodeProcessException} exception that was caught
     * @return An {@link Action} to take if the exception is handled gracefully
     * @throws NodeProcessException By default, or if the exception cannot be handled
     */
    protected Action handleProcessingException(NodeProcessException e) throws NodeProcessException {
        logger.warn("An error occurred during scripted node processing", e);
        throw e;
    }

    /**
     * Implements the main processing logic for the node. This method defines the execution skeleton:
     * <ol>
     *     <li>Creates script bindings via {@link #createScriptBindings(TreeContext)}</li>
     *     <li>Evaluates the script provided by {@link #getScript()}</li>
     *     <li>Handles any script exceptions via {@link #handleScriptException(ScriptException)}</li>
     *     <li>Calls the {@link #afterScriptEvaluation(TreeContext, Bindings)} hook for post-processing</li>
     *     <li>Populates audit details from the script result</li>
     *     <li>Processes the script result to determine the next {@link Action}</li>
     * </ol>
     *
     * @param context The context of the tree authentication
     * @return The {@link Action} to be taken
     * @throws NodeProcessException If a non-recoverable error occurs during processing
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        try {
            B bindings = createScriptBindings(context);
            Bindings outputBindings;
            try {
                ScriptResult<Object> scriptResult = scriptEvaluator.evaluateScript(getScript(), bindings, realm);
                outputBindings = scriptResult.getBindings();
            } catch (ScriptException e) {
                return handleScriptException(e);
            }

            afterScriptEvaluation(context, outputBindings);
            auditEntryDetail = authScriptUtils.getAuditEntryDetails(outputBindings);

            Object actionResult = outputBindings.get(ACTION);
            Object callbacksBuilder = outputBindings.get(CALLBACKS_BUILDER);
            Optional<Action> maybeAction = authScriptUtils.getAction(actionResult, getEvaluatorVersion(),
                    callbacksBuilder);
            if (maybeAction.isPresent()) {
                Action action = maybeAction.get();
                if (!action.sendingCallbacks() && !action.hasSuspensionHandler()
                        && !getOutcomes().contains(action.outcome)) {
                    logger.warn("invalid script outcome {} in action", action.outcome);
                    throw new NodeProcessException(String.format("Invalid outcome from script: '%s'", action.outcome));
                }
                return action;
            }
            Action.ActionBuilder actionBuilder =
                    goTo(authScriptUtils.getOutcome(outputBindings.get(OUTCOME_IDENTIFIER), getOutcomes()));
            return actionBuilder.build();
        } catch (NodeProcessException e) {
            return handleProcessingException(e);
        }

    }

    /**
     * Populates the common bindings shared across scripted decision node implementations.
     * @param builder The builder instance, at the first step
     * @param context The current {@link TreeContext}
     * @return The stepped bindings builder
     */
    protected BaseScriptedDecisionNodeBindings.BaseScriptedDecisionNodeBindingsFinalStep<B> populateCommonBindings(
            BaseScriptedDecisionNodeBindings.BaseScriptedDecisionNodeBindingsStep1<B> builder, TreeContext context)
            throws NodeProcessException {
        return builder.withNodeState(context.getStateFor(this))
                .withCallbacks(context.getAllCallbacks())
                .withHeaders(authScriptUtils.convertHeadersToModifiableObjects(context.request.headers))
                .withQueryParameters(authScriptUtils.convertParametersToModifiableObjects(context.request.parameters))
                .withScriptedIdentityRepository(scriptedIdentityRepo)
                .withResumedFromSuspend(context.hasResumedFromSuspend())
                .withExistingSession(authScriptUtils.getSessionProperties(context.request.ssoTokenId))
                .withRequestCookies(context.request.cookies)
                .withTree(tree)
                .withSamlApplication(authScriptUtils.getSAMLData(context).orElse(null))
                .withOauthApplication(authScriptUtils.getOauthDecisionNodeApplication(context).orElse(null))
                .withScriptedLocalizedMessage(scriptedLocalizedMessageFactory.create(realm, context));
    }

    /**
     * Provides audit entry details.
     * @return A {@link JsonValue} containing audit entry details.
     */
    @Override
    public JsonValue getAuditEntryDetail() {
        return Objects.requireNonNullElseGet(auditEntryDetail, () -> json(object()));
    }
}
