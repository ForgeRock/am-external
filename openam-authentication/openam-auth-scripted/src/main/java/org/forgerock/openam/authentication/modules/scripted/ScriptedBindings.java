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
package org.forgerock.openam.authentication.modules.scripted;

import static org.forgerock.openam.authentication.modules.scripted.Scripted.CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.HTTP_CLIENT_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.IDENTITY_REPOSITORY;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.REALM_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.REQUEST_DATA_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.SHARED_STATE;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.STATE_VARIABLE_NAME;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.USERNAME_VARIABLE_NAME;

import java.util.List;
import java.util.Map;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;

/**
 * Script bindings for the Scripted Module script.
 */
public final class ScriptedBindings extends ScriptBindings {

    private static final String SUCCESS_ATTR_NAME = "SUCCESS";
    private static final String FAILED_ATTR_NAME = "FAILED";
    private final ScriptHttpRequestWrapper requestData;
    private final String clientScriptOutputData;
    private final Integer state;
    private final Map<String, Object> sharedState;
    private final String username;
    private final String realm;
    private final Integer successValue;
    private final Integer failureValue;
    private final ChfHttpClient httpClient;
    private final ScriptIdentityRepository identityRepository;

    /**
     * Constructor for ScriptedBindings.
     *
     * @param builder The builder.
     */
    private ScriptedBindings(Builder builder) {
        super(builder);
        requestData = builder.requestData;
        clientScriptOutputData = builder.clientScriptOutputData;
        state = builder.state;
        sharedState = builder.sharedState;
        username = builder.username;
        realm = builder.realm;
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
    protected EvaluatorVersionBindings getEvaluatorVersionBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(REQUEST_DATA_VARIABLE_NAME, requestData, ScriptHttpRequestWrapper.class),
                        Binding.of(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputData, String.class),
                        Binding.of(STATE_VARIABLE_NAME, state, Integer.class),
                        Binding.of(SHARED_STATE, sharedState, Map.class),
                        Binding.of(USERNAME_VARIABLE_NAME, username, String.class),
                        Binding.of(REALM_VARIABLE_NAME, realm, String.class),
                        Binding.of(SUCCESS_ATTR_NAME, successValue, Integer.class),
                        Binding.of(FAILED_ATTR_NAME, failureValue, Integer.class),
                        Binding.of(HTTP_CLIENT_VARIABLE_NAME, httpClient, ChfHttpClient.class),
                        Binding.of(IDENTITY_REPOSITORY, identityRepository, ScriptIdentityRepository.class)
                ))
                .parentBindings(super.getEvaluatorVersionBindings())
                .build();
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
        ScriptedBindingsStep7 withRealm(String realm);
    }

    /**
     * Interface utilised by the fluent builder to define step 7 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep7 {
        ScriptedBindingsStep8 withSuccessValue(Integer successValue);
    }

    /**
     * Interface utilised by the fluent builder to define step 8 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep8 {
        ScriptedBindingsStep9 withFailureValue(Integer failureValue);
    }

    /**
     * Interface utilised by the fluent builder to define step 9 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep9 {
        ScriptedBindingsStep10 withHttpClient(ChfHttpClient httpClient);
    }

    /**
     * Interface utilised by the fluent builder to define step 10 in generating the ScriptedBindings.
     */
    public interface ScriptedBindingsStep10 {
        ScriptBindingsStep1 withIdentityRepository(ScriptIdentityRepository identityRepository);
    }

    /**
     * Builder object to construct a {@link ScriptedBindings}.
     */
    private static final class Builder extends ScriptBindings.Builder<Builder> implements ScriptedBindingsStep1,
            ScriptedBindingsStep2, ScriptedBindingsStep3, ScriptedBindingsStep4, ScriptedBindingsStep5,
            ScriptedBindingsStep6, ScriptedBindingsStep7, ScriptedBindingsStep8, ScriptedBindingsStep9,
            ScriptedBindingsStep10 {

        private ScriptHttpRequestWrapper requestData;
        private String clientScriptOutputData;
        private int state;
        private Map<String, Object> sharedState;
        private String username;
        private String realm;
        private Integer successValue;
        private Integer failureValue;
        private ChfHttpClient httpClient;
        private ScriptIdentityRepository identityRepository;

        @Override
        public ScriptedBindings build() {
            return new ScriptedBindings(this);
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
         * Set the realm for the builder.
         *
         * @param realm The realm as String.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep7 withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Set the successValue for the builder.
         *
         * @param successValue The successValue.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep8 withSuccessValue(Integer successValue) {
            this.successValue = successValue;
            return this;
        }

        /**
         * Set the failureValue for the builder.
         *
         * @param failureValue The failureValue.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep9 withFailureValue(Integer failureValue) {
            this.failureValue = failureValue;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link ChfHttpClient}.
         * @return The next step of the builder.
         */
        public ScriptedBindingsStep10 withHttpClient(ChfHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set the identityRepository for the builder.
         *
         * @param identityRepository The {@link ScriptIdentityRepository}.
         * @return The next step of the builder.
         */
        public ScriptBindingsStep1 withIdentityRepository(ScriptIdentityRepository identityRepository) {
            this.identityRepository = identityRepository;
            return this;
        }
    }
}
