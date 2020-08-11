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
 * Copyright 2017-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.factories.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class ScriptedDecisionNodeTest extends GuiceTestCase {

    @Mock
    ScriptEvaluator scriptEvaluator;

    @Mock
    ScriptConfiguration scriptConfiguration;

    @Mock
    ScriptHttpClientFactory httpClientFactory;

    @Mock
    ScriptedDecisionNode.Config serviceConfig;

    @Mock
    ListMultimap<String, String> headers;

    @Mock
    ScriptIdentityRepository.Factory scriptIdentityRepositoryFactory;

    @RealmTestHelper.RealmHelper
    static Realm mockRealm;

    ScriptedDecisionNode node;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        given(scriptConfiguration.getName()).willReturn("mock-script-name");
        given(scriptConfiguration.getScript()).willReturn("mock-script-body");
        given(scriptConfiguration.getLanguage()).willReturn(SupportedScriptingLanguage.JAVASCRIPT);
        given(serviceConfig.script()).willReturn(scriptConfiguration);
        given(serviceConfig.outcomes()).willReturn(ImmutableList.of("a", "b"));
        node = new ScriptedDecisionNode(scriptEvaluator, serviceConfig, null, httpClientFactory,
                mockRealm, scriptIdentityRepositoryFactory);
    }

    @Test
    public void correctScriptIsEvaluated() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcome("a"));
        node.process(getContext());

        ArgumentCaptor<ScriptObject> scriptCaptor = ArgumentCaptor.forClass(ScriptObject.class);
        verify(scriptEvaluator).evaluateScript(scriptCaptor.capture(), any(Bindings.class));

        assertThat(scriptCaptor.getValue().getName()).isEqualTo("mock-script-name");
        assertThat(scriptCaptor.getValue().getScript()).isEqualTo("mock-script-body");
        assertThat(scriptCaptor.getValue().getLanguage()).isEqualTo(SupportedScriptingLanguage.JAVASCRIPT);
    }

    @Test
    public void scriptIsPassedState() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcomeAndOutput("a", "foo:bar"));
        given(serviceConfig.outputs()).willReturn(ImmutableList.of("foo"));
        JsonValue sharedState = json(object(field("foo", "bar")));
        JsonValue transientState = json(object(field("fizz", "buzz")));
        node.process(getContext(sharedState, transientState));

        ArgumentCaptor<Bindings> bindingCaptor = ArgumentCaptor.forClass(Bindings.class);
        verify(scriptEvaluator).evaluateScript(any(ScriptObject.class), bindingCaptor.capture());

        assertThat(json(bindingCaptor.getValue().get("sharedState")).diff(sharedState).size() == 0).isTrue();
        assertThat(json(bindingCaptor.getValue().get("transientState")).diff(transientState).size() == 1).isTrue();
    }

    @Test
    public void scriptProvidesDeclaredOutputs() throws Exception {
        given(serviceConfig.outputs()).willReturn(ImmutableList.of("out"));
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcomeAndOutput("a", "out:put"));
        JsonValue sharedState = json(object(field("foo", "bar")));
        JsonValue transientState = json(object(field("fizz", "buzz")));
        node.process(getContext(sharedState, transientState));

        ArgumentCaptor<Bindings> bindingCaptor = ArgumentCaptor.forClass(Bindings.class);
        verify(scriptEvaluator).evaluateScript(any(ScriptObject.class), bindingCaptor.capture());

        assertThat(json(bindingCaptor.getValue().get("sharedState")).isDefined("out")).isTrue();
    }

    @Test
    public void scriptIsPassedParameter() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
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

        ArgumentCaptor<Bindings> bindingCaptor = ArgumentCaptor.forClass(Bindings.class);
        verify(scriptEvaluator).evaluateScript(any(ScriptObject.class), bindingCaptor.capture());

        Assert.assertEquals(bindingCaptor.getValue().get("requestParameters"), request.parameters);
    }

    @Test
    public void whenScriptSetsOutcomeToConfiguredOutcomeItReturnsANodeResultWithTheOutcome() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcome("a"));
        Action result = node.process(getContext());

        assertThat(result.outcome).isEqualTo("a");
    }

    @Test
    public void whenScriptSetsAnAction() throws Exception {
        // Given
        Action action = Action.goTo("a").build();
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithAction(action));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result).isSameAs(action);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void whenScriptSetsAnActionWithInvalidOutcome() throws Exception {
        // Given
        Action action = Action.goTo("c").build();
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithAction(action));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result).isSameAs(action);
    }

    @Test
    public void whenScriptSetsAnActionAndOutcome() throws Exception {
        // Given
        Action action = Action.goTo("a").build();
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithActionAndOutcome("a", action));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result).isSameAs(action);
    }

    @Test
    public void whenActionIsNotCorrectType() throws Exception {
        // Given
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithActionAndOutcome("a", new Object()));

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result.outcome).isEqualTo("a");
    }

    @Test
    public void whenScriptSetsAuditEntryDetail() throws Exception {
        // Given
        Map<String, Object> extraAuditInfo = new HashMap<>();
        extraAuditInfo.put("key", "value");
        JsonValue auditEntryDetail = json(object(field("auditInfo", extraAuditInfo)));
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcomeAndAuditEntryDetail("a", extraAuditInfo));

        // When
        Action result = node.process(getContext());
        JsonValue auditResult = node.getAuditEntryDetail();

        // Then
        assertThat(auditResult.isEqualTo(auditEntryDetail)).isTrue();
        assertThat(result.outcome).isEqualTo("a");
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void whenScriptSetsOutcomeToValueNotConfiguredItThrowsException() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcome("c"));
        node.process(getContext());
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void whenScriptDoesNotSetOutcomeItThrowsException() throws Exception {
        node.process(getContext());
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void whenScriptSetsOutcomeToNonBooleanItThrowsNodeProcessException() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcome(1));
        node.process(getContext());
    }

    @Test
    public void whenScriptThrowsExceptionItThrowsNodeProcessException() throws Exception {
        Throwable scriptException = new ScriptException("problem");
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class))).willThrow(scriptException);

        assertThatThrownBy(() -> node.process(getContext())).isExactlyInstanceOf(NodeProcessException.class)
                .hasCause(scriptException);
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

    private static Answer<Object> answerWithOutcome(Object outcome) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            bindings.put("outcome", outcome);
            return null;
        };
    }

    private static Answer<Object> answerWithAction(Object action) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            bindings.put("action", action);
            return null;
        };
    }

    private static Answer<Object> answerWithActionAndOutcome(Object outcome, Object action) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            bindings.put("action", action);
            bindings.put("outcome", outcome);
            return null;
        };
    }

    private static Answer<Object> answerWithOutcomeAndOutput(Object outcome, String... outputs) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            for (String output : outputs) {
                String[] parts = output.split(":");
                ((Map<String, Object>) bindings.get("sharedState")).put(parts[0], parts[1]);
            }
            bindings.put("outcome", outcome);
            return null;
        };
    }

    private static Answer<Object> answerWithOutcomeAndAuditEntryDetail(Object outcome,
                                                                       Map<String, Object> auditEntryDetail) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            bindings.put("outcome", outcome);
            bindings.put("auditEntryDetail", auditEntryDetail);
            return null;
        };
    }
}
