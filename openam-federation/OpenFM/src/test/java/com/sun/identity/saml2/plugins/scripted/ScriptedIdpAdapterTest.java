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
 * Copyright 2021-2026 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SSO_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.data.MapEntry;
import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.saml2.service.Saml2IdpAdapterContext;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAdapterScriptBindings;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class ScriptedIdpAdapterTest {

    private static final UUID SCRIPT_ID = UUID.randomUUID();
    private static final String EXAMPLE_SCRIPT = "exampleScript";
    private static final String HOSTED_ENTITY_ID = "myIdp";
    private static final String REALM = "myRealm";
    private static final String REQ_ID = "myReqId";
    private static final String RELAY_STATE = "myRelayState";
    private static final String FAULT_CODE = "myFaultCode";
    private static final String FAULT_DETAIL = "myFaultDetail";
    private final ArgumentCaptor<LegacyScriptBindings> bindingsCaptor = ArgumentCaptor.forClass(LegacyScriptBindings.class);
    private final ArgumentCaptor<Script> scriptConfigurationCaptor = ArgumentCaptor.forClass(Script.class);
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthnRequest authnRequest;
    @Mock
    private Object session;
    @Mock
    private Response saml2Response;
    private MapEntry<String, Object> hostedEntityIdBinding;
    private MapEntry<String, Object> authnRequestBinding;
    private MapEntry<String, Object> reqIdBinding;
    private MapEntry<String, Object> sessionBinding;
    private MapEntry<String, Object> relayStateBinding;
    private MapEntry<String, Object> saml2ResponseBinding;
    private MapEntry<String, Object> faultCodeBinding;
    private MapEntry<String, Object> faultDetailBinding;
    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ScriptEvaluator<LegacyScriptBindings> legacyScriptEvaluator;
    @Mock
    private ScriptEvaluator<SamlIdpAdapterScriptBindings> nextGenScriptEvaluator;
    @Mock
    private ScriptEvaluationHelper<LegacyScriptBindings> legacyHelper;
    @Mock
    private ScriptEvaluationHelper<SamlIdpAdapterScriptBindings> nextGenHelper;
    @Mock
    private ValidationHelper validationHelper;
    @Mock
    private IdpAdapterScriptHelper idpAdapterScriptHelper;
    @Mock
    private Client httpClient;
    @Mock
    private ScriptExecutor executor;
    @Mock
    private Saml2IdpAdapterContext saml2IdpAdapterContext;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private Realm realmObj;

    // Class under test
    private ScriptedIdpAdapter scriptedIdpAdapter;


    @BeforeEach
    void setup() throws Exception {
        when(scriptEvaluatorFactory.create(any(LegacyScriptContext.class))).thenReturn(legacyScriptEvaluator);
        when(legacyScriptEvaluator.getScriptEvaluationHelper()).thenReturn(legacyHelper);
        when(nextGenScriptEvaluator.getScriptEvaluationHelper()).thenReturn(nextGenHelper);
        when(scriptEvaluatorFactory.create(any(Saml2IdpAdapterContext.class))).thenReturn(nextGenScriptEvaluator);
        scriptedIdpAdapter = new ScriptedIdpAdapter(scriptEvaluatorFactory, saml2IdpAdapterContext,
                validationHelper, idpAdapterScriptHelper, httpClient, executor, realmLookup);
        lenient().when(realmLookup.lookup(anyString())).thenReturn(realmObj);

        hostedEntityIdBinding = entry(HOSTED_ENTITYID, HOSTED_ENTITY_ID);
        authnRequestBinding = entry(AUTHN_REQUEST, authnRequest);
        reqIdBinding = entry(SAML2Constants.ScriptParams.REQ_ID, REQ_ID);
        sessionBinding = entry(SESSION, session);
        relayStateBinding = entry(SAML2Constants.ScriptParams.RELAY_STATE, RELAY_STATE);
        saml2ResponseBinding = entry(SAML2_RESPONSE, saml2Response);
        faultCodeBinding = entry(SAML2Constants.ScriptParams.FAULT_CODE, FAULT_CODE);
        faultDetailBinding = entry(SAML2Constants.ScriptParams.FAULT_DETAIL, FAULT_DETAIL);

        // Given
        lenient().when(executor.getScriptConfiguration(REALM, HOSTED_ENTITY_ID, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfiguration(SCRIPT_ID.toString()));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void shouldEvaluatePreSingleSignOnFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSingleSignOn(HOSTED_ENTITY_ID, REALM, request, response, authnRequest, REQ_ID);

        // Then
        verifyScriptEvaluation("preSingleSignOn", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                authnRequestBinding,
                reqIdBinding);
    }

    @Test
    void shouldEvaluatePreAuthenticationFunction() throws Exception {
        // When
        scriptedIdpAdapter.preAuthentication(HOSTED_ENTITY_ID, REALM, request, response, authnRequest, session, REQ_ID, RELAY_STATE);

        // Then
        verifyScriptEvaluation("preAuthentication", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                authnRequestBinding,
                sessionBinding,
                reqIdBinding,
                relayStateBinding);
    }

    @Test
    void shouldEvaluatePreSendResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSendResponse(authnRequest, HOSTED_ENTITY_ID, REALM, request, response, session, REQ_ID, RELAY_STATE);

        // Then
        verifyScriptEvaluation("preSendResponse", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                authnRequestBinding,
                sessionBinding,
                reqIdBinding,
                relayStateBinding);
    }

    @Test
    void shouldEvaluatePreSignResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSignResponse(authnRequest, saml2Response, HOSTED_ENTITY_ID, REALM, request, session, RELAY_STATE);

        // Then
        verifyScriptEvaluation("preSignResponse", null, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST),
                hostedEntityIdBinding,
                saml2ResponseBinding,
                authnRequestBinding,
                sessionBinding,
                relayStateBinding);
    }

    @Test
    void shouldEvaluatePreSendFailureResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSendFailureResponse(HOSTED_ENTITY_ID, REALM, request, response, FAULT_CODE, FAULT_DETAIL);

        // Then
        verifyScriptEvaluation("preSendFailureResponse", null, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                hostedEntityIdBinding,
                faultCodeBinding,
                faultDetailBinding);
    }

    @Test
    void shouldEvaluatePreSingleSignOnFunctionV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM, HOSTED_ENTITY_ID, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedIdpAdapter.preSingleSignOn(HOSTED_ENTITY_ID, REALM, request, response, authnRequest, REQ_ID);

        // Then
        Map<String, Object> bindingsMap = verifyNextGenScriptEvaluation("preSingleSignOn", Boolean.class,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID,
                        SAML2Constants.ScriptParams.REQ_ID, REQ_ID));
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
    }

    @Test
    void shouldEvaluatePreAuthenticationFunctionV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM, HOSTED_ENTITY_ID, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedIdpAdapter.preAuthentication(HOSTED_ENTITY_ID, REALM, request, response, authnRequest, session, REQ_ID,
                RELAY_STATE);

        // Then
        Map<String, Object> bindingsMap = verifyNextGenScriptEvaluation("preAuthentication", Boolean.class,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID,
                        SAML2Constants.ScriptParams.REQ_ID, REQ_ID,
                        SAML2Constants.ScriptParams.RELAY_STATE, RELAY_STATE));
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
    }

    @Test
    void shouldEvaluatePreSendResponseFunctionV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM, HOSTED_ENTITY_ID, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedIdpAdapter.preSendResponse(authnRequest, HOSTED_ENTITY_ID, REALM, request, response, session, REQ_ID,
                RELAY_STATE);

        // Then
        Map<String, Object> bindingsMap = verifyNextGenScriptEvaluation("preSendResponse", Boolean.class,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID,
                        SAML2Constants.ScriptParams.REQ_ID, REQ_ID,
                        SAML2Constants.ScriptParams.RELAY_STATE, RELAY_STATE));
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
    }

    @Test
    void shouldEvaluatePreSignResponseFunctionV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM, HOSTED_ENTITY_ID, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedIdpAdapter.preSignResponse(authnRequest, saml2Response, HOSTED_ENTITY_ID, REALM, request, session,
                RELAY_STATE);

        // Then
        Map<String, Object> bindingsMap = verifyNextGenScriptEvaluation("preSignResponse", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID,
                        SAML2Constants.ScriptParams.RELAY_STATE, RELAY_STATE));
        assertThat(bindingsMap.get(SSO_RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isNull();
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isNull();
    }

    @Test
    void shouldEvaluatePreSendFailureResponseFunctionV2() throws Exception {
        // Given
        when(executor.getScriptConfiguration(REALM, HOSTED_ENTITY_ID, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfigurationV2(SCRIPT_ID.toString()));

        // When
        scriptedIdpAdapter.preSendFailureResponse(HOSTED_ENTITY_ID, REALM, request, response, FAULT_CODE, FAULT_DETAIL);

        // Then
        Map<String, Object> bindingsMap = verifyNextGenScriptEvaluation("preSendFailureResponse", null,
                Map.of(HOSTED_ENTITYID, HOSTED_ENTITY_ID,
                        SAML2Constants.ScriptParams.FAULT_CODE, FAULT_CODE,
                        SAML2Constants.ScriptParams.FAULT_DETAIL, FAULT_DETAIL));
        assertThat(bindingsMap.get(REQUEST)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(RESPONSE)).isInstanceOf(Map.class);
        assertThat(bindingsMap.get(REQUEST_HELPER)).isInstanceOf(HttpServletRequestHelper.class);
        assertThat(bindingsMap.get(RESPONSE_HELPER)).isInstanceOf(HttpServletResponseHelper.class);
    }

    @Test
    void shouldErrorForDeprecatedPreSendFailureResponseFunction() {
        assertThatThrownBy(() ->
                scriptedIdpAdapter.preSendFailureResponse(request, response, FAULT_CODE, FAULT_DETAIL))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Unsupported function for ScriptedIdpAdapter - use preSendFailureResponse" +
                        "(String, String, HttpServletRequest, HttpServletResponse, String, String)");
    }

    @SafeVarargs
    private void verifyScriptEvaluation(String functionName, Class<?> returnType, List<String> expectedKeys,
            Map.Entry<String, Object>... expectedBindings) throws Exception {
        if (returnType == null) {
            verify(legacyHelper, times(1)).evaluateFunction(
                    scriptConfigurationCaptor.capture(), bindingsCaptor.capture(), eq(functionName), eq(realmObj));
        } else {
            verify(legacyHelper, times(1)).evaluateFunction(scriptConfigurationCaptor.capture(),
                    bindingsCaptor.capture(), eq(returnType), eq(functionName), eq(realmObj));
        }
        final Script script = scriptConfigurationCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(EXAMPLE_SCRIPT);
        assertThat(script.getName()).isEqualTo(IDP_ADAPTER_SCRIPT);

        assertThat(bindingsCaptor.getValue().legacyBindings().keySet()).containsAll(expectedKeys);
        assertThat(bindingsCaptor.getValue().legacyBindings()).contains(expectedBindings);
    }

    private Map<String, Object> verifyNextGenScriptEvaluation(String functionName, Class<?> returnType,
            Map<String, Object> expectedBindings) throws Exception {

        ArgumentCaptor<SamlIdpAdapterScriptBindings> nextGenBindingsCaptor =
                ArgumentCaptor.forClass(SamlIdpAdapterScriptBindings.class);
        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);

        if (returnType == null) {
            verify(nextGenHelper, times(1)).evaluateFunction(
                    scriptCaptor.capture(), nextGenBindingsCaptor.capture(), eq(functionName), eq(realmObj));
        } else {
            verify(nextGenHelper, times(1)).evaluateFunction(scriptCaptor.capture(), nextGenBindingsCaptor.capture(),
                    eq(returnType), eq(functionName), eq(realmObj));
        }

        final Script script = scriptCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(EXAMPLE_SCRIPT);
        assertThat(script.getName()).isEqualTo(IDP_ADAPTER_SCRIPT);
        assertThat(script.getEvaluatorVersion()).isEqualTo(EvaluatorVersion.V2_0);

        SamlIdpAdapterScriptBindings bindings = nextGenBindingsCaptor.getValue();
        assertThat(bindings).isNotNull();

        Map<String, Object> bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.keySet()).contains(IDP_ADAPTER_SCRIPT_HELPER);
        assertThat(bindingsMap).containsAllEntriesOf(expectedBindings);
        return bindingsMap;
    }

    private Script scriptConfiguration(String id) throws Exception {
        return Script.builder()
                .setId(id)
                .setName(IDP_ADAPTER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 IDP Adapter Script")
                .setScript(EXAMPLE_SCRIPT)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V1_0)
                .setContext(SAML2_IDP_ADAPTER).build();
    }

    private Script scriptConfigurationV2(String id) throws Exception {
        return Script.builder()
                .setId(id)
                .setName(IDP_ADAPTER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 IDP Adapter Script")
                .setScript(EXAMPLE_SCRIPT)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setContext(SAML2_IDP_ADAPTER).build();
    }
}
