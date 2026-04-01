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

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SSO_RESPONSE;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.iplanet.sso.SSOToken;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.NextGenScriptBindings;
import org.forgerock.openam.scripting.api.ScriptedSession;
import com.sun.identity.saml2.plugins.scripted.HttpServletRequestHelper;
import com.sun.identity.saml2.plugins.scripted.HttpServletResponseHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

abstract class BaseSamlAdapterScriptBindings implements NextGenScriptBindings {
    /**
     * Mix-in to ignore problematic properties from HttpServletRequest.
     */
    @JsonIgnoreProperties({"parts", "asyncContext", "dispatcherType", "inputStream", "reader",
            "servletContext", "session", "upgrade", "requestDispatcher", "request"})
    private interface HttpServletRequestMixIn {}

    /**
     * Mix-in to ignore problematic properties from HttpServletResponse.
     */
    @JsonIgnoreProperties({"outputStream", "writer", "response"})
    private interface HttpServletResponseMixIn {}

    protected final ObjectMapper mapper;
    protected final Map<String, Object> request;
    protected final Map<String, Object> response;
    protected final Map<String, Object> authnRequest;
    protected final Map<String, Object> ssoResponse;
    protected final HttpServletRequestHelper requestHelper;
    protected final HttpServletResponseHelper responseHelper;
    protected final ScriptedSession scriptedSession;

    protected BaseSamlAdapterScriptBindings(HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            Response ssoResponse,
            Object ssoToken,
            boolean allowNullResponseHelper) {
        this.mapper = createObjectMapper();
        this.request = convertObjectToMap(request);
        this.response = convertObjectToMap(response);
        this.authnRequest = convertObjectToMap(authnRequest);
        this.ssoResponse = convertObjectToMap(ssoResponse);
        this.requestHelper = new HttpServletRequestHelper(request);
        this.responseHelper = allowNullResponseHelper && response == null
                ? null
                : new HttpServletResponseHelper(response);
        this.scriptedSession = toScriptedSession(ssoToken);
    }

    /**
     * Creates a custom ObjectMapper configured to safely serialize servlet objects.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        mapper.addMixIn(HttpServletRequest.class, HttpServletRequestMixIn.class);
        mapper.addMixIn(HttpServletResponse.class, HttpServletResponseMixIn.class);

        return mapper;
    }

    protected Map<String, Object> convertObjectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return mapper.convertValue(obj, new TypeReference<>() { });
    }

    @Override
    public final BindingsMap nextGenBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(REQUEST, request);
        bindings.put(RESPONSE, response);
        bindings.put(AUTHN_REQUEST, authnRequest);
        bindings.put(SSO_RESPONSE, ssoResponse);
        bindings.put(SESSION, scriptedSession);
        bindings.put(REQUEST_HELPER, requestHelper);
        bindings.put(RESPONSE_HELPER, responseHelper);
        addSpecificBindings(bindings);
        return bindings;
    }

    protected abstract void addSpecificBindings(BindingsMap bindings);

    protected static SSOToken toSsoToken(Object ssoToken) {
        return ssoToken instanceof SSOToken ? (SSOToken) ssoToken : null;
    }

    protected static ScriptedSession toScriptedSession(Object ssoToken) {
        SSOToken token = toSsoToken(ssoToken);
        return token == null ? null : new ScriptedSession(token);
    }
}
