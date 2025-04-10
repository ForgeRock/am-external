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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;

import com.sun.identity.saml2.plugins.scripted.IdpAdapterScriptHelper;

/**
 * Script bindings for the SamlIdp script.
 */
abstract class BaseSamlIdpBindings implements LegacyScriptBindings {

    protected final String hostedEntityId;
    protected final IdpAdapterScriptHelper idpAdapterScriptHelper;

    /**
     * Constructor for SamlIdpAdapterBindings.
     *
     * @param builder The builder.
     */
    protected BaseSamlIdpBindings(Builder<?> builder) {
        this.hostedEntityId = builder.hostedEntityId;
        this.idpAdapterScriptHelper = builder.idpAdapterScriptHelper;
    }

    protected BindingsMap legacyCommonBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        bindings.put(IDP_ADAPTER_SCRIPT_HELPER, idpAdapterScriptHelper);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterCommonBindings.
     */
    public interface SamlIdpAdapterCommonBindingsStep1<T> {
        SamlIdpAdapterCommonBindingsStep2<T> withHostedEntityId(String hostedEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpAdapterCommonBindings.
     */
    public interface SamlIdpAdapterCommonBindingsStep2<T> {
        SamlIdpAdapterCommonBindingsFinalStep<T> withIdpAdapterScriptHelper(IdpAdapterScriptHelper idpAdapterScriptHelper);
    }

    /**
     * Interface utilised by the fluent builder to define the final step in generating the SamlIdpAdapterCommonBindings.
     */
    public interface SamlIdpAdapterCommonBindingsFinalStep<T> {
        /**
         * Build the bindings
         *
         * @return The bindings.
         */
        T build();
    }

    /**
     * Builder object to construct a {@link BaseSamlIdpBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     * @param <T> The concrete type of bindings returned by the builder.
     */
    protected abstract static class Builder<T extends BaseSamlIdpBindings> implements
            SamlIdpAdapterCommonBindingsStep1<T>, SamlIdpAdapterCommonBindingsStep2<T>,
            SamlIdpAdapterCommonBindingsFinalStep<T> {
        protected String hostedEntityId;
        protected IdpAdapterScriptHelper idpAdapterScriptHelper;

        /**
         * Set the hostedEntityId for the builder.
         *
         * @param hostedEntityId The hostedEntityId as String.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterCommonBindingsStep2<T> withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }

        /**
         * Set the idpAdapterScriptHelper for the builder.
         *
         * @param idpAdapterScriptHelper The {@link IdpAdapterScriptHelper}.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterCommonBindingsFinalStep<T> withIdpAdapterScriptHelper(IdpAdapterScriptHelper idpAdapterScriptHelper) {
            this.idpAdapterScriptHelper = idpAdapterScriptHelper;
            return this;
        }

        protected HttpServletRequest wrapRequest(HttpServletRequest request) {
            return new HttpServletRequestWrapper(request);
        }

        protected HttpServletResponse wrapResponse(HttpServletResponse response) {
            return new HttpServletResponseWrapper(response);
        }
    }
}
