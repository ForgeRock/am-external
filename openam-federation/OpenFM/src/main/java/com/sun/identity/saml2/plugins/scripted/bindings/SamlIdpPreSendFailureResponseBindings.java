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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.domain.BindingsMap;

public final class SamlIdpPreSendFailureResponseBindings extends BaseSamlIdpBindings {

    private final String faultCode;
    private final String faultDetail;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * Constructor for SamlIdpPreSendFailureResponseBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSendFailureResponseBindings(Builder builder) {
        super(builder);
        this.faultCode = builder.faultCode;
        this.faultDetail = builder.faultDetail;
        this.request = builder.request;
        this.response = builder.response;
    }

    static SamlIdpPreSendFailureResponseBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(REQUEST, request);
        bindings.put(FAULT_CODE, faultCode);
        bindings.put(FAULT_DETAIL, faultDetail);
        bindings.put(RESPONSE, response);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the
     * SamlIdpPreSendFailureResponseBindings.
     */
    public interface SamlIdpPreSendFailureResponseBindingsStep1 {
        SamlIdpPreSendFailureResponseBindingsStep2 withRequest(HttpServletRequest request);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the
     * SamlIdpPreSendFailureResponseBindings.
     */
    public interface SamlIdpPreSendFailureResponseBindingsStep2 {
        SamlIdpPreSendFailureResponseBindingsStep3 withResponse(HttpServletResponse response);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the
     * SamlIdpPreSendFailureResponseBindings.
     */
    public interface SamlIdpPreSendFailureResponseBindingsStep3 {
        SamlIdpPreSendFailureResponseBindingsStep4 withFaultCode(String faultCode);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the
     * SamlIdpPreSendFailureResponseBindings.
     */
    public interface SamlIdpPreSendFailureResponseBindingsStep4 {
        SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSendFailureResponseBindings> withFaultDetail(String faultDetail);
    }

    /**
     * Builder object to construct a {@link SamlIdpPreSendFailureResponseBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlIdpBindings.Builder<SamlIdpPreSendFailureResponseBindings> implements
            SamlIdpPreSendFailureResponseBindingsStep1, SamlIdpPreSendFailureResponseBindingsStep2,
            SamlIdpPreSendFailureResponseBindingsStep3, SamlIdpPreSendFailureResponseBindingsStep4 {

        private String faultCode;
        private String faultDetail;
        private HttpServletRequest request;
        private HttpServletResponse response;

        /**
         * Set the request for the builder.
         *
         * @param request The request as HttpServletRequest.
         * @return The next step of the builder.
         */
        public SamlIdpPreSendFailureResponseBindingsStep2 withRequest(HttpServletRequest request) {
            this.request = wrapRequest(request);
            return this;
        }

        /**
         * Set the response for the builder.
         *
         * @param response The response as HttpServletResponse.
         * @return The next step of the builder.
         */
        public SamlIdpPreSendFailureResponseBindingsStep3 withResponse(HttpServletResponse response) {
            this.response = wrapResponse(response);
            return this;
        }

        /**
         * Set the faultCode for the builder.
         *
         * @param faultCode The faultCode as String.
         * @return The next step of the builder.
         */
        public SamlIdpPreSendFailureResponseBindingsStep4 withFaultCode(String faultCode) {
            this.faultCode = faultCode;
            return this;
        }

        /**
         * Set the faultDetail for the builder.
         *
         * @param faultDetail The faultDetail as String.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterCommonBindingsStep1<SamlIdpPreSendFailureResponseBindings> withFaultDetail(String faultDetail) {
            this.faultDetail = faultDetail;
            return this;
        }

        public SamlIdpPreSendFailureResponseBindings build() {
            return new SamlIdpPreSendFailureResponseBindings(this);
        }
    }
}
