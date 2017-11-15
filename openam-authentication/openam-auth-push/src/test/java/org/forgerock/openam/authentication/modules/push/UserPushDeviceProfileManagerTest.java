/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.push;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDevicesDao;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class UserPushDeviceProfileManagerTest {

    private static final String USER = "testUser";
    private static final String REALM = "/test";

    private UserPushDeviceProfileManager userPushDeviceProfileManager;
    private DeviceJsonUtils<PushDeviceSettings> deviceJsonUtils = new DeviceJsonUtils<>(PushDeviceSettings.class);

    private PushDevicesDao mockDevicesDao = mock(PushDevicesDao.class);
    private Debug mockDebug = mock(Debug.class);
    private SecureRandom mockSecureRandom = mock(SecureRandom.class);

    @BeforeTest
    public void theSetUp() { //you need this
        userPushDeviceProfileManager = new UserPushDeviceProfileManager(
                mockDevicesDao, mockDebug, mockSecureRandom, deviceJsonUtils);
    }

    @Test
    public void shouldCreateBasicProfile() {

        //given

        //when
        PushDeviceSettings profile = userPushDeviceProfileManager.createDeviceProfile();

        //then
        assertThat(profile.getCommunicationId()).isNull();
        assertThat(profile.getDeviceName()).isEqualTo(UserPushDeviceProfileManager.DEVICE_NAME);
        assertThat(profile.getSharedSecret()).isNotEmpty();
    }

    @Test
    public void shouldSaveProfile() throws IOException, AuthLoginException {
        // Given
        PushDeviceSettings deviceSettings = new PushDeviceSettings();
        deviceSettings.setSharedSecret("sekret");
        deviceSettings.setDeviceName("test device");

        JsonValue expectedJson = new DeviceJsonUtils<>(PushDeviceSettings.class).toJsonValue(deviceSettings);

        // When
        userPushDeviceProfileManager.saveDeviceProfile(USER, REALM, deviceSettings);

        // Then
        ArgumentCaptor<List> savedProfileList = ArgumentCaptor.forClass(List.class);
        verify(mockDevicesDao).saveDeviceProfiles(eq(USER), eq(REALM), savedProfileList.capture());
        assertThat(savedProfileList.getValue()).hasSize(1);
        assertThat(savedProfileList.getValue().get(0).toString()).isEqualTo(expectedJson.toString());
    }

    @Test
    public void correctNumRandomBytesAreProducedAsBase64() {

        //given
        int num = 10;
        String byteArray = userPushDeviceProfileManager.createRandomBytes(num);

        //when

        //then
        assertThat(Base64.decode(byteArray)).hasSize(num);
    }

}
