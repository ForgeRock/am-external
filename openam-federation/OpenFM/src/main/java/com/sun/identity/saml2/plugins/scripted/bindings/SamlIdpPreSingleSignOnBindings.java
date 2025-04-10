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
package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.AuthnRequest;

public final class SamlIdpPreSingleSignOnBindings extends BaseSamlIdpBindings {

    private final HttpServletResponse response;
    private final String requestId;
    private final HttpServletRequest request;
    private final AuthnRequest authnRequest;

    /**
     * Constructor for SamlIdpPreSingleSignOnBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSingleSignOnBindings(Builder builder) {
        super(builder);
        this.response = builder.response;
        this.requestId = builder.requestId;
        this.request = builder.request;
        this.authnRequest = builder.authnRequest;
    }

    static SamlIdpPreSingleSignOnBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(REQUEST, request);
        bindings.put(RESPONSE, response);
        bindings.put(REQ_ID, requestId);
        bindings.put(AUTHN_REQUEST, authnRequest);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpPreSingleSignOnBindings.
     */
    public interface SamlIdpPreSingleSignOnBindingsStep1 {
        SamlIdpPreSingleSignOnBindingsStep2 withResponse(HttpServletResponse response);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpPreSingleSignOnBindings.
     */
    public interface SamlIdpPreSingleSignOnBindingsStep2 {
        SamlIdpPreSingleSignOnBindingsStep3 withRequestId(String requestId);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpPreSingleSignOnBindings.
     */
    public interface SamlIdpPreSingleSignOnBindingsStep3 {
        SamlIdpPreSingleSignOnBindingsStep4 withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlIdpPreSingleSignOnBindings.
     */
    public interface SamlIdpPreSingleSignOnBindingsStep4 {
        SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSingleSignOnBindings> withAuthnRequest(AuthnRequest authnRequest);
    }


    /**
     * Builder object to construct a {@link SamlIdpPreSingleSignOnBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlIdpBindings.Builder<SamlIdpPreSingleSignOnBindings> implements
            SamlIdpPreSingleSignOnBindingsStep1, SamlIdpPreSingleSignOnBindingsStep2,
            SamlIdpPreSingleSignOnBindingsStep3, SamlIdpPreSingleSignOnBindingsStep4 {

        private HttpServletResponse response;
        private String requestId;
        private HttpServletRequest request;
        private AuthnRequest authnRequest;

        public SamlIdpPreSingleSignOnBindings build() {
            return new SamlIdpPreSingleSignOnBindings(this);
        }

        @Override
        public SamlIdpPreSingleSignOnBindingsStep2 withResponse(HttpServletResponse response) {
            this.response = wrapResponse(response);
            return this;
        }

        @Override
        public SamlIdpPreSingleSignOnBindingsStep3 withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        @Override
        public SamlIdpPreSingleSignOnBindingsStep4 withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return this;
        }

        @Override
        public SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSingleSignOnBindings> withAuthnRequest(
                AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }
    }
}
