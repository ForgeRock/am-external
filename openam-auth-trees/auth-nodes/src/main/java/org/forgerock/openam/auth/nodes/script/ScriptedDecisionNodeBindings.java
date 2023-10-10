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
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HTTP_CLIENT_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.IDM_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.REALM_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SECRETS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SHARED_STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.TRANSIENT_STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.http.Client;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeStateScriptWrapper;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.IdmIntegrationServiceScriptWrapper;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepositoryScriptWrapper;
import org.forgerock.openam.scripting.api.secrets.ScriptedSecrets;
import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the ScriptedDecisionNode script.
 */
public final class ScriptedDecisionNodeBindings extends ScriptBindings {

    private static final String JWT_ASSERTION = "jwtAssertion";
    private static final String JWT_VALIDATOR = "jwtValidator";
    private static final String RESUMED_FROM_SUSPEND = "resumedFromSuspend";

    private final NodeState nodeState;
    private final List<? extends Callback> callbacks;
    private final Map<String, List<String>> headers;
    private final String realm;
    private final Map<String, List<String>> queryParameters;
    private final ChfHttpClient httpClient;
    private final ScriptIdentityRepository scriptIdentityRepository;
    private final ScriptedIdentityRepository scriptedIdentityRepository;
    private final ScriptedSecrets secrets;
    private final boolean resumedFromSuspend;
    private final Object existingSession;
    private final Object sharedState;
    private final Object transientState;

    private final IdmIntegrationServiceScriptWrapper idmIntegrationServiceScriptWrapper;

    private final Client client;

    private ScriptedDecisionNodeBindings(Builder builder) {
        super(builder);
        this.nodeState = builder.nodeState;
        this.callbacks = builder.callbacks;
        this.headers = builder.headers;
        this.realm = builder.realm;
        this.queryParameters = builder.queryParameters;
        this.httpClient = builder.httpClient;
        this.scriptIdentityRepository = builder.scriptIdentityRepository;
        this.scriptedIdentityRepository = builder.scriptedIdentityRepository;
        this.secrets = builder.secrets;
        this.resumedFromSuspend = builder.resumedFromSuspend;
        this.sharedState = builder.sharedState;
        this.transientState = builder.transientState;
        this.existingSession = builder.existingSession;
        this.client = builder.client;
        this.idmIntegrationServiceScriptWrapper = builder.idmIntegrationServiceScriptWrapper;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the Builder.
     */
    public static ScriptedDecisionNodeBindingsStep1 builder() {
        return new Builder();
    }

    /**
     * The signature of these bindings. Used to provide information about available bindings via REST without the
     * stateful underlying objects.
     *
     * @return The signature of this ScriptBindings implementation.
     */
    public static ScriptBindings signature() {
        return new Builder().signature();
    }

    @Override
    public String getDisplayName() {
        return "Scripted Decision Node Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        return List.of(
                Binding.of(STATE_IDENTIFIER, nodeState, NodeState.class),
                Binding.of(CALLBACKS_IDENTIFIER, callbacks, List.class),
                Binding.of(HEADERS_IDENTIFIER, headers, Map.class),
                Binding.of(REALM_IDENTIFIER, realm, String.class),
                Binding.of(QUERY_PARAMETER_IDENTIFIER, queryParameters, Map.class),
                Binding.of(HTTP_CLIENT_IDENTIFIER, httpClient, ChfHttpClient.class),
                Binding.of(ID_REPO_IDENTIFIER, scriptIdentityRepository, ScriptIdentityRepository.class),
                Binding.of(SECRETS_IDENTIFIER, secrets, ScriptedSecrets.class),
                Binding.of(RESUMED_FROM_SUSPEND, resumedFromSuspend, Boolean.class),
                Binding.ofMayBeUndefined(EXISTING_SESSION, existingSession, Map.class),
                Binding.of(SHARED_STATE_IDENTIFIER, sharedState, Map.class),
                Binding.of(TRANSIENT_STATE_IDENTIFIER, transientState, Map.class)
        );
    }

