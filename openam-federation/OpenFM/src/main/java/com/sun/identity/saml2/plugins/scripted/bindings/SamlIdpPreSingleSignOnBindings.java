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

import java.util.List;

import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

final class SamlIdpPreSingleSignOnBindings extends BaseSamlIdpBindings {

    /**
     * Constructor for SamlIdpPreSingleSignOnBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSingleSignOnBindings(Builder builder) {
        super(builder);
    }

    static SamlIdpAdapterRequestBindingsStep1 builder() {
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
        return "SAML IDP Single Sign On Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        return v1RequestBindings();
    }

    private static final class Builder extends BaseSamlIdpBindings.Builder<Builder> {

        public SamlIdpPreSingleSignOnBindings build() {
            return new SamlIdpPreSingleSignOnBindings(this);
        }
    }
}
