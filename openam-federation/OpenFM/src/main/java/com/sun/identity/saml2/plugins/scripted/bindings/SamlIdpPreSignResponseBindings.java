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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.Response;

final class SamlIdpPreSignResponseBindings extends BaseSamlIdpBindings {

    private final Response saml2Response;

    /**
     * Constructor for SamlIdpPreSignResponseBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpPreSignResponseBindings(Builder builder) {
        super(builder);
        this.saml2Response = builder.saml2Response;
    }

    static SamlIdpPreSignResponseBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyPreResponseBindings());
        bindings.put(RELAY_STATE, relayState);
        bindings.put(SAML2_RESPONSE, saml2Response);
        bindings.put(SESSION, session);
        return bindings;
    }

    @Override
    public BindingsMap nextGenBindings() {
        BindingsMap bindings = new BindingsMap(nextGenPreResponseBindings());
        bindings.put(RELAY_STATE, relayState);
        bindings.put(SAML2_RESPONSE, saml2Response);
        bindings.put(SESSION, session);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpPreSignResponseBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep1 {
        SamlIdpPreSignResponseBindingsStep2 withRelayState(String relayState);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpPreSignResponseBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep2 {
        SamlIdpPreSignResponseBindingsStep3 withSession(Object session);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpPreSignResponseBindings.
     */
    public interface SamlIdpPreSignResponseBindingsStep3 {
        SamlIdpAdapterPreResponseBindingsStep1 withSaml2Response(Response res);
    }

    private static final class Builder extends BaseSamlIdpBindings.Builder<Builder> implements
            SamlIdpPreSignResponseBindingsStep1, SamlIdpPreSignResponseBindingsStep2,
            SamlIdpPreSignResponseBindingsStep3 {

        private Response saml2Response;

        /**
         * Set the saml2Response for the builder.
         *
         * @param saml2Response The {@link Response}.
         * @return The next step of the builder.
         */
        public SamlIdpAdapterPreResponseBindingsStep1 withSaml2Response(Response saml2Response) {
            this.saml2Response = saml2Response;
            return this;
        }

        public SamlIdpPreSignResponseBindings build() {
            return new SamlIdpPreSignResponseBindings(this);
        }

    }
}
