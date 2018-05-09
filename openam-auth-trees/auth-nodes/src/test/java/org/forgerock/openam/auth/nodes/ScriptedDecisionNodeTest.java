/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.factories.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ScriptedDecisionNodeTest extends GuiceTestCase {

    @Mock
    ScriptEvaluator scriptEvaluator;

    @Mock
    ScriptConfiguration scriptConfiguration;

    @Mock
    ScriptHttpClientFactory httpClientFactory;

    @Mock
    ScriptedDecisionNode.Config serviceConfig;

    ScriptedDecisionNode node;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        given(scriptConfiguration.getName()).willReturn("mock-script-name");
        given(scriptConfiguration.getScript()).willReturn("mock-script-body");
        given(scriptConfiguration.getLanguage()).willReturn(SupportedScriptingLanguage.JAVASCRIPT);
        given(serviceConfig.script()).willReturn(scriptConfiguration);
        given(serviceConfig.outcomes()).willReturn(ImmutableList.of("a", "b"));
        node = new ScriptedDecisionNode(scriptEvaluator, serviceConfig, null, httpClientFactory);
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
    public void scriptIsPassedSharedState() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcome("a"));
        JsonValue sharedState = json(object(field("foo", "bar")));
        node.process(getContext(sharedState));

        ArgumentCaptor<Bindings> bindingCaptor = ArgumentCaptor.forClass(Bindings.class);
        verify(scriptEvaluator).evaluateScript(any(ScriptObject.class), bindingCaptor.capture());

        assertThat(bindingCaptor.getValue().get("sharedState")).isSameAs(sharedState.getObject());
    }

    @Test
    public void whenScriptSetsOutcomeToConfiguredOutcomeItReturnsANodeResultWithTheOutcome() throws Exception {
        given(scriptEvaluator.evaluateScript(any(ScriptObject.class), any(Bindings.class)))
                .will(answerWithOutcome("a"));
        Action result = node.process(getContext());

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
        return getContext(json(object()));
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), emptyList());
    }

    private static Answer<Object> answerWithOutcome(final Object outcome) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            bindings.put("outcome", outcome);
            return null;
        };
    }
}
