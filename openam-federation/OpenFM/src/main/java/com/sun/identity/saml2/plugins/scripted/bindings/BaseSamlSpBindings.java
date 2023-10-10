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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HTTP_CLIENT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ADAPTER_SCRIPT_HELPER;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.forgerock.http.Client;
import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper;

/**
 * Common script bindings for the SamlSp scripts.
 */
public abstract class BaseSamlSpBindings extends ScriptBindings {

    private final String hostedEntityId;
    private final String realm;
    private final Client httpClient;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final SpAdapterScriptHelper spAdapterScriptHelper;

    /**
     * Constructor for SamlSpBindings.
     *
     * @param builder The builder.
     */
    protected BaseSamlSpBindings(Builder builder) {
        super(builder);
        this.hostedEntityId = builder.hostedEntityId;
        this.realm = builder.realm;
        this.httpClient = builder.httpClient;
        this.request = builder.request;
        this.response = builder.response;
        this.spAdapterScriptHelper = builder.spAdapterScriptHelper;
    }

    protected List<Binding> v1CommonBindings() {
        return List.of(
                Binding.of(HOSTED_ENTITYID, hostedEntityId, String.class),
                Binding.of(SAML2Constants.ScriptParams.REALM, realm, String.class),
                Binding.of(HTTP_CLIENT, httpClient, Client.class),
                Binding.of(REQUEST, request, HttpServletRequest.class),
                Binding.of(RESPONSE, response, HttpServletResponse.class),
                Binding.of(SP_ADAPTER_SCRIPT_HELPER, spAdapterScriptHelper, SpAdapterScriptHelper.class)
        );
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep1 {
        SamlSpBindingsStep2 withHostedEntityId(String hostedEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep2 {
        SamlSpBindingsStep3 withRealm(String realm);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep3 {
        SamlSpBindingsStep4 withHttpClient(Client httpClient);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep4 {
        SamlSpBindingsStep5 withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep5 {
        SamlSpBindingsStep6 withResponse(HttpServletResponse response);
    }

    /**
     * Interface utilised by the fluent builder to define step 6 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep6 {
        ScriptBindingsStep1 withSpAdapterScriptHelper(SpAdapterScriptHelper spAdapterScriptHelper);
    }

    /**
     * Abstract builder to construct common {@link BaseSamlSpBindings}.
     */
    public abstract static class Builder<T extends Builder<T>> extends ScriptBindings.Builder<T> implements
            SamlSpBindingsStep1, SamlSpBindingsStep2, SamlSpBindingsStep3, SamlSpBindingsStep4, SamlSpBindingsStep5,
            SamlSpBindingsStep6 {
        private String hostedEntityId;
        private String realm;
        private Client httpClient;
        private HttpServletRequest request;
        private HttpServletResponse response;
        private SpAdapterScriptHelper spAdapterScriptHelper;

        /**
         * Set the hostedEntityId for the builder.
         *
         * @param hostedEntityId The hostedEntityId as String.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep2 withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return self();
        }

        /**
         * Set the realm for the builder.
         *
         * @param realm The realm as String.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep3 withRealm(String realm) {
            this.realm = realm;
            return self();
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link Client}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep4 withHttpClient(Client httpClient) {
            this.httpClient = httpClient;
            return self();
        }

        /**
         * Set the request for the builder.
         *
         * @param request The {@link HttpServletRequest}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep5 withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return self();
        }

        /**
         * Set the response for the builder.
         *
         * @param response The {@link HttpServletResponse}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep6 withResponse(HttpServletResponse response) {
            this.response = wrapResponse(response);
            return self();
        }

        /**
         * Set the spAdapterScriptHelper for the builder.
         *
         * @param spAdapterScriptHelper The {@link SpAdapterScriptHelper}.
         * @return The next step of the builder.
         */
        public ScriptBindingsStep1 withSpAdapterScriptHelper(SpAdapterScriptHelper spAdapterScriptHelper) {
            this.spAdapterScriptHelper = spAdapterScriptHelper;
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
