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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.AuthnRequest;

public final class SamlIdpPreSendResponseBindings extends BaseSamlIdpBindings {

    private final String relayState;
    private final Object session;
    private final HttpServletResponse response;
    private final HttpServletRequest request;
    private final String requestId;
    private final AuthnRequest authnRequest;

    /**
     * Constructor for SamlIdpPreSendResponseBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSendResponseBindings(Builder builder) {
        super(builder);
        this.relayState = builder.relayState;
        this.session = builder.session;
        this.response = builder.response;
        this.request = builder.request;
        this.requestId = builder.requestId;
        this.authnRequest = builder.authnRequest;
    }

    static SamlIdpPreSendResponseBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(RELAY_STATE, relayState);
        bindings.put(SESSION, session);
        bindings.put(REQUEST, request);
        bindings.put(RESPONSE, response);
        bindings.put(REQ_ID, requestId);
        bindings.put(AUTHN_REQUEST, authnRequest);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSendResponseBindingsStep1 {
        SamlIdpPreSendResponseBindingsStep2 withRelayState(String relayState);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSendResponseBindingsStep2 {
        SamlIdpPreSendResponseBindingsStep3 withSession(Object session);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSendResponseBindingsStep3 {
        SamlIdpPreSendResponseBindingsStep4 withResponse(HttpServletResponse response);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSendResponseBindingsStep4 {
        SamlIdpPreSendResponseBindingsStep5 withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSendResponseBindingsStep5 {
        SamlIdpPreSendResponseBindingsStep6 withRequestId(String requestId);
    }

    /**
     * Interface utilised by the fluent builder to define step 6 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreSendResponseBindingsStep6 {
        SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSendResponseBindings> withAuthnRequest(AuthnRequest authnRequest);
    }

    /**
     * Builder object to construct a {@link SamlIdpPreSendResponseBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlIdpBindings.Builder<SamlIdpPreSendResponseBindings> implements
            SamlIdpPreSendResponseBindingsStep1, SamlIdpPreSendResponseBindingsStep2,
            SamlIdpPreSendResponseBindingsStep3, SamlIdpPreSendResponseBindingsStep4,
            SamlIdpPreSendResponseBindingsStep5, SamlIdpPreSendResponseBindingsStep6 {

        private String relayState;
        private Object session;
        private HttpServletResponse response;
        private HttpServletRequest request;
        private String requestId;
        private AuthnRequest authnRequest;

        public SamlIdpPreSendResponseBindings build() {
            return new SamlIdpPreSendResponseBindings(this);
        }

        @Override
        public SamlIdpPreSendResponseBindingsStep2 withRelayState(String relayState) {
            this.relayState = relayState;
            return this;
        }

        @Override
        public SamlIdpPreSendResponseBindingsStep3 withSession(Object session) {
            this.session = session;
            return this;
        }

        @Override
        public SamlIdpPreSendResponseBindingsStep4 withResponse(HttpServletResponse response) {
            this.response = wrapResponse(response);
            return this;
        }

        @Override
        public SamlIdpPreSendResponseBindingsStep5 withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return this;
        }

        @Override
        public SamlIdpPreSendResponseBindingsStep6 withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        @Override
        public SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSendResponseBindings> withAuthnRequest(
                AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }
    }
}
