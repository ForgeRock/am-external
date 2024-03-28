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
package org.forgerock.openam.auth.nodes.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.AUDIT_ENTRY_DETAIL;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.script.Bindings;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.script.ActionWrapper;
import org.forgerock.openam.auth.nodes.script.ScriptedCallbacksBuilder;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.test.rules.LoggerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;

import ch.qos.logback.classic.spi.ILoggingEvent;

@RunWith(MockitoJUnitRunner.class)
public class ScriptedNodeHelperTest {

    @Mock
    private ListMultimap<String, String> input;
    @Mock
    private Script script;
    @Mock
    private ScriptHttpClientFactory httpClientFactory;
    @Mock
    private ChfHttpClient chfHttpClient;
    @Mock
    private SessionService sessionService;
    @Mock
    private Session session;
    @Mock
    private Bindings bindings;

    @Rule
    public LoggerRule logger = new LoggerRule(ScriptedNodeHelper.class);

    @Test
    public void testConvertHeadersToModifiableObjects() {
        given(input.keySet()).willReturn(Set.of("key1", "Key2"));
        given(input.get("key1")).willReturn(List.of("key1-value1", "key1-value2"));
        given(input.get("Key2")).willReturn(List.of("key2-value1", "key2-value2"));
        Map<String, List<String>> expected = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        expected.put("key1", List.of("key1-value1", "key1-value2"));
        expected.put("Key2", List.of("key2-value1", "key2-value2"));

        Map<String, List<String>> actual = ScriptedNodeHelper.convertHeadersToModifiableObjects(input);
        assertThat(actual).containsExactlyEntriesOf(expected);
        actual.forEach((k, v) -> {
            assertThat(v.add("should be modifiable")).isTrue();
            assertThat(v.remove("should be modifiable")).isTrue();
        });
    }

