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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.DeviceProfileCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

@ExtendWith(MockitoExtension.class)
public class DeviceSaveNodeTest {


    @Mock
    AMIdentity amIdentity;

    @Mock
    Realm realm;

    @Mock
    DeviceSaveNode.Config config;

    @Mock
    DeviceProfilesDao deviceProfilesDao;

    @Mock
    NodeUserIdentityProvider identityProvider;

    @InjectMocks
    DeviceSaveNode node;

    JsonValue location;
    JsonValue metadata;

    @BeforeEach
    void setup() throws Exception {
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));

        location = JsonValueBuilder.jsonValue().put("latitude", 123).put("longitude", 456)
                .build();

        metadata = JsonValueBuilder.jsonValue().put("model", "android").put("version", 20)
                .build();
    }

    private void commonStubbings() {
        given(config.saveDeviceMetadata()).willReturn(true);
        given(config.saveDeviceLocation()).willReturn(true);
        given(config.maxSavedProfiles()).willReturn(5);
        given(realm.asPath()).willReturn("/");
        given(amIdentity.getName()).willReturn("bob");
    }

    @Test
    void testProcessWithNoUsername() {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.empty());
        // When
        assertThatThrownBy(
                () -> node.process(getContext(sharedState, transientState, emptyList(), Optional.empty())))
                // Then
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("User does not exist or inactive");
    }

    @Test
    void testProcessWithUserNotActive() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        given(amIdentity.isActive()).willReturn(false);

        // When
        assertThatThrownBy(
                () -> node.process(getContext(sharedState, transientState, emptyList())))
                // Then
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("User does not exist or inactive");
    }

    @Test
    void testProcessWithoutIdentifier() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("metadata", metadata);
        deviceProfile.put("location", location);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(deviceProfile.toString());

        // When
        assertThatThrownBy(
                () -> node.process(getContext(sharedState, transientState, singletonList(callback))))
                // Then
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Device Profile Collector Node to collect device attribute is required: identifier");

    }

    @Test
    void testProcessWithNotExistsInTreeContext() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier");
        deviceProfile.put("metadata", metadata);

        //Location does not exists in the sharedState, but the config has location
        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));
        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(deviceProfile.toString());

        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);

        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());

        // When
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(singletonList(deviceProfile));
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        assertThat(captor.getValue()).hasSize(1);
        deviceProfile.put("lastSelectedDate", captor.getValue().get(0).get("lastSelectedDate").asLong());
        assertThat(captor.getValue()).contains(deviceProfile);

    }

    @Test
    void testProcessWithCallbackWithUpdate() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("identifier", "testIdentifier");
        deviceProfile.put("metadata", metadata);
        deviceProfile.put("location", location);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(deviceProfile.toString());


        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("metadata", "dummy");
        existing.put("location", "dummy");

        // When
        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(singletonList(existing));
        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        assertThat(captor.getValue()).hasSize(1);
        deviceProfile.put("lastSelectedDate", captor.getValue().get(0).get("lastSelectedDate").asLong());
        assertThat(captor.getValue().get(0).toString()).isEqualTo(deviceProfile.toString());

    }

    @Test
    void testProcessPartiallyUpdateExistingDeviceAttributes() throws Exception {

        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue newAttributes = JsonValueBuilder.jsonValue().build();
        newAttributes.put("identifier", "testIdentifier");
        newAttributes.put("location", location);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, newAttributes)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(newAttributes.toString());

        // When
        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("profile", metadata);

        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(singletonList(existing));

        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());

        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        JsonValue expectedAttributes = JsonValueBuilder.jsonValue().build();
        expectedAttributes.put("identifier", "testIdentifier");
        expectedAttributes.put("profile", metadata);
        expectedAttributes.put("location", location);
        expectedAttributes.put("lastSelectedDate", captor.getValue().get(0).get("lastSelectedDate").asLong());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).isEqualTo(expectedAttributes)).isTrue();

    }

    @Test
    void testProcessWithCallbackWithCreate() throws Exception {

        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue collected = JsonValueBuilder.jsonValue().build();
        collected.put("identifier", "testIdentifier");
        collected.put("metadata", metadata);
        collected.put("location", location);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collected)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(collected.toString());

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier1");

        // When
        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(singletonList(existing));

        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());

        //Return the existing one with different identifier
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        assertThat(captor.getValue().get(0).isEqualTo(existing)).isTrue();
        collected.put("lastSelectedDate", captor.getValue().get(1).get("lastSelectedDate").asLong());
        assertThat(captor.getValue().get(1).isEqualTo(collected)).isTrue();

    }

    @Test
    void testProcessMaxProfile() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        given(config.maxSavedProfiles()).willReturn(1);
        JsonValue collected = JsonValueBuilder.jsonValue().build();
        collected.put("identifier", "testIdentifier");
        collected.put("metadata", metadata);
        collected.put("location", location);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collected)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(collected.toString());

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("lastSelectedDate", System.currentTimeMillis() - 1000);
        existing.put("identifier", "testIdentifier1");

        // When
        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(singletonList(existing));

        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());

        //Return the existing one with different identifier
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        assertThat(captor.getValue().size()).isEqualTo(1);
        assertThat(captor.getValue().get(0).get("identifier").asString()).isEqualTo("testIdentifier");

    }

    @Test
    void testProcessWithSaveFailed()
            throws IdRepoException, SSOException, DevicePersistenceException {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue deviceAttributes = JsonValueBuilder.jsonValue().build();
        deviceAttributes.put("identifier", "testIdentifier");
        deviceAttributes.put("profile", JsonValueBuilder.toJsonValue("{\"test\":\"value\"}"));
        deviceAttributes.put("location", JsonValueBuilder.toJsonValue("{\"test\":\"value\"}"));

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceAttributes)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(deviceAttributes.toString());

        // When
        given(amIdentity.isActive()).willReturn(true);
        doThrow(new DevicePersistenceException("")).when(deviceProfilesDao).saveDeviceProfiles(any(), any(), any());
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(emptyList());
        assertThatThrownBy(
                () -> node.process(getContext(sharedState, transientState, singletonList(callback))))
                // Then
                .isInstanceOf(NodeProcessException.class);

    }

    @Test
    void testProcessWithUpdateAliasFromMetadata() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue collected = JsonValueBuilder.jsonValue().build();

        JsonValue platform = JsonValueBuilder.jsonValue().put("deviceName", "My Device Name").build();
        metadata = JsonValueBuilder.jsonValue().put("platform", platform).build();

        collected.put("identifier", "testIdentifier");
        collected.put("metadata", metadata);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collected)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, false, "");
        callback.setValue(collected.toString());

        // When
        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(emptyList());

        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());

        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        assertThat(captor.getValue().get(0).get("alias").asString()).isEqualTo("My Device Name");

    }

    @Test
    void testProcessWithUpdateAliasFromContextVariable() throws Exception {
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        commonStubbings();
        JsonValue collected = JsonValueBuilder.jsonValue().build();

        JsonValue platform = JsonValueBuilder.jsonValue().put("deviceName", "My Device Name").build();
        metadata = JsonValueBuilder.jsonValue().put("platform", platform).build();

        collected.put("identifier", "testIdentifier");
        collected.put("metadata", metadata);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collected),
                field("myContextVariable", "My New Device Name")));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, false, "");
        callback.setValue(collected.toString());

        // When
        given(config.variableName()).willReturn("myContextVariable");
        ArgumentCaptor<List<JsonValue>> captor = ArgumentCaptor.forClass(List.class);
        when(deviceProfilesDao.getDeviceProfiles(any(), any())).thenReturn(emptyList());

        doNothing().when(deviceProfilesDao).saveDeviceProfiles(any(), any(), captor.capture());

        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        //Make sure set Attribute contains the profile from the Callback
        assertThat(captor.getValue().get(0).get("alias").asString()).isEqualTo("My New Device Name");

    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks, Optional<String> universalId) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, universalId);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.of("bob"));
    }

}
