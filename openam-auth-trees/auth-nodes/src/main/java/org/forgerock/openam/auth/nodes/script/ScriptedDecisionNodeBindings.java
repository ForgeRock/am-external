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

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.AUDIT_ENTRY_DETAIL;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HTTP_CLIENT_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.REALM_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SECRETS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SHARED_STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.TRANSIENT_STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.Callback;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.scripting.api.secrets.ScriptedSecrets;
import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.mozilla.javascript.Undefined;

/**
 * Script bindings for the ScriptedDecisionNode script.
 */
public final class ScriptedDecisionNodeBindings extends ScriptBindings {

    private static final String RESUMED_FROM_SUSPEND = "resumedFromSuspend";

    private final NodeState nodeState;
    private final List<? extends Callback> callbacks;
    private final Map<String, List<String>> headers;
    private final String realm;
    private final Map<String, List<String>> queryParameters;
    private final ChfHttpClient httpClient;
    private final ScriptIdentityRepository identityRepository;
    private final ScriptedSecrets secrets;
    private final JsonValue auditEntryDetail;
    private final boolean resumedFromSuspend;
    private final Object existingSession;
    private final Object sharedState;
    private final Object transientState;


    private ScriptedDecisionNodeBindings(Builder builder) {
        super(builder);
        this.nodeState = builder.nodeState;
        this.callbacks = builder.callbacks;
        this.headers = builder.headers;
        this.realm = builder.realm;
        this.queryParameters = builder.queryParameters;
        this.httpClient = builder.httpClient;
        this.identityRepository = builder.identityRepository;
        this.secrets = builder.secrets;
        this.auditEntryDetail = builder.auditEntryDetail;
        this.resumedFromSuspend = builder.resumedFromSuspend;
        this.sharedState = builder.sharedState;
        this.transientState = builder.transientState;
        this.existingSession = Objects.requireNonNullElse(builder.existingSession, Undefined.instance);
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
    protected EvaluatorVersionBindings getEvaluatorVersionBindings() {
        List<Binding> v1Bindings = new java.util.ArrayList<>(getCommonBindings());
        v1Bindings.add(Binding.of(SHARED_STATE_IDENTIFIER, sharedState, Map.class));
        v1Bindings.add(Binding.of(TRANSIENT_STATE_IDENTIFIER, transientState, Map.class));

        return EvaluatorVersionBindings.builder()
                .v1Bindings(v1Bindings)
                .v2Bindings(getCommonBindings())
                .parentBindings(super.getEvaluatorVersionBindings())
                .build();
    }

    private List<Binding> getCommonBindings() {
        return List.of(
                Binding.of(STATE_IDENTIFIER, nodeState, NodeState.class),
                Binding.of(CALLBACKS_IDENTIFIER, callbacks, List.class),
                Binding.of(HEADERS_IDENTIFIER, headers, Map.class),
                Binding.of(REALM_IDENTIFIER, realm, String.class),
                Binding.of(QUERY_PARAMETER_IDENTIFIER, queryParameters, Map.class),
                Binding.of(HTTP_CLIENT_IDENTIFIER, httpClient, ChfHttpClient.class),
                Binding.of(ID_REPO_IDENTIFIER, identityRepository, ScriptIdentityRepository.class),
                Binding.of(SECRETS_IDENTIFIER, secrets, ScriptedSecrets.class),
                Binding.of(AUDIT_ENTRY_DETAIL, auditEntryDetail, JsonValue.class),
                Binding.of(RESUMED_FROM_SUSPEND, resumedFromSuspend, Boolean.class),
                Binding.of(EXISTING_SESSION, existingSession, Map.class)
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
         * Sets the identity repository.
         *
         * @param identityRepository the identity repository
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep8 withIdentityRepository(ScriptIdentityRepository identityRepository);
    }

    /**
     * Step 8 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep8 {
        /**
         * Sets the {@link ScriptedSecrets}.
         *
         * @param secrets the {@link ScriptedSecrets}
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep9 withSecrets(ScriptedSecrets secrets);
    }

    /**
     * Step 9 of the builder.
     */
    public interface ScriptedDecisionNodeBindingsStep9 {
        /**
         * Sets the audit entry detail.
         *
         * @param auditEntryDetail the audit entry detail
         * @return the next step of the {@link Builder}
         */
        ScriptedDecisionNodeBindingsStep10 withAuditEntryDetail(JsonValue auditEntryDetail);
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
        ScriptBindingsStep1 withTransientState(Object transientState);
    }

    /**
     * Builder object to construct a {@link ScriptedDecisionNodeBindings}.
     */
    private static final class Builder extends ScriptBindings.Builder<Builder>
            implements ScriptedDecisionNodeBindingsStep1, ScriptedDecisionNodeBindingsStep2,
            ScriptedDecisionNodeBindingsStep3, ScriptedDecisionNodeBindingsStep4, ScriptedDecisionNodeBindingsStep5,
            ScriptedDecisionNodeBindingsStep6, ScriptedDecisionNodeBindingsStep7, ScriptedDecisionNodeBindingsStep8,
            ScriptedDecisionNodeBindingsStep9, ScriptedDecisionNodeBindingsStep10, ScriptedDecisionNodeBindingsStep11,
            ScriptedDecisionNodeBindingsStep12, ScriptedDecisionNodeBindingsStep13 {

        private ScriptIdentityRepository identityRepository;
        private ScriptedSecrets secrets;
        private JsonValue auditEntryDetail;
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
         * Set the identityRepository for the builder.
         *
         * @param identityRepository The {@link ScriptIdentityRepository}.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep8 withIdentityRepository(ScriptIdentityRepository identityRepository) {
            this.identityRepository = identityRepository;
            return this;
        }

        /**
         * Set the secrets for the builder.
         *
         * @param secrets The {@link ScriptedSecrets}.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep9 withSecrets(ScriptedSecrets secrets) {
            this.secrets = secrets;
            return this;
        }

        /**
         * Set the auditEntryDetail for the builder.
         *
         * @param auditEntryDetail The auditEntryDetail as {@link JsonValue}.
         * @return The next step of the Builder.
         */
        @Override
        public ScriptedDecisionNodeBindingsStep10 withAuditEntryDetail(JsonValue auditEntryDetail) {
            this.auditEntryDetail = auditEntryDetail;
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
        public ScriptBindingsStep1 withTransientState(Object transientState) {
            this.transientState = transientState;
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
