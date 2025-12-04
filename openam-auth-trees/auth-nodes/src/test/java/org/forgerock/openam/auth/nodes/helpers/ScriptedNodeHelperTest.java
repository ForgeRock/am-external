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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.AUDIT_ENTRY_DETAIL;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.script.Bindings;

import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.application.tree.OAuthScriptedBindingObjectAdapter;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.script.ActionWrapper;
import org.forgerock.openam.auth.nodes.script.ScriptedCallbacksBuilder;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mozilla.javascript.ConsString;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;

import ch.qos.logback.classic.spi.ILoggingEvent;

@ExtendWith(MockitoExtension.class)
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
    @Mock
    private SessionID sessionID;
    @Mock
    private CTSPersistentStore cts;
    @Mock
    private OAuthScriptedBindingObjectAdapter adapter;

    @RegisterExtension
    public LoggerExtension loggerExtension = new LoggerExtension(ScriptedNodeHelper.class);
    private ScriptedNodeHelper scriptedNodeHelper;

    @BeforeEach
    void setup() {
        scriptedNodeHelper = new ScriptedNodeHelper(httpClientFactory, () -> sessionService, cts, adapter);
    }

    @Test
    void testConvertHeadersToModifiableObjects() {
        given(input.keySet()).willReturn(Set.of("key1", "Key2"));
        given(input.get("key1")).willReturn(List.of("key1-value1", "key1-value2"));
        given(input.get("Key2")).willReturn(List.of("key2-value1", "key2-value2"));
        Map<String, List<String>> expected = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        expected.put("key1", List.of("key1-value1", "key1-value2"));
        expected.put("Key2", List.of("key2-value1", "key2-value2"));

        Map<String, List<String>> actual = scriptedNodeHelper.convertHeadersToModifiableObjects(input);
        assertThat(actual).containsExactlyEntriesOf(expected);
        actual.forEach((k, v) -> {
            assertThat(v.add("should be modifiable")).isTrue();
            assertThat(v.remove("should be modifiable")).isTrue();
        });
    }

    @Test
    void testConvertHeadersToModifiableObjectsNullInput() {
        assertThatThrownBy(() -> scriptedNodeHelper.convertHeadersToModifiableObjects(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testConvertHeadersToModifiableObjectsEmptyInput() {
        given(input.keySet()).willReturn(Set.of());
        Map<String, List<String>> actual = scriptedNodeHelper.convertHeadersToModifiableObjects(input);
        assertThat(actual).isEmpty();
    }

    @Test
    void testGetLegacyHttpClient() {
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(httpClientFactory.getScriptHttpClient(ScriptingLanguage.JAVASCRIPT)).willReturn(chfHttpClient);
        ChfHttpClient actual = scriptedNodeHelper.getLegacyHttpClient(script);
        assertThat(actual).isEqualTo(chfHttpClient);
    }

    @Test
    void testGetLegacyHttpClientNullScript() {
        assertThatThrownBy(() -> scriptedNodeHelper.getLegacyHttpClient(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetSessionProperties() throws SessionException {
        Map<String, String> expected = Map.of("key1", "value1");
        String ssoTokenId = "SSO_TOKEN";
        given(sessionService.asSessionID(ssoTokenId)).willReturn(sessionID);
        given(sessionService.getSession(sessionID)).willReturn(session);
        given(session.getProperties()).willReturn(expected);
        Map<String, String> actual = scriptedNodeHelper.getSessionProperties(ssoTokenId);
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    @Test
    void testGetSessionPropertiesNullSSOTokenId() {
        Map<String, String> actual = scriptedNodeHelper.getSessionProperties(null);
        assertThat(actual).isNull();
    }

    @Test
    void testConvertParametersToModifiableObjects() {
        Map<String, List<String>> expected = Map.of("key1", List.of("value1", "value2"));
        Map<String, List<String>> actual = scriptedNodeHelper.convertParametersToModifiableObjects(expected);
        assertThat(actual).containsExactlyEntriesOf(expected);
        actual.forEach((k, v) -> {
            assertThat(v.add("should be modifiable")).isTrue();
            assertThat(v.remove("should be modifiable")).isTrue();
        });
    }

    @Test
    void testConvertParametersToModifiableObjectsNullInput() {
        assertThatThrownBy(() -> scriptedNodeHelper.convertParametersToModifiableObjects(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetAuditEntryDetailsMapEntry() throws NodeProcessException {
        Map<String, String> expected = Map.of("auditKey", "auditValue");
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(expected);
        JsonValue actual = scriptedNodeHelper.getAuditEntryDetails(bindings);
        assertThat(actual.get("auditInfo").asMap()).containsExactlyEntriesOf(expected);
    }

    @Test
    void testGetAuditEntryDetailsString() throws NodeProcessException {
        String expected = "auditEntry";
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(expected);
        JsonValue actual = scriptedNodeHelper.getAuditEntryDetails(bindings);
        assertThat(actual.get("auditInfo").asString()).isEqualTo(expected);
    }

    @Test
    void testGetAuditEntryDetailsNullInput() {
        assertThatThrownBy(() -> scriptedNodeHelper.getAuditEntryDetails(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetAuditEntryDetailsNull() throws NodeProcessException {
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(null);
        JsonValue actual = scriptedNodeHelper.getAuditEntryDetails(bindings);
        assertThat(actual).isNull();
    }

    @Test
    void testGetAuditEntryDetailsObject() {
        Object expected = new Object();
        given(bindings.get(AUDIT_ENTRY_DETAIL)).willReturn(expected);
        assertThatThrownBy(() -> scriptedNodeHelper.getAuditEntryDetails(bindings))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void testReturnActionIfTypeActionForVersion1Script() {

        Action actionResult = Action.goTo("true").build();
        Optional<Action> action = Optional.of(actionResult);

        Object result = scriptedNodeHelper.getAction(actionResult, EvaluatorVersion.V1_0, null);

        assertThat(result).isEqualTo(action);
    }

    @Test
    void testShouldReturnWarningIfResultNotTypeActionForV1() {

        scriptedNodeHelper.getAction(null, EvaluatorVersion.V1_0, null);

        assertThat(loggerExtension.getWarnings(ILoggingEvent::getFormattedMessage))
                .contains("Found an action result from scripted node, but it was not an Action object");
    }

    @Test
    void testShouldReturnActionIfTypeActionWrapperForVersion2Script() {

        ActionWrapper actionWrapper = new ActionWrapper().goTo("true");
        Optional<Action> action = Optional.of(actionWrapper.buildAction());

        Optional<Action> result = scriptedNodeHelper.getAction(actionWrapper, EvaluatorVersion.V2_0, null);

        assertThat(result.get().outcome).isEqualTo(action.get().outcome);
    }

    @Test
    void testShouldReturnActionWithCallbackForVersion2Script() {

        ScriptedCallbacksBuilder callbacksBuilder = new ScriptedCallbacksBuilder();
        callbacksBuilder.nameCallback("callback");
        ActionWrapper actionWrapper = new ActionWrapper();
        actionWrapper.setCallbacks(callbacksBuilder.getCallbacks());
        Optional<Action> action = Optional.of(actionWrapper.buildAction());

        Optional<Action> result = scriptedNodeHelper.getAction(actionWrapper, EvaluatorVersion.V2_0, callbacksBuilder);

        assertThat(result.get().callbacks).isNotNull();
        assertThat(result.get().callbacks).isEqualTo(action.get().callbacks);
    }

    @Test
    void testShouldReturnWarningIfResultNotTypeActionWrapperForV2() {

        scriptedNodeHelper.getAction(null, EvaluatorVersion.V2_0, null);

        assertThat(loggerExtension.getWarnings(ILoggingEvent::getFormattedMessage))
                .contains("Found an action result from scripted node, but it was not an ActionWrapper object");
    }

    @Test
    void testShouldReturnOutcomeIfStringAndInListOfAllowedOutcomes() throws NodeProcessException {

        String rawOutcome = "outcome";
        List<String> allowedOutcomes = List.of("outcome");

        String result = scriptedNodeHelper.getOutcome(rawOutcome, allowedOutcomes);

        assertThat(result).isEqualTo(rawOutcome);
    }

    @Test
    void testShouldThrowNodeProcessExceptionIfOutcomeNotInListOfAllowedOutcomes() throws NodeProcessException {

        String rawOutcome = "badOutcome";
        List<String> allowedOutcomes = List.of("allowedOutcome");

        assertThatThrownBy(() -> scriptedNodeHelper.getOutcome(rawOutcome, allowedOutcomes))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Invalid outcome from script, 'badOutcome'");
        assertThat(loggerExtension.getWarnings(ILoggingEvent::getFormattedMessage))
                .contains("invalid script outcome badOutcome");
    }

    @Test
    void testShouldThrowNodeProcessExceptionIfOutcomeNotString() {

        Integer rawOutcome = 1;
        List<String> allowedOutcomes = List.of("allowedOutcome");

        assertThatThrownBy(() -> scriptedNodeHelper.getOutcome(rawOutcome, allowedOutcomes))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Script must set 'outcome' to a string.");
        assertThat(loggerExtension.getWarnings(ILoggingEvent::getFormattedMessage)).contains("script outcome error");
    }

    @Test
    void testConvertOutcomeToStringIfCharSequence() throws NodeProcessException {

        CharSequence outcome = new ConsString("tr", "ue");
        String result = scriptedNodeHelper.getOutcome(outcome, List.of("true"));

        assertThat(result).isEqualTo(outcome.toString());
    }
}
