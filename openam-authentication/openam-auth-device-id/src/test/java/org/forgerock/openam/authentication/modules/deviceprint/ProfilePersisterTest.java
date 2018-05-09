/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProfilePersisterTest {

    private ProfilePersister profilePersister;

    private DeviceIdDao devicePrintDao;

    private final String username = "username";
    private final String password = "password";

    @BeforeMethod
    public void setUp() {

        devicePrintDao = mock(DeviceIdDao.class);

        profilePersister = new ProfilePersister(2, username, password,
                devicePrintDao);
    }

    @Test
    public void shouldAddNewDevicePrintToProfiles() throws Exception {

        //Given
        Map<String, Object> devicePrintProfile = new HashMap<>();

        JsonValue profileOne = json(object(
                field("uuid", "UUID1"),
                field("lastSelectedDate", newDate().getTime() - 172800)));
        JsonValue profileTwo = json(object(
                field("uuid", "UUID2"),
                field("lastSelectedDate", newDate().getTime() - 86400)));

        List<JsonValue> profiles = new ArrayList<>();
        profiles.add(profileOne);
        profiles.add(profileTwo);
        given(devicePrintDao.getDeviceProfiles(username, password)).willReturn(profiles);

        //When
        profilePersister.saveDevicePrint(devicePrintProfile);

        //Then
        ArgumentCaptor<List> profilesCaptor = ArgumentCaptor.forClass(List.class);
        verify(devicePrintDao).saveDeviceProfiles(eq(username), eq(password), profilesCaptor.capture());
        List<JsonValue> savedProfiles = profilesCaptor.getValue();
        assertThat(savedProfiles).hasSize(2);
        assertThat(savedProfiles.get(0)).isEqualTo(profileTwo);
        assertThat(savedProfiles.get(1).get("uuid").asString()).isNotEmpty();
        assertThat(savedProfiles.get(1).get("selectionCounter").asInteger()).isEqualTo(1);
        assertThat(savedProfiles.get(1).get("lastSelectedDate").asLong()).isGreaterThan(0);
        assertThat(savedProfiles.get(1).get("devicePrint").asMap()).isNotNull();
    }

}