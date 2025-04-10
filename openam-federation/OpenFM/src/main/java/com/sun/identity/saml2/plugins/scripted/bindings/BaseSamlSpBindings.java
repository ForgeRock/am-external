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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HTTP_CLIENT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ADAPTER_SCRIPT_HELPER;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.forgerock.http.Client;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;

import com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper;

/**
 * Common script bindings for the SamlSp scripts.
 */
public abstract class BaseSamlSpBindings implements LegacyScriptBindings {

    private final String hostedEntityId;
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
        this.hostedEntityId = builder.hostedEntityId;
        this.httpClient = builder.httpClient;
        this.request = builder.request;
        this.response = builder.response;
        this.spAdapterScriptHelper = builder.spAdapterScriptHelper;
    }

    protected BindingsMap legacyCommonBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        bindings.put(HTTP_CLIENT, httpClient);
        bindings.put(REQUEST, request);
        bindings.put(RESPONSE, response);
        bindings.put(SP_ADAPTER_SCRIPT_HELPER, spAdapterScriptHelper);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep1<T> {
        SamlSpBindingsStep2<T> withHostedEntityId(String hostedEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep2<T> {
        SamlSpBindingsStep3<T> withHttpClient(Client httpClient);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep3<T> {
        SamlSpBindingsStep4<T> withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep4<T> {
        SamlSpBindingsStep5<T> withResponse(HttpServletResponse response);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the SamlSpBindings.
     */
    public interface SamlSpBindingsStep5<T> {
        SamlSpBindingsFinalStep<T> withSpAdapterScriptHelper(SpAdapterScriptHelper spAdapterScriptHelper);
    }

    /**
     * Final step of the builder.
     */
    public interface SamlSpBindingsFinalStep<T> {
        /**
         * Build the SamlSpBindings.
         *
         * @return The SamlSpBindings.
         */
        T build();
    }

    /**
     * Abstract builder to construct common {@link BaseSamlSpBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     * @param <T> The concrete type of bindings returned by the builder.
     */
    protected abstract static class Builder<T extends BaseSamlSpBindings> implements
            SamlSpBindingsStep1<T>, SamlSpBindingsStep2<T>, SamlSpBindingsStep3<T>, SamlSpBindingsStep4<T>,
            SamlSpBindingsStep5<T>, SamlSpBindingsFinalStep<T> {
        private String hostedEntityId;
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
        public SamlSpBindingsStep2<T> withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link Client}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep3<T> withHttpClient(Client httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set the request for the builder.
         *
         * @param request The {@link HttpServletRequest}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep4<T> withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return this;
        }

        /**
         * Set the response for the builder.
         *
         * @param response The {@link HttpServletResponse}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep5<T> withResponse(HttpServletResponse response) {
            this.response = wrapResponse(response);
            return this;
        }

        /**
         * Set the spAdapterScriptHelper for the builder.
         *
         * @param spAdapterScriptHelper The {@link SpAdapterScriptHelper}.
         * @return The next step of the builder.
         */
        public SamlSpBindingsFinalStep<T> withSpAdapterScriptHelper(SpAdapterScriptHelper spAdapterScriptHelper) {
            this.spAdapterScriptHelper = spAdapterScriptHelper;
            return this;
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
