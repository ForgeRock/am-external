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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.assertj.core.data.MapEntry;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.LegacyScriptContext;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

public class ScriptedIdpAdapterTest {

    private static final UUID scriptId = UUID.randomUUID();
    private static final String exampleScript = "exampleScript";
    private static final String hostedEntityId = "myIdp";
    private static final String realm = "myRealm";
    private static final String reqId = "myReqId";
    private static final String relayState = "myRelayState";
    private static final String faultCode = "myFaultCode";
    private static final String faultDetail = "myFaultDetail";

    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;
    @Mock
    private ValidationHelper validationHelper;
    @Mock
    private IdpAdapterScriptHelper idpAdapterScriptHelper;
    @Mock
    private ScriptExecutor executor;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response);
    private final AuthnRequest authnRequest = mock(AuthnRequest.class);
    private final Object session = mock(Object.class);
    private final Response saml2Response = mock(Response.class);

    private final MapEntry<String, Object> HOSTED_ENTITY_ID_BINDING = entry(HOSTED_ENTITYID, hostedEntityId);
    private final MapEntry<String, Object> AUTHN_REQUEST_BINDING = entry(AUTHN_REQUEST, authnRequest);
    private final MapEntry<String, Object> REQ_ID_BINDING = entry(REQ_ID, reqId);
    private final MapEntry<String, Object> SESSION_BINDING = entry(SESSION, session);
    private final MapEntry<String, Object> RELAY_STATE_BINDING = entry(RELAY_STATE, relayState);
    private final MapEntry<String, Object> SAML2_RESPONSE_BINDING = entry(SAML2_RESPONSE, saml2Response);
    private final MapEntry<String, Object> FAULT_CODE_BINDING = entry(FAULT_CODE, faultCode);
    private final MapEntry<String, Object> FAULT_DETAIL_BINDING = entry(FAULT_DETAIL, faultDetail);

    private final ArgumentCaptor<LegacyScriptBindings> bindingsCaptor = ArgumentCaptor.forClass(LegacyScriptBindings.class);
    private final ArgumentCaptor<Script> scriptConfigurationCaptor = ArgumentCaptor.forClass(Script.class);

    private MockedConstruction<HttpServletRequestWrapper> ignoredHttpServletRequestWrapper;
    private MockedConstruction<HttpServletResponseWrapper> ignoredHttpServletResponseWrapper;

    // Class under test
    private ScriptedIdpAdapter scriptedIdpAdapter;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        when(scriptEvaluatorFactory.create(any(LegacyScriptContext.class))).thenReturn(scriptEvaluator);
        scriptedIdpAdapter = new ScriptedIdpAdapter(scriptEvaluatorFactory, validationHelper, idpAdapterScriptHelper,
                executor);

        // Given
        when(executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfiguration(scriptId.toString()));

        ignoredHttpServletRequestWrapper = mockConstructionWithAnswer(
                HttpServletRequestWrapper.class, invocation -> wrappedRequest);

        ignoredHttpServletResponseWrapper = mockConstructionWithAnswer(
                HttpServletResponseWrapper.class, invocation -> wrappedResponse);

    }

    @AfterEach
    void tearDown() {
        ignoredHttpServletRequestWrapper.close();
        ignoredHttpServletResponseWrapper.close();
    }

    @Test
    void shouldEvaluatePreSingleSignOnFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSingleSignOn(hostedEntityId, realm, request, response, authnRequest, reqId);

        // Then
        verifyScriptEvaluation("preSingleSignOn", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                AUTHN_REQUEST_BINDING,
                REQ_ID_BINDING);
    }

    @Test
    void shouldEvaluatePreAuthenticationFunction() throws Exception {
        // When
        scriptedIdpAdapter.preAuthentication(hostedEntityId, realm, request, response, authnRequest, session, reqId, relayState);

        // Then
        verifyScriptEvaluation("preAuthentication", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                AUTHN_REQUEST_BINDING,
                SESSION_BINDING,
                REQ_ID_BINDING,
                RELAY_STATE_BINDING);
    }

    @Test
    void shouldEvaluatePreSendResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSendResponse(authnRequest, hostedEntityId, realm, request, response, session, reqId, relayState);

        // Then
        verifyScriptEvaluation("preSendResponse", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                AUTHN_REQUEST_BINDING,
                SESSION_BINDING,
                REQ_ID_BINDING,
                RELAY_STATE_BINDING);
    }

    @Test
    void shouldEvaluatePreSignResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSignResponse(authnRequest, saml2Response, hostedEntityId, realm, request, session, relayState);

        // Then
        verifyScriptEvaluation("preSignResponse", null, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST),
                HOSTED_ENTITY_ID_BINDING,
                SAML2_RESPONSE_BINDING,
                AUTHN_REQUEST_BINDING,
                SESSION_BINDING,
                RELAY_STATE_BINDING);
    }

    @Test
    void shouldEvaluatePreSendFailureResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSendFailureResponse(hostedEntityId, realm, request, response, faultCode, faultDetail);

        // Then
        verifyScriptEvaluation("preSendFailureResponse", null, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                FAULT_CODE_BINDING,
                FAULT_DETAIL_BINDING);
    }

    @Test
    void shouldErrorForDeprecatedPreSendFailureResponseFunction() {
        assertThatThrownBy(() ->
                scriptedIdpAdapter.preSendFailureResponse(request, response, faultCode, faultDetail))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Unsupported function for ScriptedIdpAdapter - use preSendFailureResponse" +
                        "(String, String, HttpServletRequest, HttpServletResponse, String, String)");
    }

    private void verifyScriptEvaluation(String functionName, Class returnType, List<String> expectedKeys,
            Map.Entry<String, Object>... expectedBindings) throws Exception {
        if (returnType == null) {
            verify(executor, times(1)).evaluateVoidScriptFunction(eq(scriptEvaluator),
                    scriptConfigurationCaptor.capture(), eq(realm), bindingsCaptor.capture(), eq(functionName));
        } else {
            verify(executor, times(1)).evaluateScriptFunction(eq(scriptEvaluator),
                    scriptConfigurationCaptor.capture(), eq(realm), bindingsCaptor.capture(), eq(functionName));
        }
        final Script script = scriptConfigurationCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(IDP_ADAPTER_SCRIPT);

        assertThat(bindingsCaptor.getValue().legacyBindings().keySet()).containsAll(expectedKeys);
        assertThat(bindingsCaptor.getValue().legacyBindings()).contains(expectedBindings);
    }

    private Script scriptConfiguration(String id) throws Exception{
        return Script.builder()
                .setId(id)
                .setName(IDP_ADAPTER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 IDP Adapter Script")
                .setScript(exampleScript)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(SAML2_IDP_ADAPTER).build();
    }
}
