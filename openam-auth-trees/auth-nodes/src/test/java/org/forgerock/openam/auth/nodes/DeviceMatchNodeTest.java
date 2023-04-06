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
 * Copyright 2020-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;


import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.security.auth.callback.Callback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.DeviceMatchNode.Config;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

public class DeviceMatchNodeTest {

    @Mock
    CoreWrapper coreWrapper;

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
    ScriptEvaluator scriptEvaluator;

    @Mock
    Script script;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException {
        initMocks(this);
        node = new DeviceMatchNode(deviceProfilesDao, __ -> scriptEvaluator, coreWrapper,
                mock(LegacyIdentityService.class), config, realm);

        given(realm.asPath()).willReturn("/");
        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");
        given(config.expiration()).willReturn(30);
    }

    @Test
    public void testProcessContextMatch()
            throws NodeProcessException, DevicePersistenceException {

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
    public void testProcessContextMatchWithVariant()
            throws NodeProcessException, DevicePersistenceException {

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
    public void testProcessContextNotMatch()
            throws NodeProcessException, DevicePersistenceException {

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
    public void testProcessContextNotFound()
            throws NodeProcessException, DevicePersistenceException {

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

    @Test(description = "Device exists but no metadata")
    public void testProcessContextNoMetadata()
            throws NodeProcessException, DevicePersistenceException {

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

    @Test(expectedExceptions = NodeProcessException.class)
    public void testProcessContextNoProfileInContext()
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
        node.process(getContext(sharedState, transientState, emptyList()));

    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void testProcessContextNoUserInContext()
            throws NodeProcessException {

        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier2");

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        // When
        node.process(getContext(sharedState, transientState, emptyList()));

    }

    @Test
    public void testProcessContextExpired()
            throws NodeProcessException, DevicePersistenceException {

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
    public void testUseScript() throws ScriptException, NodeProcessException {
        given(config.useScript()).willReturn(true);
        given(script.getName()).willReturn("mock-script-name");
        given(script.getScript()).willReturn("mock-script-body");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(script.getEvaluatorVersion()).willReturn(EvaluatorVersion.defaultVersion());
        given(config.script()).willReturn(script);


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

        JsonValue transientState = json(object());

        given(scriptEvaluator.evaluateScript(any(Script.class), any(Bindings.class), eq(realm)))
                .will(answerWithOutcome("true"));
        Action action = node.process(getContext(sharedState, transientState, emptyList()));

        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);
        ArgumentCaptor<Bindings> bindingCaptor = ArgumentCaptor.forClass(Bindings.class);
        verify(scriptEvaluator).evaluateScript(scriptCaptor.capture(), bindingCaptor.capture(), eq(realm));

        assertThat(scriptCaptor.getValue().getName()).isEqualTo("mock-script-name");
        assertThat(scriptCaptor.getValue().getScript()).isEqualTo("mock-script-body");
        assertThat(scriptCaptor.getValue().getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);

        assertThat(bindingCaptor.getValue().get("sharedState")).isNotNull();
        assertThat(bindingCaptor.getValue().get("transientState")).isNotNull();
        assertThat(bindingCaptor.getValue().get("deviceProfilesDao")).isNotNull();

        assertThat(action.outcome).isEqualTo("true");

    }

    private static Answer<Object> answerWithOutcome(Object outcome) {
        return invocationOnMock -> {
            Bindings bindings = invocationOnMock.getArgument(1);
            bindings.put("outcome", outcome);
            return null;
        };
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.of("bob"));
    }

}
