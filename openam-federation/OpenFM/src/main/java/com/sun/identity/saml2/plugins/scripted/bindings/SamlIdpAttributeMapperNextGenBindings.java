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
 * Copyright 2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import java.util.Optional;

import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.NextGenScriptBindings;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptNextGenHelper;

/**
 * Defines the bindings available for Next Generation SAML2 IDP Attribute Mapper scripts.
 */
public final class SamlIdpAttributeMapperNextGenBindings implements NextGenScriptBindings {

    private final IdpAttributeMapperScriptNextGenHelper idpAttributeMapperScriptHelper;
    private final ScriptedSession scriptedSession;
    private final String realm;
    private final String remoteEntityId;
    private final String hostedEntityId;

    private SamlIdpAttributeMapperNextGenBindings(Builder builder) {
        SSOToken ssoToken = builder.ssoToken instanceof SSOToken ? (SSOToken) builder.ssoToken : null;

        this.scriptedSession = Optional.ofNullable(ssoToken).map(ScriptedSession::new).orElse(null);
        this.realm = builder.realm;
        this.remoteEntityId = builder.remoteEntityId;
        this.hostedEntityId = builder.hostedEntityId;
        this.idpAttributeMapperScriptHelper = new IdpAttributeMapperScriptNextGenHelper(ssoToken, realm,
                remoteEntityId, hostedEntityId);
    }

    @Override
    public BindingsMap nextGenBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER, idpAttributeMapperScriptHelper);
        bindings.put(SESSION, scriptedSession);
        bindings.put(REALM, realm);
        bindings.put(REMOTE_ENTITY, remoteEntityId);
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        return bindings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object ssoToken;
        private String realm;
        private String remoteEntityId;
        private String hostedEntityId;

        public Builder withSsoToken(Object ssoToken) {
            this.ssoToken = ssoToken;
            return this;
        }

        public Builder withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        public Builder withRemoteEntityId(String remoteEntityId) {
            this.remoteEntityId = remoteEntityId;
            return this;
        }

        public Builder withHostedEntityId(String hostedEntityId) {
            this.hostedEntityId = hostedEntityId;
            return this;
        }

        public SamlIdpAttributeMapperNextGenBindings build() {
            return new SamlIdpAttributeMapperNextGenBindings(this);
        }
    }
}
