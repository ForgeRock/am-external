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
 * Copyright 2023-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.REALM_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SHARED_STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.TRANSIENT_STATE_IDENTIFIER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;

import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;
import org.forgerock.openam.scripting.domain.ScriptBindings;

/**
 * Script bindings for the SocialProviderHandlerNode script.
 */
public final class SocialProviderHandlerNodeBindings extends ScriptBindings {

    private static final String RAW_PROFILE_DATA = "rawProfile";
    private static final String NORMALIZED_PROFILE_DATA = "normalizedProfile";

    private final String selectedIDP;
    private final JsonValue rawProfile;
    private final JsonValue normalizedProfile;
    private final List<? extends Callback> callbacks;
    private final Map<String, List<String>> queryParameters;
    private final NodeState nodeState;
    private final Map<String, List<String>> headers;
    private final String realm;
    private final Object existingSession;
    private final Object sharedState;
    private final Object transientState;

    /**
     * Constructor for SocialProviderHandlerNodeBindings.
     *
     * @param builder The builder.
     */
    private SocialProviderHandlerNodeBindings(Builder builder) {
        super(builder);
        this.selectedIDP = builder.selectedIDP;
        this.rawProfile = builder.rawProfile;
        this.normalizedProfile = builder.normalizedProfile;
        this.callbacks = builder.callbacks;
        this.queryParameters = builder.queryParameters;
        this.nodeState = builder.nodeState;
        this.headers = builder.headers;
        this.realm = builder.realm;
        this.sharedState = builder.sharedState;
        this.transientState = builder.transientState;
        this.existingSession = builder.existingSession;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the Builder.
     */
    public static SocialProviderHandlerNodeBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public EvaluatorVersionBindings getEvaluatorVersionBindings() {
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
                Binding.of(SELECTED_IDP, selectedIDP, String.class),
                Binding.of(RAW_PROFILE_DATA, rawProfile, JsonValue.class),
                Binding.of(NORMALIZED_PROFILE_DATA, normalizedProfile, JsonValue.class),
                Binding.of(CALLBACKS_IDENTIFIER, callbacks, List.class),
                Binding.of(HEADERS_IDENTIFIER, headers, Map.class),
                Binding.of(REALM_IDENTIFIER, realm, String.class),
                Binding.of(QUERY_PARAMETER_IDENTIFIER, queryParameters, Map.class),
                Binding.of(STATE_IDENTIFIER, nodeState, NodeState.class),
                Binding.ofMayBeUndefined(EXISTING_SESSION, existingSession, Map.class)
        );
    }

    /**
     * Step 1 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep1 {
        /**
         * Sets selected IDP.
         *
         * @param selectedIDP the selected IDP.
         * @return the next step of the {@link Builder}.
         */
        SocialProviderHandlerNodeBindingsStep2 withSelectedIDP(String selectedIDP);
    }

    /**
     * Step 2 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep2 {
        /**
         * Sets the input data.
         *
         * @param inputData    the input data
         * @param isNormalized true if the input data is normalized
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep3 withInputData(JsonValue inputData, boolean isNormalized);
    }

    /**
     * Step 3 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep3 {
        /**
         * Sets the callbacks.
         *
         * @param callbacks the callbacks
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep4 withCallbacks(List<? extends Callback> callbacks);
    }

    /**
     * Step 4 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep4 {
        /**
         * Sets query parameters.
         *
         * @param queryParameters the query parameters
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep5 withQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     * Step 5 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep5 {
        /**
         * Sets node state.
         *
         * @param nodeState the node state
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep6 withNodeState(NodeState nodeState);
    }

    /**
     * Step 6 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep6 {
        /**
         * Sets headers.
         *
         * @param headers the headers
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep7 withHeaders(Map<String, List<String>> headers);
    }

    /**
     * Step 7 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep7 {
        /**
         * Sets realm.
         *
         * @param realm the realm
         * @return the next step of the {@link Builder}
         *
         */
        SocialProviderHandlerNodeBindingsStep8 withRealm(String realm);
    }

    /**
     * Step 8 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep8 {
        /**
         * Sets existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep9 withExistingSession(Map<String, String> existingSession);
    }

    /**
     * Step 9 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep9 {
        /**
         * Sets shared state.
         *
         * @param sharedState the shared state
         * @return the next step of the {@link Builder}
         */
        SocialProviderHandlerNodeBindingsStep10 withSharedState(Object sharedState);
    }

    /**
     * Step 10 of the builder.
     */
    public interface SocialProviderHandlerNodeBindingsStep10 {
        /**
         * Sets transient state.
         *
         * @param transientState the transient state
         * @return the next step of the {@link Builder}.
         */
        ScriptBindingsStep1 withTransientState(Object transientState);
    }

    /**
     * Builder object to construct a {@link SocialProviderHandlerNodeBindings}.
     */
    private static final class Builder extends ScriptBindings.Builder<Builder>
            implements SocialProviderHandlerNodeBindingsStep1, SocialProviderHandlerNodeBindingsStep2,
            SocialProviderHandlerNodeBindingsStep3, SocialProviderHandlerNodeBindingsStep4,
            SocialProviderHandlerNodeBindingsStep5, SocialProviderHandlerNodeBindingsStep6,
            SocialProviderHandlerNodeBindingsStep7, SocialProviderHandlerNodeBindingsStep8,
            SocialProviderHandlerNodeBindingsStep9, SocialProviderHandlerNodeBindingsStep10 {

        private String selectedIDP;
        private JsonValue rawProfile;
        private JsonValue normalizedProfile;
        private List<? extends Callback> callbacks;
        private Map<String, List<String>> queryParameters;
        private NodeState nodeState;
        private Map<String, List<String>> headers;
        private String realm;
        private Map<String, String> existingSession;
        private Object sharedState;
        private Object transientState;

        @Override
        public SocialProviderHandlerNodeBindings build() {
            return new SocialProviderHandlerNodeBindings(this);
        }

        /**
         * Set the selectedIDP for the builder.
         *
         * @param selectedIDP The selectedIDP.
         * @return The next step of the Builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep2 withSelectedIDP(String selectedIDP) {
            this.selectedIDP = selectedIDP;
            return this;
        }

        /**
         * Sets the profile data.
         *
         * @param inputData  the normalized profile {@link JsonValue}.
         * @param normalized true if the profile is normalised.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep3 withInputData(JsonValue inputData, boolean normalized) {
            if (normalized) {
                this.normalizedProfile = inputData;
            } else {
                this.rawProfile = inputData;
            }
            return this;
        }

        /**
         * Sets the list of {@link Callback}s.
         *
         * @param callbacks The {@link Callback}s.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep4 withCallbacks(List<? extends Callback> callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        /**
         * Sets the lquery parameters.
         *
         * @param queryParameters The query parameters.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep5 withQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        /**
         * Sets the {@link NodeState}s.
         *
         * @param nodeState The {@link NodeState}.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep6 withNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * Sets the headers.
         *
         * @param headers The headers.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep7 withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the realm.
         *
         * @param realm The realm.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep8 withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Sets the existing session.
         *
         * @param existingSession The existing session.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep9 withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Sets the shared state.
         *
         * @param sharedState The shared state.
         * @return The next step of the builder.
         */
        @Override
        public SocialProviderHandlerNodeBindingsStep10 withSharedState(Object sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Sets the transient state.
         *
         * @param transientState The transient state.
         * @return The next step of the builder.
         */
        @Override
        public ScriptBindingsStep1 withTransientState(Object transientState) {
            this.transientState = transientState;
            return this;
        }
    }
}