    @Override
    protected List<Binding> additionalV2Bindings() {
        return List.of(
                Binding.of(STATE_IDENTIFIER, new NodeStateScriptWrapper(nodeState), NodeStateScriptWrapper.class),
                Binding.of(CALLBACKS_IDENTIFIER, new ScriptedCallbacksWrapper(callbacks),
                        ScriptedCallbacksWrapper.class),
                Binding.of(HEADERS_IDENTIFIER, headers, Map.class),
                Binding.of(REALM_IDENTIFIER, realm, String.class),
                Binding.of(QUERY_PARAMETER_IDENTIFIER, queryParameters, Map.class),
                Binding.of(HTTP_CLIENT_IDENTIFIER, new HttpClientScriptWrapper(client), HttpClientScriptWrapper.class),
                Binding.of(ID_REPO_IDENTIFIER, new ScriptedIdentityRepositoryScriptWrapper(scriptedIdentityRepository),
                        ScriptedIdentityRepositoryScriptWrapper.class),
                Binding.of(SECRETS_IDENTIFIER, secrets, ScriptedSecrets.class),
                Binding.of(RESUMED_FROM_SUSPEND, resumedFromSuspend, Boolean.class),
                Binding.ofMayBeUndefined(EXISTING_SESSION, existingSession, Map.class),
                Binding.of(JWT_ASSERTION, new JwtAssertionScriptWrapper(), JwtAssertionScriptWrapper.class),
                Binding.of(JWT_VALIDATOR, new JwtValidatorScriptWrapper(), JwtValidatorScriptWrapper.class),
                Binding.of(IDM_IDENTIFIER, idmIntegrationServiceScriptWrapper,
                        IdmIntegrationServiceScriptWrapper.class),
                Binding.of(ACTION, new ActionWrapper(), ActionWrapper.class),
                Binding.of(CALLBACKS_BUILDER, new ScriptedCallbacksBuilder(), ScriptedCallbacksBuilder.class)
        );
    }

