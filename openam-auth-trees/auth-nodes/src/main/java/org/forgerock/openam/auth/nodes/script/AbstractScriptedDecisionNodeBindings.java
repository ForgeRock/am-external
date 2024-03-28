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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ACTION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_BUILDER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SECRETS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeStateScriptWrapper;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepositoryScriptWrapper;
import org.forgerock.openam.scripting.api.secrets.ScriptedSecrets;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.ScriptBindings;

/**
 * Abstract parent class of the scripted decision node bindings.
 */
public abstract class AbstractScriptedDecisionNodeBindings implements ScriptBindings {
    /**
     * Resumed from suspend key for bindings.
     */
    protected static final String RESUMED_FROM_SUSPEND = "resumedFromSuspend";
    private static final String JWT_ASSERTION = "jwtAssertion";
    private static final String JWT_VALIDATOR = "jwtValidator";
    /**
     * The {@link NodeState} binding.
     */
    protected final NodeState nodeState;
    /**
     * The callbacks binding.
     */
    protected final List<? extends Callback> callbacks;
    /**
     * The headers binding.
     */
    protected final Map<String, List<String>> headers;
    /**
     * The query parameters binding.
     */
    protected final Map<String, List<String>> queryParameters;
    /**
     * The identity repository binding.
     */
    protected final ScriptedIdentityRepository scriptedIdentityRepository;
    /**
     * The secrets binding.
     */
    protected final ScriptedSecrets secrets;
    /**
     * The resumed from suspend binding.
     */
    protected final boolean resumedFromSuspend;
    /**
     * The existing session binding.
     */
    protected final Object existingSession;

    /**
     * Constructor for the AbstractScriptedDecisionNodeBindings object.
     * @param builder the builder
     */

    protected AbstractScriptedDecisionNodeBindings(Builder<?> builder) {
        this.nodeState = builder.nodeState;
        this.callbacks = builder.callbacks;
        this.headers = builder.headers;
        this.queryParameters = builder.queryParameters;
        this.scriptedIdentityRepository = builder.scriptedIdentityRepository;
        this.secrets = builder.secrets;
        this.resumedFromSuspend = builder.resumedFromSuspend;
        this.existingSession = builder.existingSession;
    }

    /**
     * Generates a bindings map containing all common bindings for next gen scripts.
     *
     * @return a bindings map containing all common bindings for next gen scripts
     */
    public BindingsMap commonNextGenBindings() {
        BindingsMap bindingsMap = new BindingsMap();
        bindingsMap.put(STATE_IDENTIFIER, new NodeStateScriptWrapper(nodeState));
        bindingsMap.put(CALLBACKS_IDENTIFIER, new ScriptedCallbacksWrapper(callbacks));
        bindingsMap.put(HEADERS_IDENTIFIER, headers);
        bindingsMap.put(QUERY_PARAMETER_IDENTIFIER, queryParameters);
        bindingsMap.put(ID_REPO_IDENTIFIER, new ScriptedIdentityRepositoryScriptWrapper(scriptedIdentityRepository));
        bindingsMap.put(SECRETS_IDENTIFIER, secrets);
        bindingsMap.put(RESUMED_FROM_SUSPEND, resumedFromSuspend);
        bindingsMap.putIfDefined(EXISTING_SESSION, existingSession);
        bindingsMap.put(JWT_ASSERTION, new JwtAssertionScriptWrapper());
        bindingsMap.put(JWT_VALIDATOR, new JwtValidatorScriptWrapper());
        bindingsMap.put(ACTION, new ActionWrapper());
        bindingsMap.put(CALLBACKS_BUILDER, new ScriptedCallbacksBuilder());
        return bindingsMap;
    }

