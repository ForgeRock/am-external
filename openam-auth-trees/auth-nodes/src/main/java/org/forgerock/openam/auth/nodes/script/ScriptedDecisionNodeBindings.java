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

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HTTP_CLIENT_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SECRETS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SHARED_STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.TRANSIENT_STATE_IDENTIFIER;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the ScriptedDecisionNode script.
 */
public final class ScriptedDecisionNodeBindings extends AbstractScriptedDecisionNodeBindings {

    private final Object sharedState;
    private final Object transientState;
    private final ChfHttpClient httpClient;
    private final ScriptIdentityRepository scriptIdentityRepository;


    private ScriptedDecisionNodeBindings(Builder builder) {
        super(builder);
        this.sharedState = builder.sharedState;
        this.transientState = builder.transientState;
        this.httpClient = builder.httpClient;
        this.scriptIdentityRepository = builder.scriptIdentityRepository;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the Builder.
     */
    public static ScriptedDecisionNodeBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(STATE_IDENTIFIER, nodeState);
        bindings.put(CALLBACKS_IDENTIFIER, callbacks);
        bindings.put(HEADERS_IDENTIFIER, headers);
        bindings.put(QUERY_PARAMETER_IDENTIFIER, queryParameters);
        bindings.put(HTTP_CLIENT_IDENTIFIER, httpClient);
        bindings.put(ID_REPO_IDENTIFIER, scriptIdentityRepository);
        bindings.put(SECRETS_IDENTIFIER, secrets);
        bindings.put(RESUMED_FROM_SUSPEND, resumedFromSuspend);
        bindings.put(SHARED_STATE_IDENTIFIER, sharedState);
        bindings.put(TRANSIENT_STATE_IDENTIFIER, transientState);
        bindings.putIfDefined(EXISTING_SESSION, existingSession);
        return bindings;
    }

    @Override
    public BindingsMap nextGenBindings() {
        return commonNextGenBindings();
    }

    /**
     * Step 1 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep1 {
        /**
         * Sets the shared state.
         *
         * @param sharedState the shared state
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep2 withSharedState(Object sharedState);
    }

    /**
     * Step 2 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep2 {
        /**
         * Sets the transient state.
         *
         * @param transientState the transient state
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep3 withTransientState(Object transientState);
    }

    /**
     * Step 6 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep3 {
        /**
         * Sets the {@link ChfHttpClient}.
         *
         * @param httpClient the {@link ChfHttpClient}
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep4 withHttpClient(ChfHttpClient httpClient);
    }

    /**
     * Step 7 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep4 {
        /**
         * Sets the script identity repository.
         *
         * @param scriptIdentityRepository the script identity repository
         * @return the next step of the {@link Builder}
         */
        AbstractScriptedDecisionNodeBindingsStep0 withScriptIdentityRepository(
                ScriptIdentityRepository scriptIdentityRepository);
    }


    /**
     * Builder object to construct a {@link ScriptedDecisionNodeBindings}.
     */
    public static final class Builder extends AbstractScriptedDecisionNodeBindings.Builder<Builder>
            implements ScriptedDecisionNodeBindingsStep1, ScriptedDecisionNodeBindingsStep2,
                               ScriptedDecisionNodeBindingsStep3, ScriptedDecisionNodeBindingsStep4 {

        private Object sharedState;
        private Object transientState;
        private ScriptIdentityRepository scriptIdentityRepository;
        private ChfHttpClient httpClient;

        /**
         * Builds the {@link ScriptedDecisionNodeBindings}.
         *
         * @return the {@link ScriptedDecisionNodeBindings}.
         */
        @Override
        public ScriptedDecisionNodeBindings build() {
            return new ScriptedDecisionNodeBindings(this);
        }

        /**
         * Set the sharedState.
         *
         * @param sharedState the sharedState.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep2 withSharedState(Object sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Set the transientState.
         *
         * @param transientState the transientState.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep3 withTransientState(Object transientState) {
            this.transientState = transientState;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link ChfHttpClient}.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep4 withHttpClient(ChfHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set the scriptIdentityRepository for the builder.
         *
         * @param scriptIdentityRepository The {@link ScriptIdentityRepository}.
         * @return The next step of the Builder.
         */
        @Override
        public AbstractScriptedDecisionNodeBindingsStep0 withScriptIdentityRepository(
                ScriptIdentityRepository scriptIdentityRepository) {
            this.scriptIdentityRepository = scriptIdentityRepository;
            return this;
        }


    }
}
