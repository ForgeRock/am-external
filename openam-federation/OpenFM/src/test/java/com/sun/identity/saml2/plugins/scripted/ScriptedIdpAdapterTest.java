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
 * Copyright 2021-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.assertj.core.data.MapEntry;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

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
    private ScriptEvaluator scriptEvaluator;
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
    private final MapEntry<String, Object> REALM_BINDING = entry(REALM, realm);
    private final MapEntry<String, Object> AUTHN_REQUEST_BINDING = entry(AUTHN_REQUEST, authnRequest);
    private final MapEntry<String, Object> REQ_ID_BINDING = entry(REQ_ID, reqId);
    private final MapEntry<String, Object> SESSION_BINDING = entry(SESSION, session);
    private final MapEntry<String, Object> RELAY_STATE_BINDING = entry(RELAY_STATE, relayState);
    private final MapEntry<String, Object> SAML2_RESPONSE_BINDING = entry(SAML2_RESPONSE, saml2Response);
    private final MapEntry<String, Object> FAULT_CODE_BINDING = entry(FAULT_CODE, faultCode);
    private final MapEntry<String, Object> FAULT_DETAIL_BINDING = entry(FAULT_DETAIL, faultDetail);

    private final ArgumentCaptor<Bindings> bindingsCaptor = ArgumentCaptor.forClass(Bindings.class);
    private final ArgumentCaptor<Script> scriptConfigurationCaptor = ArgumentCaptor.forClass(Script.class);

    // Class under test
    private ScriptedIdpAdapter scriptedIdpAdapter;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        scriptedIdpAdapter = new ScriptedIdpAdapter(__ -> scriptEvaluator, validationHelper, idpAdapterScriptHelper,
                executor);

        // Given
        when(executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT))
                .thenReturn(scriptConfiguration(scriptId.toString()));

        whenNew(HttpServletRequestWrapper.class).withArguments(request).thenReturn(wrappedRequest);
        whenNew(HttpServletResponseWrapper.class).withArguments(response).thenReturn(wrappedResponse);
    }

    @Test
    public void shouldEvaluatePreSingleSignOnFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSingleSignOn(hostedEntityId, realm, request, response, authnRequest, reqId);

        // Then
        verifyScriptEvaluation("preSingleSignOn", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                REALM_BINDING,
                AUTHN_REQUEST_BINDING,
                REQ_ID_BINDING);
    }

    @Test
    public void shouldEvaluatePreAuthenticationFunction() throws Exception {
        // When
        scriptedIdpAdapter.preAuthentication(hostedEntityId, realm, request, response, authnRequest, session, reqId, relayState);

        // Then
        verifyScriptEvaluation("preAuthentication", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                REALM_BINDING,
                AUTHN_REQUEST_BINDING,
                SESSION_BINDING,
                REQ_ID_BINDING,
                RELAY_STATE_BINDING);
    }

    @Test
    public void shouldEvaluatePreSendResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSendResponse(authnRequest, hostedEntityId, realm, request, response, session, reqId, relayState);

        // Then
        verifyScriptEvaluation("preSendResponse", Boolean.class, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                REALM_BINDING,
                AUTHN_REQUEST_BINDING,
                SESSION_BINDING,
                REQ_ID_BINDING,
                RELAY_STATE_BINDING);
    }

    @Test
    public void shouldEvaluatePreSignResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSignResponse(authnRequest, saml2Response, hostedEntityId, realm, request, session, relayState);

        // Then
        verifyScriptEvaluation("preSignResponse", null, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST),
                HOSTED_ENTITY_ID_BINDING,
                REALM_BINDING,
                SAML2_RESPONSE_BINDING,
                AUTHN_REQUEST_BINDING,
                SESSION_BINDING,
                RELAY_STATE_BINDING);
    }

    @Test
    public void shouldEvaluatePreSendFailureResponseFunction() throws Exception {
        // When
        scriptedIdpAdapter.preSendFailureResponse(hostedEntityId, realm, request, response, faultCode, faultDetail);

        // Then
        verifyScriptEvaluation("preSendFailureResponse", null, List.of(IDP_ADAPTER_SCRIPT_HELPER, REQUEST, RESPONSE),
                HOSTED_ENTITY_ID_BINDING,
                REALM_BINDING,
                FAULT_CODE_BINDING,
                FAULT_DETAIL_BINDING);
    }

    @Test(expected = SAML2Exception.class)
    public void shouldErrorForDeprecatedPreSendFailureResponseFunction() throws Exception {
        scriptedIdpAdapter.preSendFailureResponse(request, response, faultCode, faultDetail);
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
        assertThat(bindingsCaptor.getValue().keySet()).containsAll(expectedKeys);
        assertThat(bindingsCaptor.getValue()).contains(expectedBindings);
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
