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
 * Copyright 2023-2026 Ping Identity Corporation.
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
import static org.mockito.ArgumentMatchers.anyString;
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
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.saml2.service.Saml2SpAdapterContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.LegacyScriptContext;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpAdapterScriptBindings;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.Response;

public class ScriptedSpAdapterTest {

    private static final UUID SCRIPT_ID = UUID.randomUUID();
    private static final String EXAMPLE_SCRIPT = "exampleScript";
    private static final String HOSTED_ENTITY_ID_VALUE = "mySp";
    private static final String IDP_ENTITY_ID_VALUE = "myIdp";
    private static final String REALM_NAME = "myRealm";
    private static final String PROFILE_VALUE = "myProfile";
    private static final int FAILURE_CODE_VALUE = 1;
    private static final String USER_ID_VALUE = "myUser";
    private static final String BINDING_VALUE = "myBinding";

    @Mock
    ScriptEvaluatorFactory scriptEvaluatorFactory;

    @Mock
    private ScriptEvaluator<LegacyScriptBindings> legacyScriptEvaluator;

    @Mock
    private ScriptEvaluator<SamlSpAdapterScriptBindings> nextGenScriptEvaluator;

    @Mock
    private ScriptEvaluationHelper<LegacyScriptBindings> legacyHelper;

    @Mock
    private ScriptEvaluationHelper<SamlSpAdapterScriptBindings> nextGenHelper;

    @Mock
    private ScriptExecutor executor;

    @Mock
    private Saml2SpAdapterContext saml2SpAdapterContext;

    @Mock
    private RealmLookup realmLookup;

    @Mock
    private Realm realm;

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

    private final ArgumentCaptor<LegacyScriptBindings> bindingsCaptor = ArgumentCaptor.forClass(
            LegacyScriptBindings.class);
    private final ArgumentCaptor<Script> scriptConfigurationCaptor = ArgumentCaptor.forClass(Script.class);

    private final MapEntry<String, Object> hostedEntityIdBinding = entry(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE);
    private final MapEntry<String, Object> idpEntityIdBinding = entry(IDP_ENTITY_ID, IDP_ENTITY_ID_VALUE);
    private final MapEntry<String, Object> authnRequestBinding = entry(AUTHN_REQUEST, authnRequest);
    private final MapEntry<String, Object> sessionBinding = entry(SESSION, session);
    private final MapEntry<String, Object> saml2ResponseBinding = entry(SAML2_RESPONSE, saml2Response);
    private final MapEntry<String, Object> profileBinding = entry(PROFILE, PROFILE_VALUE);
    private final MapEntry<String, Object> outBinding = entry(OUT, out);
    private final MapEntry<String, Object> isFederationBinding = entry(IS_FEDERATION, true);
    private final MapEntry<String, Object> failureCodeBinding = entry(FAILURE_CODE, FAILURE_CODE_VALUE);
    private final MapEntry<String, Object> userIdBinding = entry(USER_ID, USER_ID_VALUE);
    private final MapEntry<String, Object> idRequestBinding = entry(ID_REQUEST, idRequest);
    private final MapEntry<String, Object> idResponseBinding = entry(ID_RESPONSE, idResponse);
    private final MapEntry<String, Object> bindingBinding = entry(BINDING, BINDING_VALUE);
    private final MapEntry<String, Object> logoutRequestBinding = entry(LOGOUT_REQUEST, logoutRequest);
    private final MapEntry<String, Object> logoutResponseBinding = entry(LOGOUT_RESPONSE, logoutResponse);

    private MapEntry<String, Object> httpClientBinding;

    private static MockedConstruction<HttpServletRequestWrapper> ignoredHttpServletRequestWrapper;
    private static MockedConstruction<HttpServletResponseWrapper> ignoredHttpServletResponseWrapper;

    // Class under test
    private ScriptedSpAdapter scriptedSpAdapter;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        HttpClientHandler httpClientHandler = new HttpClientHandler();
        Client httpClient = new Client(httpClientHandler);
        httpClientBinding = entry(HTTP_CLIENT, httpClient);
        when(scriptEvaluatorFactory.create(any(LegacyScriptContext.class))).thenReturn(legacyScriptEvaluator);
        when(legacyScriptEvaluator.getScriptEvaluationHelper()).thenReturn(legacyHelper);
        when(nextGenScriptEvaluator.getScriptEvaluationHelper()).thenReturn(nextGenHelper);

        when(scriptEvaluatorFactory.create(any(Saml2SpAdapterContext.class))).thenReturn(nextGenScriptEvaluator);

        scriptedSpAdapter = new ScriptedSpAdapter(scriptEvaluatorFactory, saml2SpAdapterContext, httpClient, executor,
                realmLookup);

