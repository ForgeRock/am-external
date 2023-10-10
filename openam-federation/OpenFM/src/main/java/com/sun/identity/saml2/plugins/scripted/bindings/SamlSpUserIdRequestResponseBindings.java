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

import static com.sun.identity.saml2.common.SAML2Constants.BINDING;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ID_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ID_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.USER_ID;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;

/**
 * Script bindings for the SamlSpUserIdRequestResponse.
 */
final class SamlSpUserIdRequestResponseBindings extends BaseSamlSpBindings {

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
        return "SAML SP User ID Request/Response Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        List<Binding> v1Bindings = new ArrayList<>(v1CommonBindings());
        v1Bindings.addAll(List.of(
                Binding.of(USER_ID, userId, String.class),
                Binding.of(ID_REQUEST, idRequest, ManageNameIDRequest.class),
                Binding.of(ID_RESPONSE, idResponse, ManageNameIDResponse.class),
                Binding.of(BINDING, binding, String.class)
        ));
        return v1Bindings;
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
        SamlSpUserIdRequestResponseBindingsStep5 withBinding(String binding);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the SamlSpUserIdRequestResponseBindings.
     */
    public interface SamlSpUserIdRequestResponseBindingsStep5 {
        SamlSpBindingsStep2 withHostedEntityId(String hostedEntityId);
    }

    /**
     * Builder object to construct a {@link SamlSpUserIdRequestResponseBindings}.
     */
    private static final class Builder extends BaseSamlSpBindings.Builder<Builder>
            implements SamlSpUserIdRequestResponseBindingsStep1, SamlSpUserIdRequestResponseBindingsStep2,
            SamlSpUserIdRequestResponseBindingsStep3, SamlSpUserIdRequestResponseBindingsStep4,
            SamlSpUserIdRequestResponseBindingsStep5 {

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
        public SamlSpUserIdRequestResponseBindingsStep5 withBinding(String binding) {
            this.binding = binding;
            return this;
        }

        @Override
        public SamlSpUserIdRequestResponseBindings build() {
            return new SamlSpUserIdRequestResponseBindings(this);
        }
    }
}
