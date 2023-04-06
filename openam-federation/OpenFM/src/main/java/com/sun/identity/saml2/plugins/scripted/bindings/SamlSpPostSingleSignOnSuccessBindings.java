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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IS_FEDERATION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.OUT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.PROFILE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import java.io.PrintWriter;
import java.util.List;

import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;

import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Script bindings for the SamlSpPostSingleSignOnSuccess script.
 */
final class SamlSpPostSingleSignOnSuccessBindings extends BaseSamlSpBindings {

    private final AuthnRequest authnRequest;
    private final String profile;
    private final PrintWriter out;
    private final Response ssoResponse;
    private final Object session;
    private final boolean federation;


    /**
     * Constructor for SamlSpPostSingleSignOnSuccessBindings.
     *
     * @param builder The builder.
     */
    private SamlSpPostSingleSignOnSuccessBindings(Builder builder) {
        super(builder);
        authnRequest = builder.authnRequest;
        profile = builder.profile;
        out = builder.out;
        ssoResponse = builder.ssoResponse;
        session = builder.session;
        federation = builder.federation;
    }

    /**
     * Static method to get the builder.
     *
     * @return The first step of the builder.
     */
    static SamlSpPostSingleSignOnSuccessBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    protected EvaluatorVersionBindings getEvaluatorVersionBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(AUTHN_REQUEST, authnRequest, AuthnRequest.class),
                        Binding.of(PROFILE, profile, String.class),
                        Binding.of(OUT, out, PrintWriter.class),
                        Binding.of(SAML2_RESPONSE, ssoResponse, Response.class),
                        Binding.of(SESSION, session, Object.class),
                        Binding.of(IS_FEDERATION, federation, Boolean.class)
                ))
                .parentBindings(super.getEvaluatorVersionBindings())
                .build();
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the
     * SamlSpPostSingleSignOnSuccessBindings.
     */
    public interface SamlSpPostSingleSignOnSuccessBindingsStep1 {
        SamlSpPostSingleSignOnSuccessBindingsStep2 withAuthnRequest(AuthnRequest authnRequest);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the
     * SamlSpPostSingleSignOnSuccessBindings.
     */
    public interface SamlSpPostSingleSignOnSuccessBindingsStep2 {
        SamlSpPostSingleSignOnSuccessBindingsStep3 withProfile(String profile);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the
     * SamlSpPostSingleSignOnSuccessBindings.
     */
    public interface SamlSpPostSingleSignOnSuccessBindingsStep3 {
        SamlSpPostSingleSignOnSuccessBindingsStep4 withOut(PrintWriter out);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the
     * SamlSpPostSingleSignOnSuccessBindings.
     */
    public interface SamlSpPostSingleSignOnSuccessBindingsStep4 {
        SamlSpPostSingleSignOnSuccessBindingsStep5 withSsoResponse(Response ssoResponse);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the
     * SamlSpPostSingleSignOnSuccessBindings.
     */
    public interface SamlSpPostSingleSignOnSuccessBindingsStep5 {
        SamlSpPostSingleSignOnSuccessBindingsStep6 withSession(Object session);
    }

    /**
     * Interface utilised by the fluent builder to define step 6 in generating the
     * SamlSpPostSingleSignOnSuccessBindings.
     */
    public interface SamlSpPostSingleSignOnSuccessBindingsStep6 {
        SamlSpBindingsStep1 withFederation(boolean federation);
    }

    /**
     * Builder object to construct a {@link SamlSpPostSingleSignOnSuccessBindings}.
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<Builder>
            implements SamlSpPostSingleSignOnSuccessBindingsStep1, SamlSpPostSingleSignOnSuccessBindingsStep2,
            SamlSpPostSingleSignOnSuccessBindingsStep3, SamlSpPostSingleSignOnSuccessBindingsStep4,
            SamlSpPostSingleSignOnSuccessBindingsStep5, SamlSpPostSingleSignOnSuccessBindingsStep6 {

        private AuthnRequest authnRequest;
        private String profile;
        private PrintWriter out;
        private Response ssoResponse;
        private Object session;
        private boolean federation;

        /**
         * Set the authnRequest for the builder.
         *
         * @param authnRequest The {@link AuthnRequest}.
         * @return The next step of the builder.
         */
        public SamlSpPostSingleSignOnSuccessBindingsStep2 withAuthnRequest(AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }

        /**
         * Set the profile for the builder.
         *
         * @param profile The profile.
         * @return The next step of the builder.
         */
        public SamlSpPostSingleSignOnSuccessBindingsStep3 withProfile(String profile) {
            this.profile = profile;
            return this;
        }

        /**
         * Set the out writer for the builder.
         *
         * @param out The {@link PrintWriter}.
         * @return The next step of the builder.
         */
        public SamlSpPostSingleSignOnSuccessBindingsStep4 withOut(PrintWriter out) {
            this.out = out;
            return this;
        }

        /**
         * Set the ssoResponse for the builder.
         *
         * @param ssoResponse The {@link Response}.
         * @return The next step of the builder.
         */
        public SamlSpPostSingleSignOnSuccessBindingsStep5 withSsoResponse(Response ssoResponse) {
            this.ssoResponse = ssoResponse;
            return this;
        }

        /**
         * Set the session for the builder.
         *
         * @param session The session {@link Object}.
         * @return The next step of the builder.
         */
        public SamlSpPostSingleSignOnSuccessBindingsStep6 withSession(Object session) {
            this.session = session;
            return this;
        }

        /**
         * Set the federation for the builder.
         *
         * @param federation The federation.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep1 withFederation(boolean federation) {
            this.federation = federation;
            return this;
        }

        @Override
        public SamlSpPostSingleSignOnSuccessBindings build() {
            return new SamlSpPostSingleSignOnSuccessBindings(this);
        }
    }
}
