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
 * Copyright 2014-2018 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Set<String> userSearchAttributes = Collections.emptySet();

    @BeforeMethod
    public void setUp() {

        devicePrintDao = mock(DeviceIdDao.class);

        profilePersister = new ProfilePersister(2, username, password, devicePrintDao,
                userSearchAttributes);
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
        given(devicePrintDao.getDeviceProfiles(username, password, userSearchAttributes)).willReturn(profiles);

        //When
        profilePersister.saveDevicePrint(devicePrintProfile);

        //Then
        ArgumentCaptor<List> profilesCaptor = ArgumentCaptor.forClass(List.class);
        verify(devicePrintDao).saveDeviceProfiles(eq(username), eq(password), profilesCaptor.capture(),
                eq(userSearchAttributes) );
        List<JsonValue> savedProfiles = profilesCaptor.getValue();
        assertThat(savedProfiles).hasSize(2);
        assertThat(savedProfiles.get(0)).isEqualTo(profileTwo);
        assertThat(savedProfiles.get(1).get("uuid").asString()).isNotEmpty();
        assertThat(savedProfiles.get(1).get("selectionCounter").asInteger()).isEqualTo(1);
        assertThat(savedProfiles.get(1).get("lastSelectedDate").asLong()).isGreaterThan(0);
        assertThat(savedProfiles.get(1).get("devicePrint").asMap()).isNotNull();
    }

}