    /**
     * Step 1 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep0 {
        /**
         * Sets the {@link NodeState}.
         *
         * @param nodeState the {@link NodeState}
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep1 withNodeState(NodeState nodeState);
    }

    /**
     * Step 2 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep1 {
        /**
         * Sets the callbacks.
         *
         * @param callbacks the callbacks
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep2 withCallbacks(List<? extends Callback> callbacks);
    }

    /**
     * Step 3 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep2 {
        /**
         * Sets the headers.
         *
         * @param headers the headers
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep4 withHeaders(Map<String, List<String>> headers);
    }

    /**
     * Step 5 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep4 {
        /**
         * Sets the query parameters.
         *
         * @param queryParameters the query parameters
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep7 withQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     * Step 8 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep7 {
        /**
         * Sets the scripted identity repository.
         *
         * @param scriptedIdentityRepository the scripted identity repository
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep8 withScriptedIdentityRepository(
                ScriptedIdentityRepository scriptedIdentityRepository);
    }

    /**
     * Step 9 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep8 {
        /**
         * Sets the {@link ScriptedSecrets}.
         *
         * @param secrets the {@link ScriptedSecrets}
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep9 withSecrets(ScriptedSecrets secrets);
    }

    /**
     * Step 10 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep9 {
        /**
         * Sets the resumed from suspend boolean.
         *
         * @param resumedFromSuspend true if resumed from suspend
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep10 withResumedFromSuspend(boolean resumedFromSuspend);
    }

    /**
     * Step 11 of the builder.
     */
    public interface AbstractScriptedDecisionNodeBindingsStep10 {
        /**
         * Sets existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        Builder withExistingSession(Map<String, String> existingSession);
    }


    /**
     * Builder object to construct a {@link ScriptedDecisionNodeBindings}.
     * @param <T> the type of the builder.
     */
    public abstract static class Builder<T extends Builder<T>>
            implements AbstractScriptedDecisionNodeBindingsStep0, AbstractScriptedDecisionNodeBindingsStep1,
                               AbstractScriptedDecisionNodeBindingsStep2,
                               AbstractScriptedDecisionNodeBindingsStep4, AbstractScriptedDecisionNodeBindingsStep7,
                               AbstractScriptedDecisionNodeBindingsStep8, AbstractScriptedDecisionNodeBindingsStep9,
                               AbstractScriptedDecisionNodeBindingsStep10 {

        private NodeState nodeState;
        private ScriptedIdentityRepository scriptedIdentityRepository;
        private ScriptedSecrets secrets;
        private boolean resumedFromSuspend;
        private List<? extends Callback> callbacks;
        private Map<String, List<String>> queryParameters;
        private Map<String, List<String>> headers;
        private Map<String, String> existingSession;

        /**
         * Set the nodeState for the builder.
         *
         * @param nodeState The nodeState.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep1 withNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * Set the callbacks for the builder.
         *
         * @param callbacks The List of callbacks.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep2 withCallbacks(List<? extends Callback> callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        /**
         * Set the headers for the builder.
         *
         * @param headers The http call headers.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep4 withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Set the queryParameters for the builder.
         *
         * @param queryParameters The queryParameters of the request.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep7 withQueryParameters(
                Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        /**
         * Set the scriptedIdentityRepository for the builder.
         *
         * @param scriptedIdentityRepository The {@link ScriptedIdentityRepository}.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep8 withScriptedIdentityRepository(
                ScriptedIdentityRepository scriptedIdentityRepository) {
            this.scriptedIdentityRepository = scriptedIdentityRepository;
            return this;
        }

        /**
         * Set the secrets for the builder.
         *
         * @param secrets The {@link ScriptedSecrets}.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep9 withSecrets(ScriptedSecrets secrets) {
            this.secrets = secrets;
            return this;
        }

        /**
         * Set if the tree is resumedFromSuspend for the builder.
         *
         * @param resumedFromSuspend If the tree was resumed from suspended state.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep10 withResumedFromSuspend(boolean resumedFromSuspend) {
            this.resumedFromSuspend = resumedFromSuspend;
            return this;
        }

        /**
         * Set the existingSession.
         *
         * @param existingSession the existing session.
         * @return The next step of the Builder.
         */
        @Override
        public Builder withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Builds the {@link AbstractScriptedDecisionNodeBindings}.
         *
         * @return the {@link AbstractScriptedDecisionNodeBindings}.
         */
        public abstract AbstractScriptedDecisionNodeBindings build();

    }
}
