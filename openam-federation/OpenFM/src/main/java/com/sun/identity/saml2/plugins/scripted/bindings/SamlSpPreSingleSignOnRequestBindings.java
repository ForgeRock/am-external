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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ENTITY_ID;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Script bindings for the SamlSpPreSingleSignOnRequest script.
 */
public final class SamlSpPreSingleSignOnRequestBindings extends BaseSamlSpBindings {

    private final AuthnRequest authnRequest;
    private final String idpEntityId;

    /**
     * Constructor for SamlSpPreSingleSignOnRequest.
     *
     * @param builder The builder.
     */
    private SamlSpPreSingleSignOnRequestBindings(Builder builder) {
        super(builder);
        this.authnRequest = builder.authnRequest;
        this.idpEntityId = builder.idpEntityId;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the builder.
     */
    static SamlSpPreSingleSignOnRequestBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(AUTHN_REQUEST, authnRequest);
        bindings.put(IDP_ENTITY_ID, idpEntityId);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the
     * SamlSpPreSingleSignOnRequestBindings.
     */
    public interface SamlSpPreSingleSignOnRequestBindingsStep1 {
        SamlSpPreSingleSignOnRequestBindingsStep2 withAuthnRequest(AuthnRequest authnRequest);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the
     * SamlSpPreSingleSignOnRequestBindings.
     */
    public interface SamlSpPreSingleSignOnRequestBindingsStep2 {
        SamlSpBindingsStep1<SamlSpPreSingleSignOnRequestBindings> withIdpEntityId(String idpEntityID);
    }

    /**
     * Builder object to construct a {@link SamlSpPreSingleSignOnRequestBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<SamlSpPreSingleSignOnRequestBindings>
            implements SamlSpPreSingleSignOnRequestBindingsStep1, SamlSpPreSingleSignOnRequestBindingsStep2 {

        private AuthnRequest authnRequest;

        private String idpEntityId;

        /**
         * Set the authnRequest for the builder.
         *
         * @param authnRequest The {@link AuthnRequest}.
         * @return The next step of the builder.
         */
        public SamlSpPreSingleSignOnRequestBindingsStep2 withAuthnRequest(AuthnRequest authnRequest) {
            this.authnRequest = authnRequest;
            return this;
        }

        /**
         * Set the idpEntityID for the builder.
         *
         * @param idpEntityId The idpEntityID.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep1<SamlSpPreSingleSignOnRequestBindings> withIdpEntityId(String idpEntityId) {
            this.idpEntityId = idpEntityId;
            return this;
        }

        @Override
        public SamlSpPreSingleSignOnRequestBindings build() {
            return new SamlSpPreSingleSignOnRequestBindings(this);
        }
    }
}