        when(realmLookup.lookup(anyString())).thenReturn(realm);

        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfiguration(SCRIPT_ID.toString()));


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
        scriptedSpAdapter.preSingleSignOnRequest(HOSTED_ENTITY_ID_VALUE, IDP_ENTITY_ID_VALUE, REALM_NAME, request,
                response, authnRequest);

        // Then
        verifyScriptEvaluation("preSingleSignOnRequest", null, List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                authnRequestBinding,
                idpEntityIdBinding,
                httpClientBinding);
    }

    @Test
    void testPreSingleSignOnProcess() throws Exception {
        // When
        scriptedSpAdapter.preSingleSignOnProcess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, authnRequest,
                saml2Response,
                PROFILE_VALUE);

        // Then
        verifyScriptEvaluation("preSingleSignOnProcess", null, List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                authnRequestBinding,
                saml2ResponseBinding,
                profileBinding,
                httpClientBinding);
    }

    @Test
    void testPostSingleSignOnSuccess() throws Exception {
        // When
        scriptedSpAdapter.postSingleSignOnSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, out, session,
                authnRequest,
                saml2Response, PROFILE_VALUE, true);

        // Then
        verifyScriptEvaluation("postSingleSignOnSuccess", Boolean.class,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                outBinding,
                sessionBinding,
                authnRequestBinding,
                saml2ResponseBinding,
                profileBinding,
                isFederationBinding,
                httpClientBinding);
    }

    @Test
    void testPostSingleSignOnFailure() throws Exception {
        // When
        scriptedSpAdapter.postSingleSignOnFailure(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, authnRequest,
                saml2Response, PROFILE_VALUE, FAILURE_CODE_VALUE);

        // Then
        verifyScriptEvaluation("postSingleSignOnFailure", Boolean.class,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                authnRequestBinding,
                saml2ResponseBinding,
                failureCodeBinding,
                httpClientBinding);
    }

    @Test
    void testPostNewNameIDSuccess() throws Exception {
        // When
        scriptedSpAdapter.postNewNameIDSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, USER_ID_VALUE,
                idRequest, idResponse, BINDING_VALUE);

        // Then
        verifyScriptEvaluation("postNewNameIDSuccess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                userIdBinding,
                idRequestBinding,
                idResponseBinding,
                bindingBinding,
                httpClientBinding);
    }

    @Test
    void testPostTerminateNameIDSuccess() throws Exception {
        // When
        scriptedSpAdapter.postTerminateNameIDSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response,
                USER_ID_VALUE, idRequest, idResponse, BINDING_VALUE);

        // Then
        verifyScriptEvaluation("postTerminateNameIDSuccess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                userIdBinding,
                idRequestBinding,
                idResponseBinding,
                bindingBinding,
                httpClientBinding);
    }

    @Test
    void testPreSingleLogoutProcess() throws Exception {
        // When
        scriptedSpAdapter.preSingleLogoutProcess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, USER_ID_VALUE,
                logoutRequest, logoutResponse, BINDING_VALUE);

        // Then
        verifyScriptEvaluation("preSingleLogoutProcess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                userIdBinding,
                logoutRequestBinding,
                logoutResponseBinding,
                bindingBinding,
                httpClientBinding);
    }

    @Test
    void testPostSingleLogoutSuccess() throws Exception {
        // When
        scriptedSpAdapter.postSingleLogoutSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, USER_ID_VALUE,
                logoutRequest, logoutResponse, BINDING_VALUE);

        // Then
        verifyScriptEvaluation("postSingleLogoutSuccess", null,
                List.of(SP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                userIdBinding,
                logoutRequestBinding,
                logoutResponseBinding,
                bindingBinding,
                httpClientBinding);
    }

    @Test
    void testPreSingleSignOnRequestV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.preSingleSignOnRequest(HOSTED_ENTITY_ID_VALUE, IDP_ENTITY_ID_VALUE, REALM_NAME, request,
                response, authnRequest);

        // Then
        verifyNextGenScriptEvaluation("preSingleSignOnRequest", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        IDP_ENTITY_ID, IDP_ENTITY_ID_VALUE));
    }

    @Test
    void testPreSingleSignOnProcessV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.preSingleSignOnProcess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, authnRequest,
                saml2Response, PROFILE_VALUE);

        // Then
        verifyNextGenScriptEvaluation("preSingleSignOnProcess", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        PROFILE, PROFILE_VALUE));
    }

    @Test
    void testPostSingleSignOnSuccessV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.postSingleSignOnSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, out, session,
                authnRequest, saml2Response, PROFILE_VALUE, true);

        // Then
        verifyNextGenScriptEvaluation("postSingleSignOnSuccess", Boolean.class,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        PROFILE, PROFILE_VALUE,
                        IS_FEDERATION, true));
    }

    @Test
    void testPostSingleSignOnFailureV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.postSingleSignOnFailure(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, authnRequest,
                saml2Response, PROFILE_VALUE, FAILURE_CODE_VALUE);

        // Then
        verifyNextGenScriptEvaluation("postSingleSignOnFailure", Boolean.class,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        FAILURE_CODE, FAILURE_CODE_VALUE));
    }

    @Test
    void testPostNewNameIDSuccessV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.postNewNameIDSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, USER_ID_VALUE,
                idRequest, idResponse, BINDING_VALUE);

        // Then
        verifyNextGenScriptEvaluation("postNewNameIDSuccess", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        USER_ID, USER_ID_VALUE,
                        BINDING, BINDING_VALUE));
    }

    @Test
    void testPostTerminateNameIDSuccessV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.postTerminateNameIDSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response,
                USER_ID_VALUE, idRequest, idResponse, BINDING_VALUE);

        // Then
        verifyNextGenScriptEvaluation("postTerminateNameIDSuccess", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        USER_ID, USER_ID_VALUE,
                        BINDING, BINDING_VALUE));
    }

    @Test
    void testPreSingleLogoutProcessV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.preSingleLogoutProcess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, USER_ID_VALUE,
                logoutRequest, logoutResponse, BINDING_VALUE);

        // Then
        verifyNextGenScriptEvaluation("preSingleLogoutProcess", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        USER_ID, USER_ID_VALUE,
                        BINDING, BINDING_VALUE));
    }

    @Test
    void testPostSingleLogoutSuccessV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM_NAME, HOSTED_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedSpAdapter.postSingleLogoutSuccess(HOSTED_ENTITY_ID_VALUE, REALM_NAME, request, response, USER_ID_VALUE,
                logoutRequest, logoutResponse, BINDING_VALUE);

        // Then
        verifyNextGenScriptEvaluation("postSingleLogoutSuccess", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID_VALUE,
                        USER_ID, USER_ID_VALUE,
                        BINDING, BINDING_VALUE));
    }

    @SafeVarargs
    private void verifyScriptEvaluation(
            String functionName, Class<?> returnType, List<String> expectedKeys,
            Map.Entry<String, Object>... expectedBindings
    ) throws Exception {
        if (returnType == null) {
            verify(legacyHelper, times(1)).evaluateFunction(
                    scriptConfigurationCaptor.capture(), bindingsCaptor.capture(), eq(functionName), eq(realm));
        } else {
            verify(legacyHelper, times(1)).evaluateFunction(scriptConfigurationCaptor.capture(),
                    bindingsCaptor.capture(), eq(returnType), eq(functionName), eq(realm));
        }
        final Script script = scriptConfigurationCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(EXAMPLE_SCRIPT);
        assertThat(script.getName()).isEqualTo(SP_ADAPTER_SCRIPT);

        assertThat(bindingsCaptor.getValue().legacyBindings().keySet()).containsAll(expectedKeys);
        assertThat(bindingsCaptor.getValue().legacyBindings()).contains(expectedBindings);
    }

    private void verifyNextGenScriptEvaluation(String functionName, Class<?> returnType,
            Map<String, Object> expectedBindings) throws Exception {

        ArgumentCaptor<SamlSpAdapterScriptBindings> nextGenBindingsCaptor =
                ArgumentCaptor.forClass(SamlSpAdapterScriptBindings.class);
        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);

        if (returnType == null) {
            verify(nextGenHelper, times(1)).evaluateFunction(
                    scriptCaptor.capture(), nextGenBindingsCaptor.capture(), eq(functionName), eq(realm));
        } else {
            verify(nextGenHelper, times(1)).evaluateFunction(scriptCaptor.capture(),
                    nextGenBindingsCaptor.capture(), eq(returnType), eq(functionName), eq(realm));
        }

        final Script script = scriptCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(EXAMPLE_SCRIPT);
        assertThat(script.getName()).isEqualTo(SP_ADAPTER_SCRIPT);
        assertThat(script.getEvaluatorVersion())
                .isEqualTo(EvaluatorVersion.V2_0);

        // Verify bindings object is created
        SamlSpAdapterScriptBindings bindings = nextGenBindingsCaptor.getValue();
        assertThat(bindings).isNotNull();

        // Verify bindings content
        Map<String, Object> bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.keySet()).contains(SP_ADAPTER_SCRIPT_HELPER);
        assertThat(bindingsMap).containsAllEntriesOf(expectedBindings);
    }

    private Script scriptConfiguration(String id) throws Exception {
        return Script.builder()
                .setId(id)
                .setName(SP_ADAPTER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 SP Adapter Script")
                .setScript(EXAMPLE_SCRIPT)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(SAML2_SP_ADAPTER).build();
    }

    private Script scriptConfigurationV2(String id) throws Exception {
        return Script.builder()
                .setId(id)
                .setName(SP_ADAPTER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 SP Adapter Script V2")
                .setScript(EXAMPLE_SCRIPT)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setContext(SAML2_SP_ADAPTER).build();
    }
}
