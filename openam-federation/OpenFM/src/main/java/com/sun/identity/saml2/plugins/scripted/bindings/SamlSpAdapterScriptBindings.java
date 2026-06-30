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

import static com.sun.identity.saml2.common.SAML2Constants.BINDING;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAILURE_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ENTITY_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IS_FEDERATION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.PROFILE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.USER_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.domain.BindingsMap;
import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;

import com.sun.identity.saml2.plugins.scripted.AuthnRequestHelper;
import com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

public final class SamlSpAdapterScriptBindings extends BaseSamlAdapterScriptBindings {
    /** The IDP entity ID. */
    private final String idpEntityId;
    /** The profile. */
    private final String profile;
    /** The SP adapter script helper. */
    private final SpAdapterScriptHelper spAdapterScriptHelper;
    /** The AuthnRequest helper. */
    private final AuthnRequestHelper authnRequestHelper;
    /** True if using federation. */
    private final boolean federation;
    /** The user ID. */
    private final String userId;
    /** The failure code. */
    private final Integer failureCode;
    /** The hosted entity ID (SP). */
    private final String hostedEntityId;
    /** The SAML2 binding. */
    private final String binding;

    @Builder(style = BuilderStyle.STAGED, setterPrefix = "with", factoryMethod = "builder")
    SamlSpAdapterScriptBindings(
            HttpServletRequest request,
            @Opt HttpServletResponse response,
            @Opt AuthnRequest authnRequest,
            @Opt Response ssoResponse,
            @Opt Object ssoToken,
            @Opt String idpEntityId,
            @Opt String profile,
            @Opt boolean federation,
            @Opt String userId,
            @Opt Integer failureCode,
            @Opt String hostedEntityId,
            @Opt String binding
    ) {
        super(request, response, authnRequest, ssoResponse, ssoToken, false);

        this.idpEntityId = idpEntityId;
        this.profile = profile;
        this.spAdapterScriptHelper = new SpAdapterScriptHelper();
        this.authnRequestHelper = new AuthnRequestHelper(authnRequest);
        this.federation = federation;
        this.userId = userId;
        this.failureCode = failureCode;
        this.hostedEntityId = hostedEntityId;
        this.binding = binding;
    }

    @Override
    protected void addSpecificBindings(BindingsMap bindings) {
        bindings.put(IDP_ENTITY_ID, idpEntityId);
        bindings.put(PROFILE, profile);
        bindings.put(SP_ADAPTER_SCRIPT_HELPER, spAdapterScriptHelper);
        bindings.put(AUTHN_REQUEST_HELPER, authnRequestHelper);
        bindings.put(IS_FEDERATION, federation);
        bindings.put(USER_ID, userId);
        bindings.put(FAILURE_CODE, failureCode);
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        bindings.put(BINDING, binding);
    }

    public static SamlSpAdapterScriptBindingsBuilders.Request builder() {
        return SamlSpAdapterScriptBindingsBuilder.builder();
    }
}
