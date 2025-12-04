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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

public final class SamlIdpPreSignResponseBindings extends BaseSamlIdpBindings {

    private final String relayState;
    private final Object session;
    private final Response samlResponse;
    private final HttpServletRequest request;
    private final AuthnRequest authnRequest;

    /**
     * Constructor for SamlIdpPreSignResponseBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSignResponseBindings(Builder builder) {
        super(builder);
        this.relayState = builder.relayState;
        this.session = builder.session;
        this.samlResponse = builder.samlResponse;
        this.request = builder.request;
        this.authnRequest = builder.authnRequest;
    }

    static SamlIdpPreSignResponseBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(RELAY_STATE, relayState);
        bindings.put(SESSION, session);
        bindings.put(REQUEST, request);
        bindings.put(SAML2_RESPONSE, samlResponse);
        bindings.put(AUTHN_REQUEST, authnRequest);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep1 {
        SamlIdpPreSignResponseBindingsStep2 withRelayState(String relayState);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep2 {
        SamlIdpPreSignResponseBindingsStep3 withSession(Object session);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep3 {
        SamlIdpPreSignResponseBindingsStep4 withSaml2Response(Response samlResponse);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep4 {
        SamlIdpPreSignResponseBindingsStep5 withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 6 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep5 {
        SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSignResponseBindings> withAuthnRequest(AuthnRequest authnRequest);
    }

    /**
     * Builder object to construct a {@link SamlIdpPreSignResponseBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlIdpBindings.Builder<SamlIdpPreSignResponseBindings> implements
            SamlIdpPreSignResponseBindingsStep1, SamlIdpPreSignResponseBindingsStep2,
            SamlIdpPreSignResponseBindingsStep3, SamlIdpPreSignResponseBindingsStep4,
            SamlIdpPreSignResponseBindingsStep5 {

        private String relayState;
        private Object session;
        private Response samlResponse;
        private HttpServletRequest request;
        private AuthnRequest authnRequest;

        public SamlIdpPreSignResponseBindings build() {
            return new SamlIdpPreSignResponseBindings(this);
        }

        @Override
        public SamlIdpPreSignResponseBindingsStep2 withRelayState(String relayState) {
            this.relayState = relayState;
            return this;
        }

        @Override
        public SamlIdpPreSignResponseBindingsStep3 withSession(Object session) {
            this.session = session;
            return this;
        }

        @Override
        public SamlIdpPreSignResponseBindingsStep4 withSaml2Response(Response samlResponse) {
            this.samlResponse = samlResponse;
            return this;
        }

        @Override
        public SamlIdpPreSignResponseBindingsStep5 withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return this;
        }

        @Override
        public SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSignResponseBindings> withAuthnRequest(
                AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }
    }
}
