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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.BINDING;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAILURE_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HTTP_CLIENT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ENTITY_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ID_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ID_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IS_FEDERATION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.LOGOUT_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.LOGOUT_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.OUT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.PROFILE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_SP_ADAPTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.assertj.core.data.MapEntry;
import org.forgerock.http.Client;
import org.forgerock.http.handler.HttpClientHandler;
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

import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.Response;

public class ScriptedSpAdapterTest {

    private static final UUID scriptId = UUID.randomUUID();
    private static final String exampleScript = "exampleScript";
    private static final String hostedEntityId = "mySp";
    private static final String idpEntityId = "myIdp";
    private static final String realm = "myRealm";
    private static final String profile = "myProfile";
    private static final int failureCode = 1;
    private static final String userId = "myUser";
    private static final String binding = "myBinding";

    @Mock
    ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;
    @Mock
    private SpAdapterScriptHelper spAdapterScriptHelper;
    @Mock
    private ScriptExecutor executor;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response);
    private final AuthnRequest authnRequest = mock(AuthnRequest.class);
    private final Response saml2Response = mock(Response.class);
    private final Object session = mock(Object.class);
    private final PrintWriter out = mock(PrintWriter.class);
    private final ManageNameIDRequest idRequest = mock(ManageNameIDRequest.class);
    private final ManageNameIDResponse idResponse = mock(ManageNameIDResponse.class);
    private final LogoutRequest logoutRequest = mock(LogoutRequest.class);
    private final LogoutResponse logoutResponse = mock(LogoutResponse.class);

    private final ArgumentCaptor<LegacyScriptBindings> bindingsCaptor = ArgumentCaptor.forClass(LegacyScriptBindings.class);
    private final ArgumentCaptor<Script> scriptConfigurationCaptor = ArgumentCaptor.forClass(Script.class);

    private final MapEntry<String, Object> HOSTED_ENTITY_ID_BINDING = entry(HOSTED_ENTITYID, hostedEntityId);
    private final MapEntry<String, Object> IDP_ENTITY_ID_BINDING = entry(IDP_ENTITY_ID, idpEntityId);
    private final MapEntry<String, Object> AUTHN_REQUEST_BINDING = entry(AUTHN_REQUEST, authnRequest);
    private final MapEntry<String, Object> SESSION_BINDING = entry(SESSION, session);
    private final MapEntry<String, Object> SAML2_RESPONSE_BINDING = entry(SAML2_RESPONSE, saml2Response);
    private final MapEntry<String, Object> PROFILE_BINDING = entry(PROFILE, profile);
    private final MapEntry<String, Object> OUT_BINDING = entry(OUT, out);
    private final MapEntry<String, Object> IS_FEDERATION_BINDING = entry(IS_FEDERATION, true);
    private final MapEntry<String, Object> FAILURE_CODE_BINDING = entry(FAILURE_CODE, 1);
    private final MapEntry<String, Object> USER_ID_BINDING = entry(USER_ID, userId);
    private final MapEntry<String, Object> ID_REQUEST_BINDING = entry(ID_REQUEST, idRequest);
    private final MapEntry<String, Object> ID_RESPONSE_BINDING = entry(ID_RESPONSE, idResponse);
    private final MapEntry<String, Object> BINDING_BINDING = entry(BINDING, binding);
    private final MapEntry<String, Object> LOGOUT_REQUEST_BINDING = entry(LOGOUT_REQUEST, logoutRequest);
    private final MapEntry<String, Object> LOGOUT_RESPONSE_BINDING = entry(LOGOUT_RESPONSE, logoutResponse);

    private MapEntry<String, Object> HTTP_CLIENT_BINDING;

    private static MockedConstruction<HttpServletRequestWrapper> ignoredHttpServletRequestWrapper;
    private static MockedConstruction<HttpServletResponseWrapper> ignoredHttpServletResponseWrapper;

    // Class under test
    private ScriptedSpAdapter scriptedSpAdapter;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        HttpClientHandler httpClientHandler = new HttpClientHandler();
        Client httpClient = new Client(httpClientHandler);
        HTTP_CLIENT_BINDING = entry(HTTP_CLIENT, httpClient);
        when(scriptEvaluatorFactory.create(any(LegacyScriptContext.class))).thenReturn(scriptEvaluator);
        scriptedSpAdapter = new ScriptedSpAdapter(scriptEvaluatorFactory, spAdapterScriptHelper, httpClient, executor
        );

        // Given
        when(executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT))
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
    void testPreSingleSignOnRequest() throws Exception {
        // When
        scriptedSpAdapter.preSingleSignOnRequest(hostedEntityId, idpEntityId, realm, request, response, authnRequest);

        // Then
        verifyScriptEvaluation("preSingleSignOnRequest", null, List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                AUTHN_REQUEST_BINDING,
                IDP_ENTITY_ID_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPreSingleSignOnProcess() throws Exception {
        // When
        scriptedSpAdapter.preSingleSignOnProcess(hostedEntityId, realm, request, response, authnRequest, saml2Response,
                profile);

        // Then
        verifyScriptEvaluation("preSingleSignOnProcess", null, List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                AUTHN_REQUEST_BINDING,
                SAML2_RESPONSE_BINDING,
                PROFILE_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPostSingleSignOnSuccess() throws Exception {
        // When
        scriptedSpAdapter.postSingleSignOnSuccess(hostedEntityId, realm, request, response, out, session, authnRequest,
                saml2Response, profile, true);

        // Then
        verifyScriptEvaluation("postSingleSignOnSuccess", Boolean.class,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                OUT_BINDING,
                SESSION_BINDING,
                AUTHN_REQUEST_BINDING,
                SAML2_RESPONSE_BINDING,
                PROFILE_BINDING,
                IS_FEDERATION_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPostSingleSignOnFailure() throws Exception {
        // When
        scriptedSpAdapter.postSingleSignOnFailure(hostedEntityId, realm, request, response, authnRequest, saml2Response,
                profile, failureCode);

        // Then
        verifyScriptEvaluation("postSingleSignOnFailure", Boolean.class,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                AUTHN_REQUEST_BINDING,
                SAML2_RESPONSE_BINDING,
                FAILURE_CODE_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPostNewNameIDSuccess() throws Exception {
        // When
        scriptedSpAdapter.postNewNameIDSuccess(hostedEntityId, realm, request, response, userId, idRequest, idResponse,
                binding);

        // Then
        verifyScriptEvaluation("postNewNameIDSuccess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                USER_ID_BINDING,
                ID_REQUEST_BINDING,
                ID_RESPONSE_BINDING,
                BINDING_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPostTerminateNameIDSuccess() throws Exception {
        // When
        scriptedSpAdapter.postTerminateNameIDSuccess(hostedEntityId, realm, request, response, userId, idRequest,
                idResponse, binding);

        // Then
        verifyScriptEvaluation("postTerminateNameIDSuccess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                USER_ID_BINDING,
                ID_REQUEST_BINDING,
                ID_RESPONSE_BINDING,
                BINDING_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPreSingleLogoutProcess() throws Exception {
        // When
        scriptedSpAdapter.preSingleLogoutProcess(hostedEntityId, realm, request, response, userId, logoutRequest,
                logoutResponse, binding);

        // Then
        verifyScriptEvaluation("preSingleLogoutProcess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                USER_ID_BINDING,
                LOGOUT_REQUEST_BINDING,
                LOGOUT_RESPONSE_BINDING,
                BINDING_BINDING,
                HTTP_CLIENT_BINDING);
    }

    @Test
    void testPostSingleLogoutSuccess() throws Exception {
        // When
        scriptedSpAdapter.postSingleLogoutSuccess(hostedEntityId, realm, request, response, userId, logoutRequest,
                logoutResponse, binding);

        // Then
        verifyScriptEvaluation("postSingleLogoutSuccess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                USER_ID_BINDING,
                LOGOUT_REQUEST_BINDING,
                LOGOUT_RESPONSE_BINDING,
                BINDING_BINDING,
                HTTP_CLIENT_BINDING);
    }

    private void verifyScriptEvaluation(String functionName, Class<?> returnType, List<String> expectedKeys,
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
        assertThat(script.getName()).isEqualTo(SP_ADAPTER_SCRIPT);

        assertThat(bindingsCaptor.getValue().legacyBindings().keySet()).containsAll(expectedKeys);
        assertThat(bindingsCaptor.getValue().legacyBindings()).contains(expectedBindings);
    }

    private Script scriptConfiguration(String id) throws Exception{
        return Script.builder()
                .setId(id)
                .setName(SP_ADAPTER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 SP Adapter Script")
                .setScript(exampleScript)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(SAML2_SP_ADAPTER).build();
    }
}
