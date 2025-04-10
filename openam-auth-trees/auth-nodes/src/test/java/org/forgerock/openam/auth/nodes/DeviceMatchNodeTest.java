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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;


import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME;
import static org.forgerock.openam.auth.nodes.script.DeviceMatchNodeScriptContext.DEVICE_MATCH_NODE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.security.auth.callback.Callback;

import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.application.tree.OAuthScriptedBindingObjectAdapter;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.DeviceMatchNode.Config;
import org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper;
import org.forgerock.openam.auth.nodes.script.DeviceMatchNodeBindings;
import org.forgerock.openam.auth.nodes.script.DeviceMatchNodeScriptContext;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.LegacyScriptContext;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

import ch.qos.logback.classic.spi.ILoggingEvent;

@ExtendWith(MockitoExtension.class)
public class DeviceMatchNodeTest {

    @RegisterExtension
    LoggerExtension loggerExtension = new LoggerExtension(DeviceMatchNode.class);

    @Mock
    AMIdentity amIdentity;

    @Mock
    Realm realm;

    @Mock
    Config config;

    @Mock
    DeviceProfilesDao deviceProfilesDao;

    DeviceMatchNode node;

    @Mock
    ScriptEvaluator<DeviceMatchNodeBindings> scriptEvaluator;

    @Mock
    ScriptEvaluatorFactory scriptEvaluatorFactory;

    @Mock
    Script script;
    @Mock
    DeviceMatchNodeScriptContext deviceMatchNodeScriptContext;
    @Mock
    ScriptedIdentityRepository scriptedIdentityRepository;
    @Mock
    private CTSPersistentStore cts;

    @Mock
    private OAuthScriptedBindingObjectAdapter adapter;

    @Mock
    private NodeUserIdentityProvider identityProvider;
    @Mock
    private ScriptHttpClientFactory httpClientFactory;
    @Mock
    private SessionService sessionService;
    private UUID nodeId;