    /**
     * Step 1 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep1 {
        /**
         * Sets the {@link NodeState}.
         *
         * @param nodeState the {@link NodeState}
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep2 withNodeState(NodeState nodeState);
    }

    /**
     * Step 2 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep2 {
        /**
         * Sets the callbacks.
         *
         * @param callbacks the callbacks
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep3 withCallbacks(List<? extends Callback> callbacks);
    }

    /**
     * Step 3 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep3 {
        /**
         * Sets the headers.
         *
         * @param headers the headers
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep4 withHeaders(Map<String, List<String>> headers);
    }

    /**
     * Step 4 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep4 {
        /**
         * Sets the realm.
         *
         * @param realm the realm
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep5 withRealm(String realm);
    }

    /**
     * Step 5 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep5 {
        /**
         * Sets the query parameters.
         *
         * @param queryParameters the query parameters
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep6 withQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     * Step 6 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep6 {
        /**
         * Sets the {@link ChfHttpClient}.
         *
         * @param httpClient the {@link ChfHttpClient}
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep7 withHttpClient(ChfHttpClient httpClient);
    }

    /**
     * Step 7 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep7 {
        /**
         * Sets the script identity repository.
         *
         * @param scriptIdentityRepository the script identity repository
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep8 withScriptIdentityRepository(
                ScriptIdentityRepository scriptIdentityRepository);
    }

    /**
     * Step 8 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep8 {
        /**
         * Sets the scripted identity repository.
         *
         * @param scriptedIdentityRepository the scripted identity repository
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep9 withScriptedIdentityRepository(
                ScriptedIdentityRepository scriptedIdentityRepository);
    }

    /**
     * Step 9 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep9 {
        /**
         * Sets the {@link ScriptedSecrets}.
         *
         * @param secrets the {@link ScriptedSecrets}
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep10 withSecrets(ScriptedSecrets secrets);
    }

    /**
     * Step 10 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep10 {
        /**
         * Sets the resumed from suspend boolean.
         *
         * @param resumedFromSuspend true if resumed from suspend
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep11 withResumedFromSuspend(boolean resumedFromSuspend);
    }

    /**
     * Step 11 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep11 {
        /**
         * Sets existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep12 withExistingSession(Map<String, String> existingSession);
    }

    /**
     * Step 12 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep12 {
        /**
         * Sets the shared state.
         *
         * @param sharedState the shared state
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep13 withSharedState(Object sharedState);
    }

    /**
     * Step 13 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep13 {
        /**
         * Sets the transient state.
         *
         * @param transientState the transient state
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep14 withTransientState(Object transientState);
    }

    /**
     * Step 14 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep14 {
        /**
         * Sets the IdmIntegrationService.
         *
         * @param idmIntegrationServiceWrapper the idm integration service wrapper
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep15 withIdmIntegrationService(
                IdmIntegrationServiceScriptWrapper idmIntegrationServiceWrapper);
    }
    /**
     * Step 16 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep15 {
        /**
         * Sets the client.
         *
         * @param client the {@link Client}
         * @return the next step of the {@link Builder}
         */
        ScriptBindingsStep1 withClient(Client client);

    }

    /**
     * Builder object to construct a {@link ScriptedDecisionNodeBindings}.
     */
    private static final class Builder extends ScriptBindings.Builder<Builder>
            implements ScriptedDecisionNodeBindingsStep1, ScriptedDecisionNodeBindingsStep2,
            ScriptedDecisionNodeBindingsStep3, ScriptedDecisionNodeBindingsStep4, ScriptedDecisionNodeBindingsStep5,
            ScriptedDecisionNodeBindingsStep6, ScriptedDecisionNodeBindingsStep7, ScriptedDecisionNodeBindingsStep8,
            ScriptedDecisionNodeBindingsStep9, ScriptedDecisionNodeBindingsStep10, ScriptedDecisionNodeBindingsStep11,
            ScriptedDecisionNodeBindingsStep12, ScriptedDecisionNodeBindingsStep13, ScriptedDecisionNodeBindingsStep14,
            ScriptedDecisionNodeBindingsStep15 {

        private ScriptIdentityRepository scriptIdentityRepository;
        private ScriptedIdentityRepository scriptedIdentityRepository;
        private ScriptedSecrets secrets;
        private boolean resumedFromSuspend;
        private NodeState nodeState;
        private List<? extends Callback> callbacks;
        private String realm;
        private Map<String, List<String>> queryParameters;
        private Map<String, List<String>> headers;
        private ChfHttpClient httpClient;
        private Map<String, String> existingSession;
        private Object sharedState;
        private Object transientState;
        private Client client;
        private IdmIntegrationServiceScriptWrapper idmIntegrationServiceScriptWrapper;

        /**
         * Set the nodeState for the builder.
         *
         * @param nodeState The nodeState.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep2 withNodeState(NodeState nodeState) {
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
        public ScriptedDecisionNodeBindingsStep3 withCallbacks(List<? extends Callback> callbacks) {
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
        public ScriptedDecisionNodeBindingsStep4 withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Set the realm for the builder.
         *
         * @param realm The realm.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep5 withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Set the queryParameters for the builder.
         *
         * @param queryParameters The queryParameters of the request.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep6 withQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link ChfHttpClient}.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep7 withHttpClient(ChfHttpClient httpClient) {
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
        public ScriptedDecisionNodeBindingsStep8 withScriptIdentityRepository(
                ScriptIdentityRepository scriptIdentityRepository) {
            this.scriptIdentityRepository = scriptIdentityRepository;
            return this;
        }

        /**
         * Set the scriptedIdentityRepository for the builder.
         *
         * @param scriptedIdentityRepository The {@link ScriptedIdentityRepository}.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep9 withScriptedIdentityRepository(
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
        public ScriptedDecisionNodeBindingsStep10 withSecrets(ScriptedSecrets secrets) {
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
        public ScriptedDecisionNodeBindingsStep11 withResumedFromSuspend(boolean resumedFromSuspend) {
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
        public ScriptedDecisionNodeBindingsStep12 withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Set the sharedState.
         *
         * @param sharedState the sharedState.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep13 withSharedState(Object sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Set the transientState.
         *
         * @param transientState the sharedState.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep14 withTransientState(Object transientState) {
            this.transientState = transientState;
            return this;
        }


        /**
         * Sets the client.
         *
         * @param client the new {@link Client}
         * @return the next step of the Builder.
         */
        @Override
        public ScriptBindingsStep1 withClient(Client client) {
            this.client = client;
            return this;
        }

        /**
         * Sets the idmIntegrationServiceScriptWrapper.
         *
         * @param idmIntegrationServiceScriptWrapper the new {@link IdmIntegrationService}
         * @return the next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep15 withIdmIntegrationService(
                IdmIntegrationServiceScriptWrapper idmIntegrationServiceScriptWrapper) {
            this.idmIntegrationServiceScriptWrapper = idmIntegrationServiceScriptWrapper;
            return this;
        }

        /**
         * Builds the {@link ScriptedDecisionNodeBindings}.
         *
         * @return the {@link ScriptedDecisionNodeBindings}.
         */
        @Override
        public ScriptedDecisionNodeBindings build() {
            return new ScriptedDecisionNodeBindings(this);
        }


    }
}
