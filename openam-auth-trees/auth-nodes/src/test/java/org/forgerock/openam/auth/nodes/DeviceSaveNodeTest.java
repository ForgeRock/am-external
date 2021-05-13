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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;


import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.DeviceProfileCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.JsonValueBuilder;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeviceSaveNodeTest {

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    AMIdentity amIdentity;

    @Mock
    Realm realm;

    @Mock
    DeviceSaveNode.Config config;

    @Mock
    DeviceProfilesDao deviceProfilesDao;

    @Mock
    IdentityUtils identityUtils;

    @InjectMocks
    DeviceSaveNode node;

    JsonValue location;
    JsonValue metadata;

    @BeforeMethod
    public void setup() throws Exception {
        node = null;
        initMocks(this);
        given(config.saveDeviceMetadata()).willReturn(true);
        given(config.saveDeviceLocation()).willReturn(true);
        given(config.maxSavedProfiles()).willReturn(5);
        given(realm.asPath()).willReturn("/");
        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");

        location = JsonValueBuilder.jsonValue().put("latitude", 123).put("longitude", 456)
                .build();

        metadata = JsonValueBuilder.jsonValue().put("model", "android").put("version", 20)
                .build();
    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "User does not exist or inactive")
    public void testProcessWithNoUsername() throws NodeProcessException {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        node.process(getContext(sharedState, transientState, emptyList(), Optional.empty()));
    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "User does not exist or inactive")
    public void testProcessWithUserNotActive()
            throws NodeProcessException, IdRepoException, SSOException {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        given(amIdentity.isActive()).willReturn(false);

        node.process(getContext(sharedState, transientState, emptyList()));
    }

    @Test(description = "No Identifier", expectedExceptions = NodeProcessException.class)
    public void testProcessWithoutIdentifier()
            throws NodeProcessException, IdRepoException, SSOException {
        JsonValue deviceProfile = JsonValueBuilder.jsonValue().build();
        deviceProfile.put("metadata", metadata);
        deviceProfile.put("location", location);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceProfile)));

        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, "");
        callback.setValue(deviceProfile.toString());

        // When
        node.process(getContext(sharedState, transientState, singletonList(callback)));

    }

    @Test(description = "Value not exists in TreeContext")
    public void testProcessWithNotExistsInTreeContext()
            throws NodeProcessException, IdRepoException, SSOException, DevicePersistenceException {
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

    @Test(description = "Update existing device attributes")
    public void testProcessWithCallbackWithUpdate()
            throws NodeProcessException, DevicePersistenceException {
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

    @Test(description = "Partially Update existing device attributes")
    public void testProcessPartiallyUpdateExistingDeviceAttributes()
            throws NodeProcessException, DevicePersistenceException {

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

    @Test(description = "Keep the existing profile and create new profile")
    public void testProcessWithCallbackWithCreate()
            throws NodeProcessException, IdRepoException, SSOException, DevicePersistenceException {
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

    @Test(description = "Test with Max profile")
    public void testProcessMaxProfile()
            throws NodeProcessException, IdRepoException, SSOException, DevicePersistenceException {
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

    @Test(description = "Save Failed", expectedExceptions = NodeProcessException.class)
    public void testProcessWithSaveFailed()
            throws NodeProcessException, IdRepoException, SSOException, DevicePersistenceException {
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
        node.process(getContext(sharedState, transientState, singletonList(callback)));

    }

    @Test(description = "Test with Alias from metadata")
    public void testProcessWithUpdateAliasFromMetadata()
            throws NodeProcessException, DevicePersistenceException {
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

    @Test(description = "Test with Alias from context variable")
    public void testProcessWithUpdateAliasFromContextVariable()
            throws NodeProcessException, DevicePersistenceException {
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