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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

final class SamlIdpPreSendFailureResponseBindings extends BaseSamlIdpBindings {

    private final String faultCode;
    private final String faultDetail;

    /**
     * Constructor for SamlIdpPreSendFailureResponseBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSendFailureResponseBindings(Builder builder) {
        super(builder);
        this.faultCode = builder.faultCode;
        this.faultDetail = builder.faultDetail;
    }

    static SamlIdpPreSendFailureResponseBindingsStep1 builder() {
        return new Builder();
    }

    /**
     * The signature of these bindings. Used to provide information about available bindings via REST without the
     * stateful underlying objects.
     *
     * @return The signature of this ScriptBindings implementation.
     */
    public static ScriptBindings signature() {
        return new Builder().signature();
    }

    @Override
    public String getDisplayName() {
        return "SAML IDP Pre-send Failure Response Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        List<Binding> v1Bindings = new ArrayList<>(v1CommonBindings());
        v1Bindings.addAll(List.of(
                Binding.of(REQUEST, request, HttpServletRequest.class),
                Binding.of(FAULT_CODE, faultCode, String.class),
                Binding.of(FAULT_DETAIL, faultDetail, String.class),
                Binding.of(RESPONSE, response, HttpServletResponse.class)
        ));
        return v1Bindings;
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
        SamlIdpAdapterCommonBindingsStep1 withFaultDetail(String faultDetail);
    }

    private static final class Builder extends BaseSamlIdpBindings.Builder<Builder> implements
            SamlIdpPreSendFailureResponseBindingsStep1, SamlIdpPreSendFailureResponseBindingsStep2,
            SamlIdpPreSendFailureResponseBindingsStep3, SamlIdpPreSendFailureResponseBindingsStep4 {

        private String faultCode;
        private String faultDetail;

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
        public SamlIdpAdapterCommonBindingsStep1 withFaultDetail(String faultDetail) {
            this.faultDetail = faultDetail;
            return this;
        }

        public SamlIdpPreSendFailureResponseBindings build() {
            return new SamlIdpPreSendFailureResponseBindings(this);
        }
    }
}
