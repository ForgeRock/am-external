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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;

import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.NextGenScriptBindings;

/**
 * Abstract parent class of the IDP Account Mapper script bindings.
 */
public abstract class BaseSamlIDPAccountMapperBindings implements NextGenScriptBindings {

    /**
     * The hosted entity id binding.
     */
    protected final String hostedEntityId;

    /**
     * Constructor for AbstractSamlIDPAccountMapperBindings.
     *
     * @param builder The builder.
     */
    protected BaseSamlIDPAccountMapperBindings(Builder<?> builder) {
        this.hostedEntityId = builder.hostedEntityId;
    }

    /**
     * Generates a bindings map containing all common bindings for next gen scripts.
     *
     * @return a bindings map containing all common bindings for next gen scripts
     */
    public BindingsMap commonNextGenBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        return bindings;
    }

    /**
     * Step 1 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseSamlIDPAccountMapperBindingsStep1<T> {
        /**
         * Sets the hosted entity id.
         *
         * @param hostedEntityId the hosted entity
         * @return the next step of the {@link Builder}
         */
        BaseSamlIDPAccountMapperBindingsFinalStep<T> withHostedEntityId(String hostedEntityId);
    }

    /**
     * Final step of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseSamlIDPAccountMapperBindingsFinalStep<T> {
        /**
         * Builds the {@link BaseSamlIDPAccountMapperBindings}.
         *
         * @return the {@link BaseSamlIDPAccountMapperBindings}.
         */
        T build();
    }

    /**
     * Builder Object for IDP Account Mapper script bindings.
     */
    protected abstract static class Builder<T extends BaseSamlIDPAccountMapperBindings> implements
            BaseSamlIDPAccountMapperBindingsStep1<T>,
            BaseSamlIDPAccountMapperBindingsFinalStep<T> {
        private String hostedEntityId;

        /**
         * Set the hostedEntityId.
         *
         * @param hostedEntityId the hosted entity ID.
         * @return the {@link Builder}.
         */
        @Override
        public BaseSamlIDPAccountMapperBindingsFinalStep<T> withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }
    }
}
