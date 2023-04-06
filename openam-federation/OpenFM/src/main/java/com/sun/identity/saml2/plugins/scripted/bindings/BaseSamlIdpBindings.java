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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.sun.identity.saml2.plugins.scripted.IdpAdapterScriptHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Script bindings for the SamlIdp script.
 */
abstract class BaseSamlIdpBindings extends ScriptBindings {

    protected final AuthnRequest authnRequest;
    protected final String hostedEntityId;
    protected final IdpAdapterScriptHelper idpAdapterScriptHelper;
    protected final String realm;
    protected final String relayState;
    protected final HttpServletRequest request;
    protected final String requestId;
    protected final HttpServletResponse response;
    protected final Object session;

    /**
     * Constructor for SamlIdpAdapterBindings.
     *
     * @param builder The builder.
     */
    protected BaseSamlIdpBindings(Builder builder) {
        super(builder);
        this.authnRequest = builder.authnRequest;
        this.hostedEntityId = builder.hostedEntityId;
        this.idpAdapterScriptHelper = builder.idpAdapterScriptHelper;
        this.realm = builder.realm;
        this.relayState = builder.relayState;
        this.request = builder.request;
        this.requestId = builder.requestId;
        this.response = builder.response;
        this.session = builder.session;
    }

    protected EvaluatorVersionBindings getCommonBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(HOSTED_ENTITYID, hostedEntityId, String.class),
                        Binding.of(REALM, realm, String.class),
                        Binding.of(IDP_ADAPTER_SCRIPT_HELPER, idpAdapterScriptHelper, IdpAdapterScriptHelper.class)
                ))
                .parentBindings(super.getEvaluatorVersionBindings())
                .build();
    }

    protected EvaluatorVersionBindings getPreResponseBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(REQUEST, request, HttpServletRequest.class),
                        Binding.of(AUTHN_REQUEST, authnRequest, AuthnRequest.class)
                ))
                .parentBindings(getCommonBindings())
                .build();
    }

    protected EvaluatorVersionBindings getRequestBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(RESPONSE, response, HttpServletResponse.class),
                        Binding.of(REQ_ID, requestId, String.class)
                ))
                .parentBindings(getPreResponseBindings())
                .build();
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpAdapterRequestBindingsStep1 {
        SamlIdpAdapterRequestBindingsStep2 withResponse(HttpServletResponse response);
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpAdapterRequestBindingsStep2 {
        SamlIdpAdapterPreResponseBindingsStep1 withRequestId(String requestId);
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpAdapterPreResponseBindingsStep1 {
        SamlIdpAdapterPreResponseBindingsStep2 withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpAdapterPreResponseBindingsStep2 {
        SamlIdpAdapterCommonBindingsStep1 withAuthnRequest(AuthnRequest authnRequest);
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterCommonBindings.
     */
    public interface SamlIdpAdapterCommonBindingsStep1 {
        SamlIdpAdapterCommonBindingsStep2 withHostedEntityId(String hostedEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpAdapterCommonBindings.
     */
    public interface SamlIdpAdapterCommonBindingsStep2 {
        SamlIdpAdapterCommonBindingsStep3 withRealm(String realm);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpAdapterCommonBindings.
     */
    public interface SamlIdpAdapterCommonBindingsStep3 {
        ScriptBindingsStep1 withIdpAdapterScriptHelper(IdpAdapterScriptHelper idpAdapterScriptHelper);
    }

    /**
     * Builder object to construct a {@link BaseSamlIdpBindings}.
     */
    public abstract static class Builder<T extends Builder<T>> extends ScriptBindings.Builder<T> implements
            SamlIdpAdapterRequestBindingsStep1, SamlIdpAdapterRequestBindingsStep2,
            SamlIdpAdapterPreResponseBindingsStep1, SamlIdpAdapterPreResponseBindingsStep2,
            SamlIdpAdapterCommonBindingsStep1, SamlIdpAdapterCommonBindingsStep2, SamlIdpAdapterCommonBindingsStep3 {
        protected AuthnRequest authnRequest;
        protected String hostedEntityId;
        protected IdpAdapterScriptHelper idpAdapterScriptHelper;
        protected String realm;
        protected String relayState;
        protected HttpServletRequest request;
        protected String requestId;
        protected HttpServletResponse response;
        protected Object session;

        /**
         * Set the authnRequest for the builder.
         *
         * @param authnRequest The {@link AuthnRequest}.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterCommonBindingsStep1 withAuthnRequest(AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }

        /**
         * Set the hostedEntityId for the builder.
         *
         * @param hostedEntityId The hostedEntityId as String.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterCommonBindingsStep2 withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }

        /**
         * Set the idpAdapterScriptHelper for the builder.
         *
         * @param idpAdapterScriptHelper The {@link IdpAdapterScriptHelper}.
         * @return The next step of the builder.
         */
        public ScriptBindingsStep1 withIdpAdapterScriptHelper(IdpAdapterScriptHelper idpAdapterScriptHelper) {
            this.idpAdapterScriptHelper = idpAdapterScriptHelper;
            return this;
        }

        /**
         * Set the realm for the builder.
         *
         * @param realm The realm as String.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterCommonBindingsStep3 withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Set the requestId for the builder.
         *
         * @param requestId The requestId as String.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterPreResponseBindingsStep1 withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Set the relayState for the builder.
         *
         * @param relayState The relayState as String.
         * @return The next step of the builder.
         */
        public T withRelayState(String relayState) {
            this.relayState = relayState;
            return self();
        }

        /**
         * Set the request for the builder.
         *
         * @param request The {@link HttpServletRequest}.
         * @return The next step of the builder.
         */
        public T withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return self();
        }

        /**
         * Set the response for the builder.
         *
         * @param response The {@link HttpServletResponse}.
         * @return The next step of the builder.
         */
        public T withResponse(HttpServletResponse response) {
            this.response = wrapResponse(response);
            return self();
        }

        /**
         * Set the session for the builder.
         *
         * @param session The session as {@link Object}.
         * @return The next step of the builder.
         */
        public T withSession(Object session) {
            this.session = session;
            return self();
        }

        /**
         * Getter for the servlet request wrapper.
         *
         * @param request The {@link HttpServletRequest}.
         * @return The HttpServletRequestWrapper object wrapping the request.
         */
        private HttpServletRequest wrapRequest(HttpServletRequest request) {
            return new HttpServletRequestWrapper(request);
        }

        /**
         * Getter for the servlet response wrapper.
         *
         * @param response The {@link HttpServletResponse}.
         * @return The HttpServletResponseWrapper object wrapping the response.
         */
        private HttpServletResponse wrapResponse(HttpServletResponse response) {
            return new HttpServletResponseWrapper(response);
        }
    }
}
