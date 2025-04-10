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

import static com.sun.identity.saml2.common.SAML2Constants.BINDING;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ID_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ID_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.USER_ID;

import org.forgerock.openam.scripting.domain.BindingsMap;

import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;

/**
 * Script bindings for the SamlSpUserIdRequestResponse.
 */
public final class SamlSpUserIdRequestResponseBindings extends BaseSamlSpBindings {

    private final String userId;
    private final ManageNameIDRequest idRequest;
    private final ManageNameIDResponse idResponse;
    private final String binding;

    /**
     * Constructor for SamlSpUserIdRequestResponse.
     *
     * @param builder The builder.
     */
    private SamlSpUserIdRequestResponseBindings(Builder builder) {
        super(builder);
        this.userId = builder.userId;
        this.idRequest = builder.idRequest;
        this.idResponse = builder.idResponse;
        this.binding = builder.binding;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the builder.
     */
    static SamlSpUserIdRequestResponseBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap(legacyCommonBindings());
        bindings.put(USER_ID, userId);
        bindings.put(ID_REQUEST, idRequest);
        bindings.put(ID_RESPONSE, idResponse);
        bindings.put(BINDING, binding);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlSpUserIdRequestResponseBindings.
     */
    public interface SamlSpUserIdRequestResponseBindingsStep1 {
        SamlSpUserIdRequestResponseBindingsStep2 withUserId(String userId);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlSpUserIdRequestResponseBindings.
     */
    public interface SamlSpUserIdRequestResponseBindingsStep2 {
        SamlSpUserIdRequestResponseBindingsStep3 withIdRequest(ManageNameIDRequest idRequest);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlSpUserIdRequestResponseBindings.
     */
    public interface SamlSpUserIdRequestResponseBindingsStep3 {
        SamlSpUserIdRequestResponseBindingsStep4 withIdResponse(ManageNameIDResponse idResponse);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlSpUserIdRequestResponseBindings.
     */
    public interface SamlSpUserIdRequestResponseBindingsStep4 {
        SamlSpBindingsStep1<SamlSpUserIdRequestResponseBindings> withBinding(String binding);
    }

    /**
     * Builder object to construct a {@link SamlSpUserIdRequestResponseBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<SamlSpUserIdRequestResponseBindings>
            implements SamlSpUserIdRequestResponseBindingsStep1, SamlSpUserIdRequestResponseBindingsStep2,
            SamlSpUserIdRequestResponseBindingsStep3, SamlSpUserIdRequestResponseBindingsStep4 {

        private String userId;
        private ManageNameIDRequest idRequest;
        private ManageNameIDResponse idResponse;
        private String binding;

        /**
         * Set the userId for the builder.
         *
         * @param userId The userId.
         * @return The next step of the builder.
         */
        public SamlSpUserIdRequestResponseBindingsStep2 withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Set the idRequest for the builder.
         *
         * @param idRequest The {@link ManageNameIDRequest}.
         * @return The next step of the builder.
         */
        public SamlSpUserIdRequestResponseBindingsStep3 withIdRequest(ManageNameIDRequest idRequest) {
            this.idRequest = idRequest;
            return this;
        }

        /**
         * Set the idResponse for the builder.
         *
         * @param idResponse The {@link ManageNameIDResponse}.
         * @return The next step of the builder.
         */
        public SamlSpUserIdRequestResponseBindingsStep4 withIdResponse(ManageNameIDResponse idResponse) {
            this.idResponse = idResponse;
            return this;
        }

        /**
         * Set the binding for the builder.
         *
         * @param binding The binding.
         * @return The next step of the builder.
         */
        public SamlSpBindingsStep1<SamlSpUserIdRequestResponseBindings> withBinding(String binding) {
            this.binding = binding;
            return this;
        }

        @Override
        public SamlSpUserIdRequestResponseBindings build() {
            return new SamlSpUserIdRequestResponseBindings(this);
        }
    }
}
