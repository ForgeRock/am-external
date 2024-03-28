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
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SECRETS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.scripting.api.secrets.ScriptedSecrets;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the ConfigProviderNode script.
 */
public final class ConfigProviderNodeBindings implements ScriptBindings {

    private final NodeState nodeState;
    private final ScriptIdentityRepository identityRepository;
    private final ScriptedSecrets secrets;
    private final Map<String, List<String>> headers;
    private final ChfHttpClient httpClient;
    private final Object existingSession;
    private final Map<String, List<String>> queryParameters;

    /**
     * Constructor for ConfigProviderNodeBindings.
     *
     * @param builder The builder.
     */
    private ConfigProviderNodeBindings(Builder builder) {
        nodeState = builder.nodeState;
        identityRepository = builder.identityRepository;
        secrets = builder.secrets;
        headers = builder.headers;
        httpClient = builder.httpClient;
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

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(STATE_IDENTIFIER, nodeState);
        bindings.put(ID_REPO_IDENTIFIER, identityRepository);
        bindings.put(SECRETS_IDENTIFIER, secrets);
        bindings.put(HEADERS_IDENTIFIER, headers);
        bindings.put(HTTP_CLIENT_IDENTIFIER, httpClient);
        bindings.putIfDefined(EXISTING_SESSION, existingSession);
        bindings.put(QUERY_PARAMETER_IDENTIFIER, queryParameters);
        return bindings;
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
         * Sets query parameters.
         *
         * @param queryParameters the query parameters
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep7 withQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     * Step 7 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep7 {
        /**
         * Sets the existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        Builder withExistingSession(Map<String, String> existingSession);
    }

    /**
     * Builder object to construct a {@link ConfigProviderNodeBindings}.
     */
    public static final class Builder implements
            ConfigProviderNodeBindingsStep1, ConfigProviderNodeBindingsStep2, ConfigProviderNodeBindingsStep3,
            ConfigProviderNodeBindingsStep4, ConfigProviderNodeBindingsStep5, ConfigProviderNodeBindingsStep6,
            ConfigProviderNodeBindingsStep7 {
        private NodeState nodeState;
        private ScriptIdentityRepository identityRepository;
        private ScriptedSecrets secrets;
        private Map<String, List<String>> headers;
        private ChfHttpClient httpClient;
        private Map<String, String> existingSession;
        private Map<String, List<String>> queryParameters;

        /**
         * Creates the {@link ConfigProviderNodeBindings} from the configured attributes.
         *
         * @return an instance of {@link ConfigProviderNodeBindings}.
         */
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
         * Set the existingSession for the builder.
         *
         * @param existingSession The existingSession.
         * @return The next step of the builder.
         */
        public Builder withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Set the queryParameters for the builder.
         *
         * @param queryParameters The queryParameters.
         * @return The next step of the builder.
         */
        public Builder withQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }
    }
}
