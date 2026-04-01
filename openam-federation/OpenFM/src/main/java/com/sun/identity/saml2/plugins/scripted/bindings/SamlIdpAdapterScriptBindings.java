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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.domain.BindingsMap;
import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.plugins.scripted.IdpAdapterNextGenScriptHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Next Gen bindings for the SAML2 IDP Adapter scripts.
 */
public final class SamlIdpAdapterScriptBindings extends BaseSamlAdapterScriptBindings {
    /** The hosted entity ID. */
    private final String hostedEntityId;
    /** The request ID. */
    private final String reqId;
    /** The relay state. */
    private final String relayState;
    /** The fault code. */
    private final String faultCode;
    /** The fault detail. */
    private final String faultDetail;
    /** Helper for next gen scripts. */
    private final IdpAdapterNextGenScriptHelper idpAdapterScriptHelper;

    @Builder(style = BuilderStyle.STAGED, setterPrefix = "with", factoryMethod = "builder")
    SamlIdpAdapterScriptBindings(
            HttpServletRequest request,
            @Opt HttpServletResponse response,
            @Opt AuthnRequest authnRequest,
            @Opt Response ssoResponse,
            @Opt Object ssoToken,
            @Opt String hostedEntityId,
            @Opt String reqId,
            @Opt String relayState,
            @Opt String faultCode,
            @Opt String faultDetail
    ) {
        super(request, response, authnRequest, ssoResponse, ssoToken, true);
        SSOToken ssoTokenValue = toSsoToken(ssoToken);

        this.hostedEntityId = hostedEntityId;
        this.reqId = reqId;
        this.relayState = relayState;
        this.faultCode = faultCode;
        this.faultDetail = faultDetail;
        this.idpAdapterScriptHelper = new IdpAdapterNextGenScriptHelper(ssoTokenValue, authnRequest);
    }

    @Override
    protected void addSpecificBindings(BindingsMap bindings) {
        bindings.put(IDP_ADAPTER_SCRIPT_HELPER, idpAdapterScriptHelper);
        bindings.put(HOSTED_ENTITYID, hostedEntityId);
        bindings.put(REQ_ID, reqId);
        bindings.put(RELAY_STATE, relayState);
        bindings.put(FAULT_CODE, faultCode);
        bindings.put(FAULT_DETAIL, faultDetail);
    }

    public static SamlIdpAdapterScriptBindingsBuilders.Request builder() {
        return SamlIdpAdapterScriptBindingsBuilder.builder();
    }
}
