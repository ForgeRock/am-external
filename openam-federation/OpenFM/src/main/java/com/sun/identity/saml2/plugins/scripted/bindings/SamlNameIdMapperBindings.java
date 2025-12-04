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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.NAMEID_FORMAT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.NAMEID_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentity;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityScriptWrapper;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.saml2.plugins.scripted.NameIDScriptHelper;

/**
 * Script bindings for the SAML2 NameID Mapper scripts.
 */
public class SamlNameIdMapperBindings extends BaseSamlIDPAccountMapperBindings {

    private static final Logger logger = LoggerFactory.getLogger(SamlNameIdMapperBindings.class);

    private final String remoteEntityId;
    private final String nameIDFormat;
    private final Object session;
    private final NameIDScriptHelper nameIDScriptHelper;

    /**
     * Constructor for SamlNameIdMapperBindings.
     *
     * @param builder The builder.
     */
    public SamlNameIdMapperBindings(Builder builder) {
        super(builder);
        this.remoteEntityId = builder.remoteEntityId;
        this.nameIDFormat = builder.nameIDFormat;
        this.session = builder.session;
        this.nameIDScriptHelper = builder.nameIDScriptHelper;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the Builder.
     */
    public static SamlNameIdMapperBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap nextGenBindings() {
        var bindings =  new BindingsMap(commonNextGenBindings());
        bindings.put(NAMEID_SCRIPT_HELPER, nameIDScriptHelper);
        bindings.put(REMOTE_ENTITY, remoteEntityId);
        bindings.put(NAMEID_FORMAT, nameIDFormat);
        bindings.putIfDefined(SESSION, convertToScriptedSession(session));
        bindings.putIfDefined(IDENTITY, convertToScriptedIdentity(session));
        return bindings;
    }

    private ScriptedSession convertToScriptedSession(Object session) {
        if (session instanceof SSOToken) {
            return new ScriptedSession((SSOToken) session);
        } else {
            logger.warn("Session object is not an SSOToken - session binding will be unavailable");
            return null;
        }
    }

    private ScriptedIdentityScriptWrapper convertToScriptedIdentity(Object session) {
        AMIdentity amIdentity;
        if (session instanceof SSOToken) {
            try {
                amIdentity = new AMIdentity((SSOToken) session);
            } catch (SSOException | IdRepoException e) {
                logger.warn("Could not retrieve identity from session - identity binding will be unavailable", e);
                amIdentity = null;
            }
        } else {
            logger.warn("Could not retrieve identity from session - identity binding will be unavailable");
            amIdentity = null;
        }
        return new ScriptedIdentityScriptWrapper(new ScriptedIdentity(amIdentity));
    }

    /**
     * Step 1 of the builder.
     */
    public interface SamlNameIdMapperBindingsStep1 {
        /**
         * Sets the remote entity
         *
         * @param remoteEntityId the remote entity
         * @return the next step of the {@link Builder}
         */
        SamlNameIdMapperBindingsStep2 withRemoteEntityId(String remoteEntityId);
    }

    /**
     * Step 2 of the builder.
     */
    public interface SamlNameIdMapperBindingsStep2 {
        /**
         * Sets the nameIDFormat
         *
         * @param nameIDFormat the nameIDFormat
         * @return the next step of the {@link Builder}
         */
        SamlNameIdMapperBindingsStep3 withNameIDFormat(String nameIDFormat);
    }

    /**
     * Step 3 of the builder.
     */
    public interface SamlNameIdMapperBindingsStep3 {
        /**
         * Sets the session
         *
         * @param session the session
         * @return the next step of the {@link Builder}
         */
        SamlNameIdMapperBindingsStep4 withSession(Object session);
    }

    /**
     * Step 4 of the builder.
     */
    public interface SamlNameIdMapperBindingsStep4 {
        /**
         * Sets the NameIDScriptHelper
         *
         * @param nameIDScriptHelper the NameIDScriptHelper instance
         * @return the next step of the {@link Builder}
         */
        BaseSamlIDPAccountMapperBindingsStep1<SamlNameIdMapperBindings>
        withNameIdScriptHelper(NameIDScriptHelper nameIDScriptHelper);
    }

    /**
     * Builder Object for {@link SamlNameIdMapperBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseSamlIDPAccountMapperBindings.Builder<SamlNameIdMapperBindings>
            implements SamlNameIdMapperBindingsStep1, SamlNameIdMapperBindingsStep2, SamlNameIdMapperBindingsStep3,
            SamlNameIdMapperBindingsStep4 {
        private String remoteEntityId;
        private String nameIDFormat;
        private Object session;
        private NameIDScriptHelper nameIDScriptHelper;

        /**
         * Set the remoteEntityId for the builder.
         *
         * @param remoteEntityId The remote entity ID.
         * @return The next step of the Builder.
         */
        @Override
        public SamlNameIdMapperBindingsStep2 withRemoteEntityId(String remoteEntityId) {
            this.remoteEntityId = remoteEntityId;
            return this;
        }

        /** Set the nameIDFormat for the builder.
         *
         * @param nameIDFormat The nameIDFormat.
         * @return The next step of the Builder.
         */
        @Override
        public SamlNameIdMapperBindingsStep3 withNameIDFormat(String nameIDFormat) {
            this.nameIDFormat = nameIDFormat;
            return this;
        }

        /**
         * Set the session for the builder.
         *
         * @param session The session object.
         * @return The next step of the Builder.
         */
        @Override
        public SamlNameIdMapperBindingsStep4 withSession(Object session) {
            this.session = session;
            return this;
        }

        /**
         * Set the NameIDScriptHelper for the builder.
         *
         * @param nameIDScriptHelper the NameIDScriptHelper instance
         * @return The next step of the Builder.
         */
        @Override
        public BaseSamlIDPAccountMapperBindingsStep1<SamlNameIdMapperBindings> withNameIdScriptHelper(NameIDScriptHelper nameIDScriptHelper) {
            this.nameIDScriptHelper = nameIDScriptHelper;
            return this;
        }

        /**
         * Build the {@link SamlNameIdMapperBindings}.
         *
         * @return The {@link SamlNameIdMapperBindings}.
         */
        public SamlNameIdMapperBindings build() {
            return new SamlNameIdMapperBindings(this);
        }
    }
}
