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
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAILURE_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ENTITY_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IS_FEDERATION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.PROFILE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SSO_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.plugins.scripted.AuthnRequestHelper;
import com.sun.identity.saml2.plugins.scripted.HttpServletRequestHelper;
import com.sun.identity.saml2.plugins.scripted.HttpServletResponseHelper;
import com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Unit tests for {@link SamlSpAdapterScriptBindings}.
 */
public class SamlSpAdapterScriptBindingsTest {

    private static final String TEST_IDP_ENTITY_ID = "http://idp.example.com";
    private static final String TEST_HOSTED_ENTITY_ID = "http://sp.example.com";
    private static final String TEST_PROFILE = "HTTP-POST";
    private static final String TEST_USER_ID = "testUser";
    private static final String TEST_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    private static final Integer TEST_FAILURE_CODE = 5;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private SSOToken ssoToken;
    private AuthnRequest authnRequest;
    private Response ssoResponse;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        ssoToken = mock(SSOToken.class);
        authnRequest = mock(AuthnRequest.class);
        ssoResponse = mock(Response.class);
    }

    @Test
    void shouldBuildBindingsWithAllParameters() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withAuthnRequest(authnRequest)
                .withSsoResponse(ssoResponse)
                .withSsoToken(ssoToken)
                .withIdpEntityId(TEST_IDP_ENTITY_ID)
                .withHostedEntityId(TEST_HOSTED_ENTITY_ID)
                .withProfile(TEST_PROFILE)
                .withUserId(TEST_USER_ID)
                .withBinding(TEST_BINDING)
                .withFailureCode(TEST_FAILURE_CODE)
                .withFederation(true)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertThat(bindingsMap.get(IDP_ENTITY_ID)).isEqualTo(TEST_IDP_ENTITY_ID);
        assertThat(bindingsMap.get(HOSTED_ENTITYID)).isEqualTo(TEST_HOSTED_ENTITY_ID);
        assertThat(bindingsMap.get(PROFILE)).isEqualTo(TEST_PROFILE);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
        assertThat(bindingsMap.get(AUTHN_REQUEST_HELPER)).isInstanceOf(AuthnRequestHelper.class);
        assertThat(bindingsMap.get(USER_ID)).isEqualTo(TEST_USER_ID);
        assertThat(bindingsMap.get(BINDING)).isEqualTo(TEST_BINDING);
        assertThat(bindingsMap.get(FAILURE_CODE)).isEqualTo(TEST_FAILURE_CODE);
        assertThat(bindingsMap.get(IS_FEDERATION)).isEqualTo(true);

        assertThat(bindingsMap.get(SP_ADAPTER_SCRIPT_HELPER)).isInstanceOf(SpAdapterScriptHelper.class);
        assertThat(bindingsMap.get(SESSION)).isInstanceOf(ScriptedSession.class);
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(AUTHN_REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(SSO_RESPONSE)).isInstanceOf(Map.class);
    }

    @Test
    void shouldBuildBindingsWithMinimalParameters() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertThat(bindingsMap.get(IDP_ENTITY_ID)).isNull();
        assertThat(bindingsMap.get(HOSTED_ENTITYID)).isNull();
        assertThat(bindingsMap.get(PROFILE)).isNull();
        assertThat(bindingsMap.get(USER_ID)).isNull();
        assertThat(bindingsMap.get(BINDING)).isNull();
        // Helpers should still be created
        assertThat(bindingsMap.get(IS_FEDERATION)).isEqualTo(false);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
        assertThat(bindingsMap.get(AUTHN_REQUEST_HELPER)).isInstanceOf(AuthnRequestHelper.class);
        assertThat(bindingsMap.get(SESSION)).isNull();
        assertThat(bindingsMap.get(AUTHN_REQUEST)).isNull();
        assertThat(bindingsMap.get(SSO_RESPONSE)).isNull();

        // Helper should still be created
        assertThat(bindingsMap.get(SP_ADAPTER_SCRIPT_HELPER)).isInstanceOf(SpAdapterScriptHelper.class);
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
    }

    @Test
    void shouldHandleNullSsoToken() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoToken(null)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(SESSION)).isNull();
        assertThat(bindingsMap.get(SP_ADAPTER_SCRIPT_HELPER)).isNotNull();
    }

    @Test
    void shouldHandleNonSsoTokenObject() {
        // Given
        Object notAnSsoToken = "This is not an SSOToken";

        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoToken(notAnSsoToken)
                .build();

        // Then should not create ScriptedSession for non-SSOToken
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(SESSION)).isNull();
    }

    @Test
    void shouldSetSessionPropertiesOnSsoToken() throws Exception {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoToken(ssoToken)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        ScriptedSession session = (ScriptedSession) bindingsMap.get(SESSION);
        session.setProperty("testKey", "testValue");

        // Then verify that the SSOToken is actually used by the ScriptedSession
        assertThat(bindingsMap.get(SESSION))
                .isNotNull()
                .isInstanceOf(ScriptedSession.class);
        verify(ssoToken).setProperty("testKey", "testValue");
    }

    @Test
    void shouldSetRequestAttributeOnHttpServletRequest() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        HttpServletRequestHelper requestHelper = (HttpServletRequestHelper) bindingsMap.get(REQUEST_HELPER);
        requestHelper.setAttribute("testAttribute", "testValue");

        // Then verify setAttribute is called on HttpServletRequest
        assertThat(requestHelper).isNotNull();
        verify(request).setAttribute("testAttribute", "testValue");
    }

    @Test
    void shouldSetResponseHeaderOnHttpServletResponse() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        HttpServletResponseHelper responseHelper = (HttpServletResponseHelper) bindingsMap.get(RESPONSE_HELPER);
        responseHelper.setHeader("X-Custom-Header", "customValue");

        // Then verify that setHeader is called on HttpServletResponse
        assertThat(responseHelper).isNotNull();
        verify(response).setHeader("X-Custom-Header", "customValue");
    }

    @Test
    void shouldAddResponseHeaderOnHttpServletResponse() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        HttpServletResponseHelper responseHelper = (HttpServletResponseHelper) bindingsMap.get(RESPONSE_HELPER);
        responseHelper.addHeader("X-Custom-Header", "customValue");

        // Then verify that addHeader is called on HttpServletResponse
        assertThat(responseHelper).isNotNull();
        verify(response).addHeader("X-Custom-Header", "customValue");
    }

    @Test
    void shouldConvertObjectsToMapsSuccessfully() {
        // When - Build with all convertible objects
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withAuthnRequest(authnRequest)
                .withSsoResponse(ssoResponse)
                .build();

        // Then - All objects should be converted to Maps
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertIsMap(bindingsMap, REQUEST, "HttpServletRequest should be converted to Map");
        assertIsMap(bindingsMap, RESPONSE, "HttpServletResponse should be converted to Map");
        assertIsMap(bindingsMap, AUTHN_REQUEST, "AuthnRequest should be converted to Map");
        assertIsMap(bindingsMap, SSO_RESPONSE, "SSO Response should be converted to Map");
    }

    @Test
    void shouldHandleNullAuthnRequest() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withAuthnRequest(null)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(AUTHN_REQUEST)).isNull();
        assertThat(bindingsMap.get(AUTHN_REQUEST_HELPER)).isInstanceOf(AuthnRequestHelper.class);
        assertThat(((AuthnRequestHelper) bindingsMap.get(AUTHN_REQUEST_HELPER)).getDestination()).isNull();
    }

    @Test
    void shouldSetDestinationViaAuthnRequestHelper() throws Exception {
        String updatedDestination = "https://idp.example.com/updated";

        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withAuthnRequest(authnRequest)
                .build();

        BindingsMap bindingsMap = bindings.nextGenBindings();
        AuthnRequestHelper helper = (AuthnRequestHelper) bindingsMap.get(AUTHN_REQUEST_HELPER);
        helper.setDestination(updatedDestination);

        // Then
        assertThat(helper).isNotNull();
        verify(authnRequest).setDestination(updatedDestination);
    }

    @Test
    void shouldHandleNullSsoResponse() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withSsoResponse(null)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(SSO_RESPONSE)).isNull();
    }

    @Test
    void shouldSetFederationToTrue() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withFederation(true)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(IS_FEDERATION)).isEqualTo(true);
    }

    @Test
    void shouldSetFederationToFalse() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withFederation(false)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(IS_FEDERATION)).isEqualTo(false);
    }

    @Test
    void shouldIncludeAllBindingKeys() {
        // When
        SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                .withRequest(request)
                .withResponse(response)
                .withAuthnRequest(authnRequest)
                .withSsoResponse(ssoResponse)
                .withSsoToken(ssoToken)
                .withIdpEntityId(TEST_IDP_ENTITY_ID)
                .withHostedEntityId(TEST_HOSTED_ENTITY_ID)
                .withProfile(TEST_PROFILE)
                .withUserId(TEST_USER_ID)
                .withBinding(TEST_BINDING)
                .withFailureCode(TEST_FAILURE_CODE)
                .withFederation(true)
                .build();

        // Then - Verify all 14 expected keys are present
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap).containsKeys(
                IDP_ENTITY_ID, HOSTED_ENTITYID, PROFILE, USER_ID, BINDING,
                FAILURE_CODE, IS_FEDERATION, REQUEST, RESPONSE, AUTHN_REQUEST, SSO_RESPONSE,
                SESSION, SP_ADAPTER_SCRIPT_HELPER, AUTHN_REQUEST_HELPER, REQUEST_HELPER, RESPONSE_HELPER
        );
        assertThat(bindingsMap).hasSize(16);
    }

    private void assertIsMap(BindingsMap bindingsMap, String key, String description) {
        assertThat(bindingsMap.get(key))
                .as(description)
                .isNotNull()
                .isInstanceOf(Map.class);
    }
}
