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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.OAUTH_APPLICATION;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.SAML_APPLICATION;
import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.OAUTH_OBJECT_KEY_PARAM;
import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.SAML_OBJECT_KEY_PARAM;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.application.tree.OAuthScriptedBindingObjectAdapter;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeBindings;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.secrets.IScriptedSecrets;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.LegacyScriptContext;
import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.saml2.common.SAML2FailoverUtils;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class ScriptedDecisionNodeTest extends GuiceTestCase {

    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;

    @Mock
    private ScriptEvaluator<ScriptedDecisionNodeBindings> scriptEvaluator;

    @Mock
    private Script script;

    @Mock
    private ScriptHttpClientFactory httpClientFactory;

    @Mock
    private ScriptedDecisionNode.Config serviceConfig;

    private final ListMultimap<String, String> headers = ImmutableListMultimap.of();

    @Mock
    private ScriptIdentityRepository.Factory scriptIdentityRepositoryFactory;

    @Mock
    private ScriptedIdentityRepository.Factory scriptedIdentityRepositoryFactory;

    @Mock
    private IScriptedSecrets secrets;

    @Mock
    static Realm mockRealm;

    @Mock
    private SAMLScriptedBindingObject samlScriptedBindingObject;

    @Mock
    private ScriptedDecisionNodeContext scriptedDecisionNodeContext;

    @Mock
    private SessionService sessionService;

    @Mock
    private CTSPersistentStore cts;

    @Mock
    private OAuthScriptedBindingObjectAdapter adapter;

    @Mock
    private OAuthScriptedBindingObject oAuthScriptedBindingObject;

    @Mock
    private Token token;

    ScriptedDecisionNode node;

    @BeforeEach
    void setup() throws Exception {
        given(script.getName()).willReturn("mock-script-name");
        given(script.getScript()).willReturn("mock-script-body");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(script.getEvaluatorVersion()).willReturn(EvaluatorVersion.defaultVersion());
        given(serviceConfig.script()).willReturn(script);
        given(serviceConfig.outcomes()).willReturn(ImmutableList.of("a", "b"));

        IScriptedSecrets.Factory secretsFactory = mock(IScriptedSecrets.Factory.class);
        given(secretsFactory.create(any(), any())).willReturn(secrets);
        given(scriptEvaluatorFactory.create(any(LegacyScriptContext.class), any(ScriptedDecisionNodeContext.class)))
                .willReturn(scriptEvaluator);
        var authScriptUtilities = new ScriptedNodeHelper(httpClientFactory, () -> sessionService, cts, adapter);
        node = new ScriptedDecisionNode(scriptEvaluatorFactory, serviceConfig,
                mockRealm, scriptIdentityRepositoryFactory,
                scriptedIdentityRepositoryFactory, secretsFactory, authScriptUtilities, scriptedDecisionNodeContext);
    }

    @Test
    void correctScriptIsEvaluated() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("a"));
        node.process(getContext());

        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);
        verify(scriptEvaluator).evaluateScript(scriptCaptor.capture(), any(ScriptedDecisionNodeBindings.class),
                eq(mockRealm));

        assertThat(scriptCaptor.getValue().getName()).isEqualTo("mock-script-name");
        assertThat(scriptCaptor.getValue().getScript()).isEqualTo("mock-script-body");
        assertThat(scriptCaptor.getValue().getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);
    }

    @Test
    void scriptIsPassedState() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcomeAndOutput("a", "foo:bar"));
        given(serviceConfig.outputs()).willReturn(ImmutableList.of("foo"));
        JsonValue sharedState = json(object(field("foo", "bar")));
        JsonValue transientState = json(object(field("fizz", "buzz")));
        node.process(getContext(sharedState, transientState));

        ArgumentCaptor<ScriptedDecisionNodeBindings> bindingCaptor =
                ArgumentCaptor.forClass(ScriptedDecisionNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingCaptor.capture(), eq(mockRealm));

        assertThat(json(bindingCaptor.getValue().legacyBindings().get("sharedState"))
                .diff(sharedState).size() == 0).isTrue();
        assertThat(json(bindingCaptor.getValue().legacyBindings().get("transientState"))
                .diff(transientState).size() == 1).isTrue();
    }

    @Test
    void scriptProvidesDeclaredOutputs() throws Exception {
        given(serviceConfig.outputs()).willReturn(ImmutableList.of("out"));
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcomeAndOutput("a", "out:put"));
        JsonValue sharedState = json(object(field("foo", "bar")));
        JsonValue transientState = json(object(field("fizz", "buzz")));
        node.process(getContext(sharedState, transientState));

        ArgumentCaptor<ScriptedDecisionNodeBindings> bindingCaptor =
                ArgumentCaptor.forClass(ScriptedDecisionNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingCaptor.capture(), eq(mockRealm));

        assertThat(json(bindingCaptor.getValue().legacyBindings().get("sharedState")).isDefined("out")).isTrue();
    }

    @Test
    void scriptIsPassedParameter() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("a"));
        JsonValue sharedState = json(object(field("foo", "bar")));
        JsonValue transientState = json(object(field("fizz", "buzz")));

        Map<String, String[]> parameters = new HashMap<>();
        String[] values = {"parameter"};
        parameters.put("dummy", values);

        ExternalRequestContext request = new ExternalRequestContext.Builder()
                .clientIp("127.0.0.1")
                .hostName("dummy.example.com")
                .locales(new PreferredLocales())
                .cookies(new HashMap<>())
                .headers(headers)
                .parameters(parameters)
                .build();
        node.process(getContext(sharedState, transientState, request));

        ArgumentCaptor<ScriptedDecisionNodeBindings> bindingCaptor =
                ArgumentCaptor.forClass(ScriptedDecisionNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingCaptor.capture(), eq(mockRealm));

        assertThat(bindingCaptor.getValue().legacyBindings().get("requestParameters")).isEqualTo(request.parameters);
    }

    @Test
    void whenScriptSetsOutcomeToConfiguredOutcomeItReturnsANodeResultWithTheOutcome() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("a"));
        Action result = node.process(getContext());

        assertThat(result.outcome).isEqualTo("a");
    }

    @Test
    void whenScriptSetsAnAction() throws Exception {
        // Given
        Action action = Action.goTo("a").build();
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithAction(action));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result).isSameAs(action);
    }

    @Test
    void whenScriptSetsAnActionWithInvalidOutcome() throws Exception {
        // Given
        Action action = Action.goTo("c").build();
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithAction(action));

        // When & Then
        assertThrows(NodeProcessException.class, () -> {
            Action result = node.process(getContext());
            assertThat(result).isSameAs(action);
        });
    }

    @Test
    void whenScriptSetsAnActionAndOutcome() throws Exception {
        // Given
        Action action = Action.goTo("a").build();
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithActionAndOutcome("a", action));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result).isSameAs(action);
    }

    @Test
    void whenActionIsNotCorrectType() throws Exception {
        // Given
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithActionAndOutcome("a", new Object()));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result.outcome).isEqualTo("a");
    }

    @Test
    void whenScriptSetsAuditEntryDetail() throws Exception {
        // Given
        Map<String, Object> extraAuditInfo = new HashMap<>();
        extraAuditInfo.put("key", "value");
        JsonValue auditEntryDetail = json(object(field("auditInfo", extraAuditInfo)));
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcomeAndAuditEntryDetail("a", extraAuditInfo));

        // When
        Action result = node.process(getContext());
        JsonValue auditResult = node.getAuditEntryDetail();

        // Then
        assertThat(auditResult.isEqualTo(auditEntryDetail)).isTrue();
        assertThat(result.outcome).isEqualTo("a");
    }

    @Test
    void whenScriptSetsOutcomeToValueNotConfiguredItThrowsException() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("c"));
        assertThatThrownBy(() -> node.process(getContext())).isExactlyInstanceOf(NodeProcessException.class);
    }

    @Test
    void whenScriptDoesNotSetOutcomeItThrowsException() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome(null));
        assertThatThrownBy(() -> node.process(getContext())).isExactlyInstanceOf(NodeProcessException.class);
    }

    @Test
    void whenScriptSetsOutcomeToNonBooleanItThrowsNodeProcessException() throws Exception {
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome(1));
        assertThatThrownBy(() -> node.process(getContext())).isExactlyInstanceOf(NodeProcessException.class);
    }

    @Test
    void whenScriptThrowsExceptionItThrowsNodeProcessException() throws Exception {
        Throwable scriptException = new ScriptException("problem");
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .willThrow(scriptException);

        assertThatThrownBy(() -> node.process(getContext())).isExactlyInstanceOf(NodeProcessException.class)
                .hasCause(scriptException);
    }

    @Test
    void whenInSAMLAppJourneyFlowSAMLApplicationBindingIsReturned() throws Exception {
        // Given & When
        var appBindings = Map.of(SAML_OBJECT_KEY_PARAM, new String[] { "samlApplicationValue" });

        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("a"));
        try (
                MockedStatic<SAML2FailoverUtils> mockedSaml2FailoverUtils = Mockito.mockStatic(SAML2FailoverUtils.class)
        ) {
            mockedSaml2FailoverUtils.when(() -> SAML2FailoverUtils.retrieveSAML2Token(any()))
                    .thenReturn(samlScriptedBindingObject);
            node.process(getContext(appBindings));
        }

        // Then
        var bindingCaptor = ArgumentCaptor.forClass(ScriptedDecisionNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingCaptor.capture(), eq(mockRealm));
        assertThat(bindingCaptor.getValue().nextGenBindings().get(SAML_APPLICATION)).isNotNull();
    }

    @Test
    void whenInOAuthAppJourneyFlowSAMLApplicationBindingIsReturned() throws Exception {
        // Given & When
        var appBindings = Map.of(OAUTH_OBJECT_KEY_PARAM, new String[] { "oauthApplicationValue" });

        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("a"));
        given(cts.read(any())).willReturn(token);
        given(adapter.fromToken(token)).willReturn(oAuthScriptedBindingObject);

        node.process(getContext(appBindings));

        // Then
        var bindingCaptor = ArgumentCaptor.forClass(ScriptedDecisionNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingCaptor.capture(), eq(mockRealm));
        assertThat(bindingCaptor.getValue().nextGenBindings().get(OAUTH_APPLICATION)).isNotNull();
    }

    @Test void whenNotInAppJounreyFlowThenApplicationBindingsAreNotAvailable() throws Exception {
        // Given & When
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ScriptedDecisionNodeBindings.class), eq(mockRealm)))
                .will(answerWithOutcome("a"));

        // When
        node.process(getContext());

        // Then
        var bindingCaptor = ArgumentCaptor.forClass(ScriptedDecisionNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingCaptor.capture(), eq(mockRealm));
        assertThat(bindingCaptor.getValue().nextGenBindings().get(SAML_APPLICATION)).isNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get(OAUTH_APPLICATION)).isNull();
    }

    private TreeContext getContext() {
        return getContext(json(object()), json(object()));
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return new TreeContext(sharedState, transientState, new Builder().build(), emptyList(), Optional.empty());
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, ExternalRequestContext request) {
        return new TreeContext(sharedState, transientState, request, emptyList(), Optional.empty());
    }

    private TreeContext getContext(Map<String, String[]> appBindings) {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().parameters(appBindings).build(), emptyList(), Optional.empty());
    }

    private static Answer<Object> answerWithOutcome(Object outcome) {
        return invocationOnMock -> {
            Bindings bindings = new SimpleBindings();
            bindings.put("outcome", outcome);
            ScriptResult scriptResult = mock(ScriptResult.class);
            given(scriptResult.getBindings()).willReturn(bindings);
            return scriptResult;
        };
    }

    private static Answer<Object> answerWithAction(Object action) {
        return invocationOnMock -> {
            Bindings bindings = new SimpleBindings();
            bindings.put("action", action);
            ScriptResult scriptResult = mock(ScriptResult.class);
            given(scriptResult.getBindings()).willReturn(bindings);
            return scriptResult;
        };
    }

    private static Answer<Object> answerWithActionAndOutcome(Object outcome, Object action) {
        return invocationOnMock -> {
            Bindings bindings = new SimpleBindings();
            bindings.put("action", action);
            bindings.put("outcome", outcome);
            ScriptResult scriptResult = mock(ScriptResult.class);
            given(scriptResult.getBindings()).willReturn(bindings);
            return scriptResult;
        };
    }

    private static Answer<Object> answerWithOutcomeAndOutput(Object outcome, String... outputs) {
        return invocationOnMock -> {
            LegacyScriptBindings bindingsIn = invocationOnMock.getArgument(1);
            Bindings bindings = new SimpleBindings();
            bindings.put("outcome", outcome);
            bindings.put("sharedState", bindingsIn.legacyBindings().get("sharedState"));
            for (String output : outputs) {
                String[] parts = output.split(":");
                ((Map<String, Object>) bindings.get("sharedState")).put(parts[0], parts[1]);
            }
            ScriptResult scriptResult = mock(ScriptResult.class);
            given(scriptResult.getBindings()).willReturn(bindings);
            return scriptResult;
        };
    }

    private static Answer<Object> answerWithOutcomeAndAuditEntryDetail(Object outcome,
            Map<String, Object> auditEntryDetail) {
        return invocationOnMock -> {
            Bindings bindings = new SimpleBindings();
            bindings.put("outcome", outcome);
            bindings.put("auditEntryDetail", auditEntryDetail);
            ScriptResult scriptResult = mock(ScriptResult.class);
            given(scriptResult.getBindings()).willReturn(bindings);
            return scriptResult;
        };
    }
}
