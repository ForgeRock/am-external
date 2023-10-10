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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ENTITY_ID;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Script bindings for the SamlSpPreSingleSignOnRequest script.
 */
final class SamlSpPreSingleSignOnRequestBindings extends BaseSamlSpBindings {

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
        return "SAML SP Pre Single Sign On Request Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        List<Binding> v1Bindings = new ArrayList<>(v1CommonBindings());
        v1Bindings.addAll(List.of(
                Binding.of(AUTHN_REQUEST, authnRequest, AuthnRequest.class),
                Binding.of(IDP_ENTITY_ID, idpEntityId, String.class)
        ));
        return v1Bindings;
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
        SamlSpBindingsStep1 withIdpEntityId(String idpEntityID);
    }

    /**
     * Builder object to construct a {@link SamlSpPreSingleSignOnRequestBindings}.
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<Builder>
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
        public SamlSpBindingsStep1 withIdpEntityId(String idpEntityId) {
            this.idpEntityId = idpEntityId;
            return this;
        }

        @Override
        public SamlSpPreSingleSignOnRequestBindings build() {
            return new SamlSpPreSingleSignOnRequestBindings(this);
        }
    }
}
