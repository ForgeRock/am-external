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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SSO_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.plugins.scripted.HttpServletRequestHelper;
import com.sun.identity.saml2.plugins.scripted.HttpServletResponseHelper;
import com.sun.identity.saml2.plugins.scripted.IdpAdapterNextGenScriptHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SamlIdpAdapterScriptBindings}.
 */
@ExtendWith(MockitoExtension.class)
public class SamlIdpAdapterScriptBindingsTest {

    private static final String TEST_HOSTED_ENTITY_ID = "http://idp.example.com";
    private static final String TEST_REQ_ID = "request-id";
    private static final String TEST_RELAY_STATE = "relay-state";
    private static final String TEST_FAULT_CODE = "fault-code";
    private static final String TEST_FAULT_DETAIL = "fault-detail";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private SSOToken ssoToken;
    @Mock
    private AuthnRequest authnRequest;
    @Mock
    private Response ssoResponse;


    @Test
    void shouldBuildBindingsWithAllParameters() {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withAuthnRequest(authnRequest)
                .withSsoResponse(ssoResponse)
                .withSsoToken(ssoToken)
                .withHostedEntityId(TEST_HOSTED_ENTITY_ID)
                .withReqId(TEST_REQ_ID)
                .withRelayState(TEST_RELAY_STATE)
                .withFaultCode(TEST_FAULT_CODE)
                .withFaultDetail(TEST_FAULT_DETAIL)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertThat(bindingsMap.get(HOSTED_ENTITYID)).isEqualTo(TEST_HOSTED_ENTITY_ID);
        assertThat(bindingsMap.get(REQ_ID)).isEqualTo(TEST_REQ_ID);
        assertThat(bindingsMap.get(RELAY_STATE)).isEqualTo(TEST_RELAY_STATE);
        assertThat(bindingsMap.get(FAULT_CODE)).isEqualTo(TEST_FAULT_CODE);
        assertThat(bindingsMap.get(FAULT_DETAIL)).isEqualTo(TEST_FAULT_DETAIL);
        assertThat(bindingsMap.get(IDP_ADAPTER_SCRIPT_HELPER)).isInstanceOf(IdpAdapterNextGenScriptHelper.class);
        assertThat(bindingsMap.get(SESSION)).isInstanceOf(ScriptedSession.class);
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
        assertThat(bindingsMap.get(AUTHN_REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(SSO_RESPONSE)).isInstanceOf(Map.class);
    }

    @Test
    void shouldBuildBindingsWithMinimalParameters() {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertThat(bindingsMap.get(HOSTED_ENTITYID)).isNull();
        assertThat(bindingsMap.get(REQ_ID)).isNull();
        assertThat(bindingsMap.get(RELAY_STATE)).isNull();
        assertThat(bindingsMap.get(FAULT_CODE)).isNull();
        assertThat(bindingsMap.get(FAULT_DETAIL)).isNull();
        assertThat(bindingsMap.get(SESSION)).isNull();
        assertThat(bindingsMap.get(AUTHN_REQUEST)).isNull();
        assertThat(bindingsMap.get(SSO_RESPONSE)).isNull();
        assertThat(bindingsMap.get(IDP_ADAPTER_SCRIPT_HELPER)).isInstanceOf(IdpAdapterNextGenScriptHelper.class);
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
    }

    @Test
    void shouldHandleNullSsoToken() {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoToken(null)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(SESSION)).isNull();
        assertThat(bindingsMap.get(IDP_ADAPTER_SCRIPT_HELPER)).isNotNull();
    }

    @Test
    void shouldHandleNonSsoTokenObject() {
        // Given
        Object notAnSsoToken = "not-an-sso-token";

        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoToken(notAnSsoToken)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(SESSION)).isNull();
    }

    @Test
    void shouldSetSessionPropertiesOnSsoToken() throws Exception {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoToken(ssoToken)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        ScriptedSession session = (ScriptedSession) bindingsMap.get(SESSION);
        session.setProperty("testKey", "testValue");

        // Then
        assertThat(session).isNotNull().isInstanceOf(ScriptedSession.class);
        verify(ssoToken).setProperty("testKey", "testValue");
    }

    @Test
    void shouldSetRequestAttributeOnHttpServletRequest() {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        HttpServletRequestHelper requestHelper = (HttpServletRequestHelper) bindingsMap.get(REQUEST_HELPER);
        requestHelper.setAttribute("testAttribute", "testValue");

        // Then
        verify(request).setAttribute("testAttribute", "testValue");
    }

    @Test
    void shouldSetResponseHeaderOnHttpServletResponse() {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        HttpServletResponseHelper responseHelper = (HttpServletResponseHelper) bindingsMap.get(RESPONSE_HELPER);
        responseHelper.setHeader("testHeader", "testValue");

        // Then
        verify(response).setHeader("testHeader", "testValue");
    }

    @Test
    void shouldSendRedirectViaHttpServletResponse() throws Exception {
        // When
        SamlIdpAdapterScriptBindings bindings = SamlIdpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        HttpServletResponseHelper responseHelper = (HttpServletResponseHelper) bindingsMap.get(RESPONSE_HELPER);
        responseHelper.sendRedirect("https://example.com");

        // Then
        verify(response).sendRedirect("https://example.com");
    }
}