    private static JsonValue getSharedState() {
        JsonValue metadata = JsonValueBuilder.jsonValue().build();
        metadata.put("platform", "android");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("metadata", metadata);
        existing.put("lastSelectedDate", System.currentTimeMillis());

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier");
        deviceProfile.put("metadata", metadata);

        //Set the Share state
        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));
        return sharedState;
    }

    @BeforeEach
    void setup() throws IdRepoException, SSOException {
        nodeId = UUID.randomUUID();
        given(scriptEvaluatorFactory.create(any(LegacyScriptContext.class), eq(deviceMatchNodeScriptContext)))
                .willReturn(scriptEvaluator);
        var authScriptUtilities = new ScriptedNodeHelper(httpClientFactory, () -> sessionService, cts, adapter);
        node = new DeviceMatchNode(deviceProfilesDao, scriptEvaluatorFactory, deviceMatchNodeScriptContext,
                    identityProvider, config, realm, scriptedIdentityRepository, nodeId, authScriptUtilities);

    }

    private void commonStubbings() throws IdRepoException, SSOException {
        given(realm.asPath()).willReturn("/");
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
    }

    @Test
    void testProcessContextMatch()
            throws Exception {

        commonStubbings();
        given(amIdentity.getName()).willReturn("bob");
        given(config.expiration()).willReturn(30);
        JsonValue metadata = JsonValueBuilder.jsonValue().build();
        metadata.put("platform", "android");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("metadata", metadata);
        existing.put("lastSelectedDate", System.currentTimeMillis());

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier");
        deviceProfile.put("metadata", metadata);

        //Set the Share state
        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    void testProcessContextMatchWithVariant() throws Exception {

        commonStubbings();
        given(amIdentity.getName()).willReturn("bob");
        given(config.expiration()).willReturn(30);
        given(config.acceptableVariance()).willReturn(1);

        JsonValue metadata = JsonValueBuilder.jsonValue().build();
        metadata.put("platform", "android");
        metadata.put("version", "9");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("metadata", metadata);
        existing.put("lastSelectedDate", System.currentTimeMillis());

        JsonValue capturedMetadata = JsonValueBuilder.jsonValue().build();
        capturedMetadata.put("platform", "android");
        capturedMetadata.put("version", "10");

        JsonValue captured = JsonValueBuilder.jsonValue().build();
        captured.put("identifier", "testIdentifier");
        captured.put("metadata", capturedMetadata);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, captured)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    void testProcessContextNotMatch() throws Exception {

        commonStubbings();
        given(amIdentity.getName()).willReturn("bob");
        given(config.expiration()).willReturn(30);
        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("platform", "ios");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("metadata", "test");
        existing.put("lastSelectedDate", System.currentTimeMillis());

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier");
        deviceProfile.put("metadata", profile);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    void testProcessContextNotFound() throws Exception {

        commonStubbings();
        given(amIdentity.getName()).willReturn("bob");
        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("platform", "ios");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier1");
        existing.put("metadata", profile);

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier2");
        deviceProfile.put("metadata", profile);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("unknownDevice");
    }

    @Test
    void testProcessContextNoMetadata()
            throws Exception {

        commonStubbings();
        given(amIdentity.getName()).willReturn("bob");
        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("platform", "ios");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier1");

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier1");
        deviceProfile.put("metadata", profile);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("unknownDevice");
    }

    @Test
    void testProcessContextNoProfileInContext()
            throws NodeProcessException {

        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("platform", "ios");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier1");
        existing.put("metadata", profile);

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier2");

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));


        JsonValue transientState = json(object());

        // When
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                // Then
                .isInstanceOf(NodeProcessException.class);

    }

    @Test
    void testProcessContextNoUserInContext()
            throws NodeProcessException {

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier2");

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                // THEN
                .isInstanceOf(NodeProcessException.class);

    }

    @Test
    void testProcessContextExpired() throws Exception {

        commonStubbings();
        given(amIdentity.getName()).willReturn("bob");
        given(config.expiration()).willReturn(30);
        JsonValue metadata = JsonValueBuilder.jsonValue().build();
        metadata.put("platform", "android");

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("metadata", metadata);
        Instant lastSelectedDate = Instant.now().minus(40, ChronoUnit.DAYS);
        existing.put("lastSelectedDate", lastSelectedDate.toEpochMilli());

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier");
        deviceProfile.put("metadata", metadata);

        //Set the Share state
        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    void testUseScript() throws ScriptException, NodeProcessException {
        given(config.useScript()).willReturn(true);
        given(script.getName()).willReturn("mock-script-name");
        given(script.getScript()).willReturn("mock-script-body");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(script.getEvaluatorVersion()).willReturn(EvaluatorVersion.defaultVersion());
        given(config.script()).willReturn(script);


        JsonValue sharedState = getSharedState();

        JsonValue transientState = json(object());

        ScriptResult<Object> scriptResult = mock(ScriptResult.class);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("outcome", "true");
        when(scriptResult.getBindings()).thenReturn(bindings);
        given(scriptEvaluator.evaluateScript(any(Script.class), any(DeviceMatchNodeBindings.class), eq(realm)))
                .willReturn(scriptResult);
        Action action = node.process(getContext(sharedState, transientState, emptyList()));

        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);
        ArgumentCaptor<DeviceMatchNodeBindings> bindingCaptor = ArgumentCaptor.forClass(DeviceMatchNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(scriptCaptor.capture(), bindingCaptor.capture(), eq(realm));

        assertThat(scriptCaptor.getValue().getName()).isEqualTo("mock-script-name");
        assertThat(scriptCaptor.getValue().getScript()).isEqualTo("mock-script-body");
        assertThat(scriptCaptor.getValue().getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);

        assertThat(bindingCaptor.getValue().legacyBindings().get("sharedState")).isNotNull();
        assertThat(bindingCaptor.getValue().legacyBindings().get("transientState")).isNotNull();
        assertThat(bindingCaptor.getValue().legacyBindings().get("deviceProfilesDao")).isNotNull();

        assertThat(action.outcome).isEqualTo("true");
    }

    @Test
    void testUseNextGenScript() throws ScriptException, NodeProcessException {
        given(config.useScript()).willReturn(true);
        given(script.getName()).willReturn("mock-script-name");
        given(script.getScript()).willReturn("mock-script-body");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(script.getEvaluatorVersion()).willReturn(EvaluatorVersion.V2_0);
        given(script.getContext()).willReturn(deviceMatchNodeScriptContext);
        given(config.script()).willReturn(script);


        JsonValue sharedState = getSharedState();

        JsonValue transientState = json(object());

        ScriptResult<Object> scriptResult = mock(ScriptResult.class);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("outcome", "true");
        when(scriptResult.getBindings()).thenReturn(bindings);
        given(scriptEvaluator.evaluateScript(any(Script.class), any(DeviceMatchNodeBindings.class), eq(realm)))
                .willReturn(scriptResult);
        Action action = node.process(getContext(sharedState, transientState, emptyList()));

        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);
        ArgumentCaptor<DeviceMatchNodeBindings> bindingCaptor = ArgumentCaptor.forClass(DeviceMatchNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(scriptCaptor.capture(), bindingCaptor.capture(), eq(realm));

        assertThat(scriptCaptor.getValue().getName()).isEqualTo("mock-script-name");
        assertThat(scriptCaptor.getValue().getScript()).isEqualTo("mock-script-body");
        assertThat(scriptCaptor.getValue().getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);

        assertThat(bindingCaptor.getValue().nextGenBindings().get("sharedState")).isNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get("transientState")).isNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get("deviceProfilesDao")).isNotNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get("nodeState")).isNotNull();

        assertThat(action.outcome).isEqualTo("true");
        assertThat(loggerExtension.getWarnings(ILoggingEvent::getMessage)).isEmpty();
    }

    @Test
    void testInvalidScriptContext() throws Exception {
        given(deviceMatchNodeScriptContext.name()).willReturn(DEVICE_MATCH_NODE_NAME);
        given(config.useScript()).willReturn(true);
        given(script.getEvaluatorVersion()).willReturn(EvaluatorVersion.V2_0);
        given(script.getContext()).willReturn(mock(ScriptedDecisionNodeContext.class));

        String mockScriptId = UUID.randomUUID().toString();
        Script mockUpdatedScript = mock(Script.class);
        given(mockUpdatedScript.getId()).willReturn(mockScriptId);
        given(mockUpdatedScript.getName()).willReturn("mock-script-name");
        given(mockUpdatedScript.getScript()).willReturn("mock-script-body");
        given(mockUpdatedScript.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(mockUpdatedScript.getContext()).willReturn(deviceMatchNodeScriptContext);

        Script.Builder mockScriptBuilder = mock(Script.Builder.class);
        given(mockScriptBuilder.setContext(eq(deviceMatchNodeScriptContext))).willReturn(mockScriptBuilder);
        given(mockScriptBuilder.build()).willReturn(mockUpdatedScript);
        given(script.populatedBuilder()).willReturn(mockScriptBuilder);
        given(config.script()).willReturn(script);


        JsonValue sharedState = getSharedState();

        JsonValue transientState = json(object());

        ScriptResult<Object> scriptResult = mock(ScriptResult.class);
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("outcome", "true");
        when(scriptResult.getBindings()).thenReturn(bindings);
        given(scriptEvaluator.evaluateScript(any(Script.class), any(DeviceMatchNodeBindings.class), eq(realm)))
                .willReturn(scriptResult);
        Action action = node.process(getContext(sharedState, transientState, emptyList()));

        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);
        ArgumentCaptor<DeviceMatchNodeBindings> bindingCaptor = ArgumentCaptor.forClass(DeviceMatchNodeBindings.class);
        verify(scriptEvaluator).evaluateScript(scriptCaptor.capture(), bindingCaptor.capture(), eq(realm));

        assertThat(scriptCaptor.getValue().getName()).isEqualTo("mock-script-name");
        assertThat(scriptCaptor.getValue().getScript()).isEqualTo("mock-script-body");
        assertThat(scriptCaptor.getValue().getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);
        assertThat(scriptCaptor.getValue().getContext()).isEqualTo(deviceMatchNodeScriptContext);

        assertThat(bindingCaptor.getValue().nextGenBindings().get("sharedState")).isNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get("transientState")).isNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get("deviceProfilesDao")).isNotNull();
        assertThat(bindingCaptor.getValue().nextGenBindings().get("nodeState")).isNotNull();

        assertThat(action.outcome).isEqualTo("true");

        assertThat(loggerExtension.getWarnings(ILoggingEvent::getFormattedMessage)).containsExactly(
                "[ACTION REQUIRED] Invalid script configuration found. A Device Match Node with id '"
                        + nodeId + "' is configured to use a next-gen script with id '"
                        + mockScriptId + "' which has an invalid context. When a Device Match Node is configured to "
                        + "use a next-gen script, that script must use the 'DEVICE_MATCH_NODE' script context.");
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.of("bob"));
    }

}
