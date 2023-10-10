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

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HTTP_CLIENT_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.REALM_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SECRETS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.scripting.api.secrets.ScriptedSecrets;
import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the ConfigProviderNode script.
 */
public final class ConfigProviderNodeBindings extends ScriptBindings {

    private final NodeState nodeState;
    private final ScriptIdentityRepository identityRepository;
    private final ScriptedSecrets secrets;
    private final Map<String, List<String>> headers;
    private final ChfHttpClient httpClient;
    private final String realm;
    private final Object existingSession;
    private final Map<String, List<String>> queryParameters;


    /**
     * Constructor for ConfigProviderNodeBindings.
     *
     * @param builder The builder.
     */
    private ConfigProviderNodeBindings(Builder builder) {
        super(builder);
        nodeState = builder.nodeState;
        identityRepository = builder.identityRepository;
        secrets = builder.secrets;
        headers = builder.headers;
        httpClient = builder.httpClient;
        realm = builder.realm;
        queryParameters = builder.queryParameters;
        existingSession = builder.existingSession;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The builder.
     */
    public static ConfigProviderNodeBindingsStep1 builder() {
        return new ConfigProviderNodeBindings.Builder();
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
        return "Config Provider Node Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        return List.of(
                Binding.of(STATE_IDENTIFIER, nodeState, NodeState.class),
                Binding.of(ID_REPO_IDENTIFIER, identityRepository, ScriptIdentityRepository.class),
                Binding.of(SECRETS_IDENTIFIER, secrets, ScriptedSecrets.class),
                Binding.of(HEADERS_IDENTIFIER, headers, Map.class),
                Binding.of(HTTP_CLIENT_IDENTIFIER, httpClient, ChfHttpClient.class),
                Binding.of(REALM_IDENTIFIER, realm, String.class),
                Binding.ofMayBeUndefined(EXISTING_SESSION, existingSession, Map.class),
                Binding.of(QUERY_PARAMETER_IDENTIFIER, queryParameters, Map.class)
        );
    }

    /**
     * Step 1 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep1 {
        /**
         * Sets the {@link NodeState}.
         *
         * @param nodeState the {@link NodeState
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep2 withNodeState(NodeState nodeState);
    }

    /**
     * Step 2 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep2 {
        /**
         * Sets the identity repository.
         *
         * @param identityRepository the identity repository
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep3 withIdRepo(ScriptIdentityRepository identityRepository);
    }

    /**
     * Step 3 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep3 {
        /**
         * Sets the {@link ScriptedSecrets}.
         *
         * @param secrets the {@link ScriptedSecrets}
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep4 withSecrets(ScriptedSecrets secrets);
    }

    /**
     * Step 4 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep4 {
        /**
         * Sets the headers.
         *
         * @param headers the headers
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep5 withHeaders(Map<String, List<String>> headers);
    }

    /**
     * Step 5 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep5 {
        /**
         * Sets the {@link ChfHttpClient}.
         *
         * @param httpClient the {@link ChfHttpClient}
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep6 withHttpClient(ChfHttpClient httpClient);
    }

    /**
     * Step 6 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep6 {
        /**
         * Sets the realm.
         *
         * @param realm the realm
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep7 withRealm(String realm);
    }

    /**
     * Step 7 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep7 {
        /**
         * Sets query parameters.
         *
         * @param queryParameters the query parameters
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep8 withQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     * Step 8 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep8 extends ScriptBindingsStep1 {
        /**
         * Sets the existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep8 withExistingSession(Map<String, String> existingSession);
    }

    /**
     * Builder object to construct a {@link ConfigProviderNodeBindings}.
     */
    private static final class Builder extends ScriptBindings.Builder<Builder> implements
            ConfigProviderNodeBindingsStep1, ConfigProviderNodeBindingsStep2, ConfigProviderNodeBindingsStep3,
            ConfigProviderNodeBindingsStep4, ConfigProviderNodeBindingsStep5, ConfigProviderNodeBindingsStep6,
            ConfigProviderNodeBindingsStep7, ConfigProviderNodeBindingsStep8 {
        private NodeState nodeState;
        private ScriptIdentityRepository identityRepository;
        private ScriptedSecrets secrets;
        private Map<String, List<String>> headers;
        private ChfHttpClient httpClient;
        private String realm;
        private Map<String, String> existingSession;
        private Map<String, List<String>> queryParameters;

        @Override
        public ConfigProviderNodeBindings build() {
            return new ConfigProviderNodeBindings(this);
        }

        /**
         * Set the node state.
         *
         * @param nodeState The node state {@link NodeState}
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep2 withNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * Set the identityRepository for the builder.
         *
         * @param identityRepository The identityRepository {@link ScriptIdentityRepository}.
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep3 withIdRepo(ScriptIdentityRepository identityRepository) {
            this.identityRepository = identityRepository;
            return this;
        }

        /**
         * Set the secrets for the builder.
         *
         * @param secrets The secrets {@link ScriptedSecrets}.
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep4 withSecrets(ScriptedSecrets secrets) {
            this.secrets = secrets;
            return this;
        }

        /**
         * Set the headers for the builder.
         *
         * @param headers The headers.
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep5 withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The httpClient {@link ChfHttpClient}
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep6 withHttpClient(ChfHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set the realm for the builder.
         *
         * @param realm The realm path.
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep7 withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Set the existingSession for the builder.
         *
         * @param existingSession The existingSession.
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep8 withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Set the queryParameters for the builder.
         *
         * @param queryParameters The queryParameters.
         * @return The next step of the builder.
         */
        public ConfigProviderNodeBindingsStep8 withQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }
    }
}
