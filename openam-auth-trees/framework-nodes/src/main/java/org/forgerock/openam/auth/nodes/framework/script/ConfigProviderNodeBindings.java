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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HTTP_CLIENT_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.oauth2.OAuth2Constants.ScriptParams.SECRETS_IDENTIFIER;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.auth.nodes.script.BaseScriptedDecisionNodeBindings;
import org.forgerock.openam.scripting.api.secrets.IScriptedSecrets;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the ConfigProviderNode script.
 */
public final class ConfigProviderNodeBindings extends BaseScriptedDecisionNodeBindings
        implements LegacyScriptBindings {

    private final ScriptIdentityRepository identityRepository;
    private final IScriptedSecrets secrets;
    private final ChfHttpClient httpClient;

    /**
     * Constructor for ConfigProviderNodeBindings.
     *
     * @param builder The builder.
     */
    private ConfigProviderNodeBindings(Builder builder) {
        super(builder);
        identityRepository = builder.identityRepository;
        secrets = builder.secrets;
        httpClient = builder.httpClient;
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

    @Override
    public BindingsMap nextGenBindings() {
        return commonNextGenBindings();
    }

    /**
     * Step 1 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep1 {
        /**
         * Sets the identity repository.
         *
         * @param identityRepository the identity repository
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep2 withIdRepo(ScriptIdentityRepository identityRepository);
    }

    /**
     * Step 2 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep2 {
        /**
         * Sets the {@link IScriptedSecrets}.
         *
         * @param secrets the {@link IScriptedSecrets}
         * @return the next step of the {@link Builder}
         */
        ConfigProviderNodeBindingsStep3 withSecrets(IScriptedSecrets secrets);
    }

    /**
     * Step 5 of the builder.
     */
    public interface ConfigProviderNodeBindingsStep3 {
        /**
         * Sets the {@link ChfHttpClient}.
         *
         * @param httpClient the {@link ChfHttpClient}
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep1<ConfigProviderNodeBindings> withHttpClient(ChfHttpClient httpClient);
    }

    /**
     * Builder object to construct a {@link ConfigProviderNodeBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseScriptedDecisionNodeBindings.Builder<ConfigProviderNodeBindings>
            implements ConfigProviderNodeBindingsStep1, ConfigProviderNodeBindingsStep2,
            ConfigProviderNodeBindingsStep3 {
        private ScriptIdentityRepository identityRepository;
        private IScriptedSecrets secrets;
        private ChfHttpClient httpClient;

        /**
         * Creates the {@link ConfigProviderNodeBindings} from the configured attributes.
         *
         * @return an instance of {@link ConfigProviderNodeBindings}.
         */
        @Override
        public ConfigProviderNodeBindings build() {
            return new ConfigProviderNodeBindings(this);
        }

        /**
         * Set the identityRepository for the builder.
         *
         * @param identityRepository The identityRepository {@link ScriptIdentityRepository}.
         * @return The next step of the builder.
         */
        @Override
        public ConfigProviderNodeBindingsStep2 withIdRepo(ScriptIdentityRepository identityRepository) {
            this.identityRepository = identityRepository;
            return this;
        }

        /**
         * Set the secrets for the builder.
         *
         * @param secrets The secrets {@link IScriptedSecrets}.
         * @return The next step of the builder.
         */
        @Override
        public ConfigProviderNodeBindingsStep3 withSecrets(IScriptedSecrets secrets) {
            this.secrets = secrets;
            return this;
        }


        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The httpClient {@link ChfHttpClient}
         * @return The next step of the builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep1<ConfigProviderNodeBindings> withHttpClient(
                ChfHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }
    }
}
