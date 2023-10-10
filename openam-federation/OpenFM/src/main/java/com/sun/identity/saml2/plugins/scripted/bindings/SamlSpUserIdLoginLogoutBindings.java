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
package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.BINDING;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.LOGOUT_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.LOGOUT_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.USER_ID;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;

/**
 * Script bindings for the SamlSpUserIdLoginLogout script.
 */
final class SamlSpUserIdLoginLogoutBindings extends BaseSamlSpBindings {

    private final String userId;
    private final LogoutRequest logoutRequest;
    private final LogoutResponse logoutResponse;
    private final String binding;

    /**
     * Constructor for SamlSpUserIdLoginLogoutBindings.
     *
     * @param builder The builder.
     */
    private SamlSpUserIdLoginLogoutBindings(Builder builder) {
        super(builder);
        this.userId = builder.userId;
        this.logoutRequest = builder.logoutRequest;
        this.logoutResponse = builder.logoutResponse;
        this.binding = builder.binding;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the builder.
     */
    static SamlSpUserIdLoginLogoutBindingsStep1 builder() {
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
        return "SAML SP User ID Login/Logout Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        List<Binding> v1Bindings = new ArrayList<>(v1CommonBindings());
        v1Bindings.addAll(List.of(
                Binding.of(USER_ID, userId, String.class),
                Binding.of(LOGOUT_REQUEST, logoutRequest, LogoutRequest.class),
                Binding.of(LOGOUT_RESPONSE, logoutResponse, LogoutResponse.class),
                Binding.of(BINDING, binding, String.class)
        ));
        return v1Bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlSpUserIdLoginLogoutBindings.
     */
    public interface SamlSpUserIdLoginLogoutBindingsStep1 {
        SamlSpUserIdLoginLogoutBindingsStep2 withUserId(String userId);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlSpUserIdLoginLogoutBindings.
     */
    public interface SamlSpUserIdLoginLogoutBindingsStep2 {
        SamlSpUserIdLoginLogoutBindingsStep3 withLogoutRequest(LogoutRequest logoutRequest);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlSpUserIdLoginLogoutBindings.
     */
    public interface SamlSpUserIdLoginLogoutBindingsStep3 {
        SamlSpUserIdLoginLogoutBindingsStep4 withLogoutResponse(LogoutResponse logoutResponse);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlSpUserIdLoginLogoutBindings.
     */
    public interface SamlSpUserIdLoginLogoutBindingsStep4 {
        SamlSpBindingsStep1 withBinding(String binding);
    }

    /**
     * Builder object to construct a {@link SamlSpUserIdLoginLogoutBindings}.
     */
    public static final class Builder extends BaseSamlSpBindings.Builder<Builder>
            implements SamlSpUserIdLoginLogoutBindingsStep1, SamlSpUserIdLoginLogoutBindingsStep2,
            SamlSpUserIdLoginLogoutBindingsStep3, SamlSpUserIdLoginLogoutBindingsStep4 {

        private String userId;
        private LogoutRequest logoutRequest;
        private LogoutResponse logoutResponse;
        private String binding;

        /**
         * Set the userId for the builder.
         *
         * @param userId The userId.
         * @return The first step of the builder.
         */
        public SamlSpUserIdLoginLogoutBindingsStep2 withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Set the logoutRequest for the builder.
         *
         * @param logoutRequest The {@link LogoutRequest}.
         * @return The next step of the builder.
         */
        public SamlSpUserIdLoginLogoutBindingsStep3 withLogoutRequest(LogoutRequest logoutRequest) {
            this.logoutRequest = logoutRequest;
            return this;
        }

        /**
         * Set the logoutResponse for the builder.
         *
         * @param logoutResponse The {@link LogoutResponse}.
         * @return The next step of the builder.
         */
        public SamlSpUserIdLoginLogoutBindingsStep4 withLogoutResponse(LogoutResponse logoutResponse) {
            this.logoutResponse = logoutResponse;
            return this;
        }

        /**
         * Set the binding for the builder.
         *
         * @param binding The binding.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep1 withBinding(String binding) {
            this.binding = binding;
            return this;
        }

        @Override
        public SamlSpUserIdLoginLogoutBindings build() {
            return new SamlSpUserIdLoginLogoutBindings(this);
        }
    }
}
