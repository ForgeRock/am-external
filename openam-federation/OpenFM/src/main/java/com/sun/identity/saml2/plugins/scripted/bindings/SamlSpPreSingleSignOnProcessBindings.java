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
package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.PROFILE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Script bindings for the SamlSpPreSingleSignOnProcess script.
 */
public final class SamlSpPreSingleSignOnProcessBindings extends BaseSamlSpBindings {

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
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(AUTHN_REQUEST, authnRequest);
        bindings.put(SAML2_RESPONSE, ssoResponse);
        bindings.put(PROFILE, profile);
        return bindings;
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
        SamlSpBindingsStep1<SamlSpPreSingleSignOnProcessBindings> withProfile(String profile);
    }

    /**
     * Builder object to construct a {@link SamlSpPreSingleSignOnProcessBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<SamlSpPreSingleSignOnProcessBindings>
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
        public SamlSpBindingsStep1<SamlSpPreSingleSignOnProcessBindings> withProfile(String profile) {
            this.profile = profile;
            return this;
        }

        @Override
        public SamlSpPreSingleSignOnProcessBindings build() {
            return new SamlSpPreSingleSignOnProcessBindings(this);
        }
    }
}