    @Test
    public void testConvertHeadersToModifiableObjectsNullInput() {
        assertThatThrownBy(() -> ScriptedNodeHelper.convertHeadersToModifiableObjects(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testConvertHeadersToModifiableObjectsEmptyInput() {
        given(input.keySet()).willReturn(Set.of());
        Map<String, List<String>> actual = ScriptedNodeHelper.convertHeadersToModifiableObjects(input);
        assertThat(actual).isEmpty();
    }

    @Test
    public void testGetHttpClient() {
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(httpClientFactory.getScriptHttpClient(ScriptingLanguage.JAVASCRIPT)).willReturn(chfHttpClient);
        ChfHttpClient actual = ScriptedNodeHelper.getHttpClient(script, httpClientFactory);
        assertThat(actual).isEqualTo(chfHttpClient);
    }

    @Test
    public void testGetHttpClientNullScript() {
        assertThatThrownBy(() -> ScriptedNodeHelper.getHttpClient(null, httpClientFactory))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGetHttpClientNullClientFactory() {
        assertThatThrownBy(() -> ScriptedNodeHelper.getHttpClient(script, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGetSessionProperties() throws SessionException {
        Map<String, String> expected = Map.of("key1", "value1");
        String ssoTokenId = "SSO_TOKEN";
        given(sessionService.getSession(eq(new SessionID(ssoTokenId)))).willReturn(session);
        given(session.getProperties()).willReturn(expected);
        Map<String, String> actual = ScriptedNodeHelper.getSessionProperties(sessionService, ssoTokenId);
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    @Test
    public void testGetSessionPropertiesNullSessionService() {
        String ssoTokenId = "SSO_TOKEN";
        assertThatThrownBy(() -> ScriptedNodeHelper.getSessionProperties(null, ssoTokenId))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGetSessionPropertiesNullSSOTokenId() throws SessionException {
        Map<String, String> expected = Map.of("key1", "value1");
        given(sessionService.getSession(eq(new SessionID(null)))).willReturn(session);
        given(session.getProperties()).willReturn(expected);
        Map<String, String> actual = ScriptedNodeHelper.getSessionProperties(sessionService, null);
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    @Test
    public void testConvertParametersToModifiableObjects() {
        Map<String, List<String>> expected = Map.of("key1", List.of("value1", "value2"));
        Map<String, List<String>> actual = ScriptedNodeHelper.convertParametersToModifiableObjects(expected);
        assertThat(actual).containsExactlyEntriesOf(expected);
        actual.forEach((k, v) -> {
            assertThat(v.add("should be modifiable")).isTrue();
            assertThat(v.remove("should be modifiable")).isTrue();
        });
    }

    @Test
    public void testConvertParametersToModifiableObjectsNullInput() {
        assertThatThrownBy(() -> ScriptedNodeHelper.convertParametersToModifiableObjects(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGetAuditEntryDetailsMapEntry() throws NodeProcessException {
        Map<String, String> expected = Map.of("auditKey", "auditValue");
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(expected);
        JsonValue actual = ScriptedNodeHelper.getAuditEntryDetails(bindings);
        assertThat(actual.get("auditInfo").asMap()).containsExactlyEntriesOf(expected);
    }

    @Test
    public void testGetAuditEntryDetailsString() throws NodeProcessException {
        String expected = "auditEntry";
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(expected);
        JsonValue actual = ScriptedNodeHelper.getAuditEntryDetails(bindings);
        assertThat(actual.get("auditInfo").asString()).isEqualTo(expected);
    }

    @Test
    public void testGetAuditEntryDetailsNullInput() {
        assertThatThrownBy(() -> ScriptedNodeHelper.getAuditEntryDetails(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGetAuditEntryDetailsNull() throws NodeProcessException {
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(null);
        JsonValue actual = ScriptedNodeHelper.getAuditEntryDetails(bindings);
        assertThat(actual).isNull();
    }

    @Test
    public void testGetAuditEntryDetailsObject() {
        Object expected = new Object();
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(expected);
        assertThatThrownBy(() -> ScriptedNodeHelper.getAuditEntryDetails(bindings))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    public void testReturnActionIfTypeActionForVersion1Script() {

        Action actionResult = Action.goTo("true").build();
        Optional<Action> action = Optional.of(actionResult);

        Object result = ScriptedNodeHelper.getAction(actionResult, EvaluatorVersion.V1_0, null);

        assertThat(result).isEqualTo(action);
    }

    @Test
    public void testShouldReturnWarningIfResultNotTypeActionForV1() {

        ScriptedNodeHelper.getAction(null, EvaluatorVersion.V1_0, null);

        assertThat(logger.getWarnings(ILoggingEvent::getFormattedMessage))
                .contains("Found an action result from scripted node, but it was not an Action object");
    }

    @Test
    public void testShouldReturnActionIfTypeActionWrapperForVersion2Script() {

        ActionWrapper actionWrapper = new ActionWrapper().goTo("true");
        Optional<Action> action = Optional.of(actionWrapper.buildAction());

        Optional<Action> result = ScriptedNodeHelper.getAction(actionWrapper, EvaluatorVersion.V2_0, null);

        assertThat(result.get().outcome).isEqualTo(action.get().outcome);
    }

    @Test
    public void testShouldReturnActionWithCallbackForVersion2Script() {

        ScriptedCallbacksBuilder callbacksBuilder = new ScriptedCallbacksBuilder();
        callbacksBuilder.nameCallback("callback");
        ActionWrapper actionWrapper = new ActionWrapper();
        actionWrapper.setCallbacks(callbacksBuilder.getCallbacks());
        Optional<Action> action = Optional.of(actionWrapper.buildAction());

        Optional<Action> result = ScriptedNodeHelper.getAction(actionWrapper, EvaluatorVersion.V2_0, callbacksBuilder);

        assertThat(result.get().callbacks).isNotNull();
        assertThat(result.get().callbacks).isEqualTo(action.get().callbacks);
    }

    @Test
    public void testShouldReturnWarningIfResultNotTypeActionWrapperForV2() {

        ScriptedNodeHelper.getAction(null, EvaluatorVersion.V2_0, null);

        assertThat(logger.getWarnings(ILoggingEvent::getFormattedMessage))
                .contains("Found an action result from scripted node, but it was not an ActionWrapper object");
    }

    @Test
    public void testShouldReturnOutcomeIfStringAndInListOfAllowedOutcomes() throws NodeProcessException {

        String rawOutcome = "outcome";
        List<String> allowedOutcomes = List.of("outcome");

        String result = ScriptedNodeHelper.getOutcome(rawOutcome, allowedOutcomes);

        assertThat(result).isEqualTo(rawOutcome);
    }

    @Test
    public void testShouldThrowNodeProcessExceptionIfOutcomeNotInListOfAllowedOutcomes() throws NodeProcessException {

        String rawOutcome = "badOutcome";
        List<String> allowedOutcomes = List.of("allowedOutcome");

        assertThatThrownBy(() -> ScriptedNodeHelper.getOutcome(rawOutcome, allowedOutcomes))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Invalid outcome from script, 'badOutcome'");
        assertThat(logger.getWarnings(ILoggingEvent::getFormattedMessage))
                .contains("invalid script outcome badOutcome");
    }

    @Test
    public void testShouldThrowNodeProcessExceptionIfOutcomeNotString() {

        Integer rawOutcome = 1;
        List<String> allowedOutcomes = List.of("allowedOutcome");

        assertThatThrownBy(() -> ScriptedNodeHelper.getOutcome(rawOutcome, allowedOutcomes))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Script must set 'outcome' to a string.");
        assertThat(logger.getWarnings(ILoggingEvent::getFormattedMessage)).contains("script outcome error");
    }
}