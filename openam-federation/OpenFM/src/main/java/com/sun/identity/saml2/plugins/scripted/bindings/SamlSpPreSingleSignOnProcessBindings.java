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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.PROFILE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;

import java.util.List;

import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;

import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Script bindings for the SamlSpPreSingleSignOnProcess script.
 */
final class SamlSpPreSingleSignOnProcessBindings extends BaseSamlSpBindings {

    private final AuthnRequest authnRequest;
    private final Response ssoResponse;
    private final String profile;

    /**
     * Constructor for SamlSpPreSingleSignOnProcessBindings.
     *
     * @param builder The builder.
     */
    private SamlSpPreSingleSignOnProcessBindings(SamlSpPreSingleSignOnProcessBindings.Builder builder) {
        super(builder);
        this.authnRequest = builder.authnRequest;
        this.ssoResponse = builder.ssoResponse;
        this.profile = builder.profile;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the builder.
     */
    static SamlSpPreSingleSignOnProcessBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    protected EvaluatorVersionBindings getEvaluatorVersionBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(AUTHN_REQUEST, authnRequest, AuthnRequest.class),
                        Binding.of(SAML2_RESPONSE, ssoResponse, Response.class),
                        Binding.of(PROFILE, profile, String.class)
                ))
                .parentBindings(super.getEvaluatorVersionBindings())
                .build();
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the
     * SamlSpPreSingleSignOnProcessBindings.
     */
    public interface SamlSpPreSingleSignOnProcessBindingsStep1 {
        SamlSpPreSingleSignOnProcessBindingsStep2 withAuthnRequest(AuthnRequest authnRequest);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the
     * SamlSpPreSingleSignOnProcessBindings.
     */
    public interface SamlSpPreSingleSignOnProcessBindingsStep2 {
        SamlSpPreSingleSignOnProcessBindingsStep3 withSsoResponse(Response ssoResponse);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the
     * SamlSpPreSingleSignOnProcessBindings.
     */
    public interface SamlSpPreSingleSignOnProcessBindingsStep3 {
        SamlSpBindingsStep1 withProfile(String profile);
    }

    /**
     * Builder object to construct a {@link SamlSpPreSingleSignOnProcessBindings}.
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<Builder>
            implements SamlSpPreSingleSignOnProcessBindingsStep1,
            SamlSpPreSingleSignOnProcessBindingsStep2, SamlSpPreSingleSignOnProcessBindingsStep3 {

        private AuthnRequest authnRequest;
        private Response ssoResponse;
        private String profile;

        /**
         * Set the authnRequest for the builder.
         *
         * @param authnRequest The {@link AuthnRequest}.
         * @return The next step of the builder.
         */
        public SamlSpPreSingleSignOnProcessBindingsStep2 withAuthnRequest(AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }

        /**
         * Set the ssoResponse for the builder.
         *
         * @param ssoResponse The {@link Response}.
         * @return The next step of the builder.
         */
        public SamlSpPreSingleSignOnProcessBindingsStep3 withSsoResponse(Response ssoResponse) {
            this.ssoResponse = ssoResponse;
            return this;
        }

        /**
         * Set the profile for the builder.
         *
         * @param profile The profile.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep1 withProfile(String profile) {
            this.profile = profile;
            return this;
        }

        @Override
        public SamlSpPreSingleSignOnProcessBindings build() {
            return new SamlSpPreSingleSignOnProcessBindings(this);
        }
    }
}
