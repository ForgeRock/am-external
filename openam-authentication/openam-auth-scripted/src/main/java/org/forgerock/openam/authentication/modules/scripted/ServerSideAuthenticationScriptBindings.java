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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.scripted;

import static org.forgerock.openam.authentication.modules.scripted.Scripted.CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.HTTP_CLIENT_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.IDENTITY_REPOSITORY;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.REQUEST_DATA_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.SHARED_STATE;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.STATE_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.USERNAME_VARIABLE_NAME;

import java.util.Map;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the Scripted Module script.
 */
public final class ServerSideAuthenticationScriptBindings implements LegacyScriptBindings {

    private static final String SUCCESS_ATTR_NAME = "SUCCESS";
    private static final String FAILED_ATTR_NAME = "FAILED";
    private final ScriptHttpRequestWrapper requestData;
    private final String clientScriptOutputData;
    private final Integer state;
    private final Map<String, Object> sharedState;
    private final String username;
    private final Integer successValue;
    private final Integer failureValue;
    private final ChfHttpClient httpClient;
    private final ScriptIdentityRepository identityRepository;

    /**
     * Constructor for ScriptedBindings.
     *
     * @param builder The builder.
     */
    private ServerSideAuthenticationScriptBindings(Builder builder) {
        requestData = builder.requestData;
        clientScriptOutputData = builder.clientScriptOutputData;
        state = builder.state;
        sharedState = builder.sharedState;
        username = builder.username;
        successValue = builder.successValue;
        failureValue = builder.failureValue;
        httpClient = builder.httpClient;
        identityRepository = builder.identityRepository;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The builder.
     */
    public static ScriptedBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(REQUEST_DATA_VARIABLE_NAME, requestData);
        bindings.put(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputData);
        bindings.put(STATE_VARIABLE_NAME, state);
        bindings.put(SHARED_STATE, sharedState);
        bindings.put(USERNAME_VARIABLE_NAME, username);
        bindings.put(SUCCESS_ATTR_NAME, successValue);
        bindings.put(FAILED_ATTR_NAME, failureValue);
        bindings.put(HTTP_CLIENT_VARIABLE_NAME, httpClient);
        bindings.put(IDENTITY_REPOSITORY, identityRepository);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep1 {
        ScriptedBindingsStep2 withRequestData(ScriptHttpRequestWrapper requestData);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep2 {
        ScriptedBindingsStep3 withClientScriptOutputData(String clientScriptOutputData);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep3 {
        ScriptedBindingsStep4 withState(Integer state);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep4 {
        ScriptedBindingsStep5 withSharedState(Map<String, Object> sharedState);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep5 {
        ScriptedBindingsStep6 withUsername(String userName);
    }

    /**
     * Interface utilised by the fluent builder to define step 6 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep6 {
        ScriptedBindingsStep7 withSuccessValue(Integer successValue);
    }

    /**
     * Interface utilised by the fluent builder to define step 7 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep7 {
        ScriptedBindingsStep8 withFailureValue(Integer failureValue);
    }

    /**
     * Interface utilised by the fluent builder to define step 8 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep8 {
        ScriptedBindingsStep9 withHttpClient(ChfHttpClient httpClient);
    }

    /**
     * Interface utilised by the fluent builder to define step 9 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep9 {
        ScriptedBindingsFinalStep withIdentityRepository(ScriptIdentityRepository identityRepository);
    }

    /**
     * Final step of the builder.
     */
    public interface ScriptedBindingsFinalStep {
        /**
         * Build the {@link ServerSideAuthenticationScriptBindings}.
         *
         * @return The {@link ServerSideAuthenticationScriptBindings}.
         */
        ServerSideAuthenticationScriptBindings build();
    }

    /**
     * Builder object to construct a {@link ServerSideAuthenticationScriptBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder implements ScriptedBindingsStep1,
            ScriptedBindingsStep2, ScriptedBindingsStep3, ScriptedBindingsStep4, ScriptedBindingsStep5,
            ScriptedBindingsStep6, ScriptedBindingsStep7, ScriptedBindingsStep8, ScriptedBindingsStep9,
            ScriptedBindingsFinalStep {

        private ScriptHttpRequestWrapper requestData;
        private String clientScriptOutputData;
        private int state;
        private Map<String, Object> sharedState;
        private String username;
        private Integer successValue;
        private Integer failureValue;
        private ChfHttpClient httpClient;
        private ScriptIdentityRepository identityRepository;

        public ServerSideAuthenticationScriptBindings build() {
            return new ServerSideAuthenticationScriptBindings(this);
        }

        /**
         * Set the requestData for the builder.
         *
         * @param requestData The {@link ScriptHttpRequestWrapper}.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep2 withRequestData(ScriptHttpRequestWrapper requestData) {
            this.requestData = requestData;
            return this;
        }

        /**
         * Set the clientScriptOutputData for the builder.
         *
         * @param clientScriptOutputData The clientScriptOutputData.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep3 withClientScriptOutputData(String clientScriptOutputData) {
            this.clientScriptOutputData = clientScriptOutputData;
            return this;
        }

        /**
         * Set the state for the builder.
         *
         * @param state The state.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep4 withState(Integer state) {
            this.state = state;
            return this;
        }

        /**
         * Set the sharedState for the builder.
         *
         * @param sharedState The sharedState.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep5 withSharedState(Map<String, Object> sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Set the username for the builder.
         *
         * @param username The username.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep6 withUsername(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the successValue for the builder.
         *
         * @param successValue The successValue.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep7 withSuccessValue(Integer successValue) {
            this.successValue = successValue;
            return this;
        }

        /**
         * Set the failureValue for the builder.
         *
         * @param failureValue The failureValue.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep8 withFailureValue(Integer failureValue) {
            this.failureValue = failureValue;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link ChfHttpClient}.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep9 withHttpClient(ChfHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set the identityRepository for the builder.
         *
         * @param identityRepository The {@link ScriptIdentityRepository}.
         * @return The next step of the builder.
         */
        public Builder withIdentityRepository(ScriptIdentityRepository identityRepository) {
            this.identityRepository = identityRepository;
            return this;
        }
    }
}
