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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import java.util.List;

import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptHelper;

/**
 * Script bindings for the SamlIdpAttributeMapper script.
 */
public class SamlIdpAttributeMapperBindings extends ScriptBindings {

    private final String hostedEntityId;
    private final IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper;
    private final String realm;
    private final String remoteEntityId;
    private final Object session;

    /**
     * Constructor for SamlIdpAttributeMapperBindings.
     *
     * @param builder The builder.
     */
    private SamlIdpAttributeMapperBindings(Builder builder) {
        super(builder);
        this.hostedEntityId = builder.hostedEntityId;
        this.idpAttributeMapperScriptHelper = builder.idpAttributeMapperScriptHelper;
        this.realm = builder.realm;
        this.remoteEntityId = builder.remoteEntityId;
        this.session = builder.session;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The builder.
     */
    public static SamlIdpAttributeMapperBindingsStep1 builder() {
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
        return "SAML IDP Attribute Mapper Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        return List.of(
                Binding.of(HOSTED_ENTITYID, hostedEntityId, String.class),
                Binding.of(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER, idpAttributeMapperScriptHelper, IdpAttributeMapperScriptHelper.class),
                Binding.of(REALM, realm, String.class),
                Binding.of(REMOTE_ENTITY, remoteEntityId, String.class),
                Binding.of(SESSION, session, Object.class)
        );
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
        SamlIdpAttributeMapperBindingsStep4 withRealm(String realm);
    }

    /**
     * Interface utilised by the fluent builder to define step 4 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep4 {
        SamlIdpAttributeMapperBindingsStep5 withRemoteEntityId(String remoteEntityId);
    }

    /**
     * Interface utilised by the fluent builder to define step 5 in generating the SamlIdpAttributeMapperBindings.
     */
    public interface SamlIdpAttributeMapperBindingsStep5 {
        ScriptBindingsStep1 withSession(Object session);
    }

    /**
     * Builder object to construct a {@link SamlIdpAttributeMapperBindings}.
     */
    private static class Builder extends ScriptBindings.Builder<Builder> implements
            SamlIdpAttributeMapperBindingsStep1, SamlIdpAttributeMapperBindingsStep2,
            SamlIdpAttributeMapperBindingsStep3, SamlIdpAttributeMapperBindingsStep4,
            SamlIdpAttributeMapperBindingsStep5 {

        private String hostedEntityId;
        private IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper;
        private String realm;
        private String remoteEntityId;
        private Object session;

        @Override
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
         * Set the realm for the builder.
         *
         * @param realm The realm as String.
         * @return The next step of the builder.
         */
        public SamlIdpAttributeMapperBindingsStep4 withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Set the remoteEntityId for the builder.
         *
         * @param remoteEntityId The remoteEntityId as String.
         * @return The next step of the builder.
         */
        public SamlIdpAttributeMapperBindingsStep5 withRemoteEntityId(String remoteEntityId) {
            this.remoteEntityId = remoteEntityId;
            return this;
        }

        /**
         * Set the session for the builder.
         *
         * @param session The session as {@link Object}.
         * @return The next step of the builder.
         */
        public ScriptBindingsStep1 withSession(Object session) {
            this.session = session;
            return this;
        }
    }
}
