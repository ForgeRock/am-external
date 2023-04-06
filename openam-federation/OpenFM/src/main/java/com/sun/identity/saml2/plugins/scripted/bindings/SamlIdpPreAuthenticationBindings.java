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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import java.util.List;

import org.forgerock.openam.scripting.domain.EvaluatorVersionBindings;

final class SamlIdpPreAuthenticationBindings extends BaseSamlIdpBindings {

    /**
     * Constructor for SamlIdpPreAuthenticationBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreAuthenticationBindings(Builder builder) {
        super(builder);
    }

    static SamlIdpPreAuthenticationBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    protected EvaluatorVersionBindings getEvaluatorVersionBindings() {
        return EvaluatorVersionBindings.builder()
                .allVersionBindings(List.of(
                        Binding.of(RELAY_STATE, relayState, String.class),
                        Binding.of(SESSION, session, Object.class)
                ))
                .parentBindings(super.getRequestBindings())
                .build();
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreAuthenticationBindingsStep1 {
        SamlIdpPreAuthenticationBindingsStep2 withRelayState(String relayState);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpAdapterRequestBindings.
     */
    public interface SamlIdpPreAuthenticationBindingsStep2 {
        SamlIdpAdapterRequestBindingsStep1 withSession(Object session);
    }

    private static final class Builder extends BaseSamlIdpBindings.Builder<Builder> implements
            SamlIdpPreAuthenticationBindingsStep1, SamlIdpPreAuthenticationBindingsStep2 {

        public SamlIdpPreAuthenticationBindings build() {
            return new SamlIdpPreAuthenticationBindings(this);
        }
    }
}
