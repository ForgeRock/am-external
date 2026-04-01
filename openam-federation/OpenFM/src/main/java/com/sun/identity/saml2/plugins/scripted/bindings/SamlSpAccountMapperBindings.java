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
 * Copyright 2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ASSERTION_WRAPPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ACCOUNT_MAPPER_SCRIPT_HELPER;

import java.util.Map;
import java.util.Objects;

import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.NextGenScriptBindings;

import com.sun.identity.saml2.plugins.scripted.SpAccountMapperScriptHelper;

/**
 * Script bindings for the SamlSpAccountMapper script.
 */
public class SamlSpAccountMapperBindings implements NextGenScriptBindings {

    private final Map<String,Object> assertion;
    private final String hostedEntityId;
    private final SpAccountMapperScriptHelper spAccountMapperScriptHelper;

    /**
     * Constructor for SamlSpAccountMapperBindings.
     *
     * @param builder The builder.
     */
    private SamlSpAccountMapperBindings(Builder builder) {
        this.assertion = builder.assertion;
        this.hostedEntityId = builder.hostedEntityId;
        this.spAccountMapperScriptHelper = builder.spAccountMapperScriptHelper;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The builder.
     */
    public static SamlSpAccountMapperBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap nextGenBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(ASSERTION_WRAPPER, assertion);
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        bindings.put(SP_ACCOUNT_MAPPER_SCRIPT_HELPER, spAccountMapperScriptHelper);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlSpAccountMapperBindings.
     */
    public interface SamlSpAccountMapperBindingsStep1 {
        SamlSpAccountMapperBindingsStep2 withAssertion(Map<String, Object> assertion);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlSpAccountMapperBindings.
     */
    public interface SamlSpAccountMapperBindingsStep2 {
        SamlSpAccountMapperBindingsStep3 withHostedEntityId(String hostedEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlSpAccountMapperBindings.
     */
    public interface SamlSpAccountMapperBindingsStep3 {
        SamlSpAccountMapperBindingsFinalStep withSpAccountMapperScriptHelper(
                SpAccountMapperScriptHelper spAccountMapperScriptHelper);
    }

    /**
     * Final step of the builder.
     */
    public interface SamlSpAccountMapperBindingsFinalStep {
        SamlSpAccountMapperBindings build();
    }

    /**
     * Builder object to construct a {@link SamlSpAccountMapperBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static class Builder implements
            SamlSpAccountMapperBindingsStep1, SamlSpAccountMapperBindingsStep2,
            SamlSpAccountMapperBindingsStep3, SamlSpAccountMapperBindingsFinalStep {

        private Map<String, Object> assertion;
        private String hostedEntityId;
        private SpAccountMapperScriptHelper spAccountMapperScriptHelper;

        public SamlSpAccountMapperBindings build() {
            Objects.requireNonNull(hostedEntityId, "hostedEntityId must not be null");

            return new SamlSpAccountMapperBindings(this);
        }

        /**
         * Set the assertion for the builder.
         *
         * @param assertion The assertion.
         * @return The next step of the builder.
         */
        public SamlSpAccountMapperBindingsStep2 withAssertion(Map<String, Object> assertion) {
            this.assertion = assertion;
            return this;
        }

        /**
         * Set the hostedEntityId for the builder.
         *
         * @param hostedEntityId The hostedEntityId as String.
         * @return The next step of the builder.
         */
        public SamlSpAccountMapperBindingsStep3 withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }

        /**
         * Set the spAccountMapperScriptHelper for the builder.
         *
         * @param spAccountMapperScriptHelper The {@link SpAccountMapperScriptHelper}.
         * @return The next step of the builder.
         */
        public SamlSpAccountMapperBindingsFinalStep withSpAccountMapperScriptHelper(
                SpAccountMapperScriptHelper spAccountMapperScriptHelper) {
            this.spAccountMapperScriptHelper = spAccountMapperScriptHelper;
            return this;
        }

    }
}
