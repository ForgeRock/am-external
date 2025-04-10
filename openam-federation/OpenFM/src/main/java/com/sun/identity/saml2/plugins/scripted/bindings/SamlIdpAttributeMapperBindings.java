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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HTTP_CLIENT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import org.forgerock.http.Client;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;

import com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptHelper;

/**
 * Script bindings for the SamlIdpAttributeMapper script.
 */
public class SamlIdpAttributeMapperBindings implements LegacyScriptBindings {

    private final String hostedEntityId;
    private final IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper;
    private final String remoteEntityId;
    private final Object session;
    private final Client httpClient;

    /**
     * Constructor for SamlIdpAttributeMapperBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpAttributeMapperBindings(Builder builder) {
        this.hostedEntityId = builder.hostedEntityId;
        this.idpAttributeMapperScriptHelper = builder.idpAttributeMapperScriptHelper;
        this.remoteEntityId = builder.remoteEntityId;
        this.session = builder.session;
        this.httpClient = builder.httpClient;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The builder.
     */
    public static SamlIdpAttributeMapperBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        bindings.put(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER, idpAttributeMapperScriptHelper);
        bindings.put(REMOTE_ENTITY, remoteEntityId);
        bindings.put(SESSION, session);
        bindings.put(HTTP_CLIENT, httpClient);
        return bindings;
    }

    /**
     * Interface utilised by the fluent builder to define step 1 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep1 {
        SamlIdpAttributeMapperBindingsStep2 withHostedEntityId(String hostedEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 2 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep2 {
        SamlIdpAttributeMapperBindingsStep3 withIdpAttributeMapperScriptHelper(IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper);
    }

    /**
     * Interface utilised by the fluent builder to define step 3 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep3 {
        SamlIdpAttributeMapperBindingsStep4 withRemoteEntityId(String remoteEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep4 {
        SamlIdpAttributeMapperBindingsStep5 withSession(Object session);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep5 {
        SamlIdpAttributeMapperBindingsFinalStep withHttpClient(Client httpClient);
    }

    /**
     * Final step of the builder.
     */
    public interface SamlIdpAttributeMapperBindingsFinalStep {
        SamlIdpAttributeMapperBindings build();
    }

    /**
     * Builder object to construct a {@link SamlIdpAttributeMapperBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static class Builder implements
            SamlIdpAttributeMapperBindingsStep1, SamlIdpAttributeMapperBindingsStep2,
            SamlIdpAttributeMapperBindingsStep3, SamlIdpAttributeMapperBindingsStep4,
            SamlIdpAttributeMapperBindingsStep5, SamlIdpAttributeMapperBindingsFinalStep {

        private String hostedEntityId;
        private IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper;
        private String remoteEntityId;
        private Object session;
        private Client httpClient;

        public SamlIdpAttributeMapperBindings build() {
            return new SamlIdpAttributeMapperBindings(this);
        }

        /**
         * Set the hostedEntityId for the builder.
         *
         * @param hostedEntityId The hostedEntityId as String.
         * @return The next step of the builder.
         */
        public SamlIdpAttributeMapperBindingsStep2 withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }

        /**
         * Set the idpAttributeMapperScriptHelper for the builder.
         *
         * @param idpAttributeMapperScriptHelper The {@link IdpAttributeMapperScriptHelper}.
         * @return The next step of the builder.
         */
        public SamlIdpAttributeMapperBindingsStep3 withIdpAttributeMapperScriptHelper(IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper) {
            this.idpAttributeMapperScriptHelper = idpAttributeMapperScriptHelper;
            return this;
        }

        /**
         * Set the remoteEntityId for the builder.
         *
         * @param remoteEntityId The remoteEntityId as String.
         * @return The next step of the builder.
         */
        public SamlIdpAttributeMapperBindingsStep4 withRemoteEntityId(String remoteEntityId) {
            this.remoteEntityId = remoteEntityId;
            return this;
        }

        /**
         * Set the session for the builder.
         *
         * @param session The session as {@link Object}.
         * @return The next step of the builder.
         */
        public SamlIdpAttributeMapperBindingsStep5 withSession(Object session) {
            this.session = session;
            return this;
        }

        /**
         * Set the httpClient for the builder.
         *
         * @param httpClient The {@link Client}.
         * @return The next step of the builder.
         */
        public Builder withHttpClient(Client httpClient) {
            this.httpClient = httpClient;
            return this;
        }
    }
}
