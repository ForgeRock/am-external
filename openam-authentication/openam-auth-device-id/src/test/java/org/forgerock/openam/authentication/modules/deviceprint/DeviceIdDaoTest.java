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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.Time.newDate;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.DeviceSerialisation;
import org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.deviceprint.DeviceIdService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;

public class DeviceIdDaoTest {
    private DeviceIdDao devicePrintDao;

    private final String username = "username";
    private final String realm = "realm";

    private AuthenticatorDeviceServiceFactory<DeviceIdService> mockServiceFactory;
    private DeviceIdService mockDeviceService;
    private DeviceSerialisation mockDeviceSerialization;
    private AMIdentity mockAMIdentity;

    @BeforeMethod
    public void theSetUp() { //you need this
        mockServiceFactory = mock(AuthenticatorDeviceServiceFactory.class);
        mockDeviceService = mock(DeviceIdService.class);
        mockDeviceSerialization = mock(DeviceSerialisation.class);
        mockAMIdentity = mock(AMIdentity.class);
        devicePrintDao = new DeviceIdDaoMockedGetIdentity(mockServiceFactory);
    }

    @Test
    public void shouldGetProfiles() throws Exception {

        //Given
        given(mockServiceFactory.create("realm")).willReturn(mockDeviceService);
        given(mockDeviceService.getDeviceSerialisationStrategy()).willReturn(mockDeviceSerialization);
        given(mockDeviceService.getConfigStorageAttributeName()).willReturn("attrName");
        given(mockDeviceSerialization.stringToDeviceProfile(anyString())).willReturn(json(object()));
        given(mockAMIdentity.getAttribute("attrName")).willReturn(Collections.singleton(""));

        //When
        List<JsonValue> profiles = devicePrintDao.getDeviceProfiles(username, realm);

        //Then
        assertThat(profiles.size()).isEqualTo(1);
    }

    @Test
    public void shouldSaveProfiles() throws Exception {

        //Given
        List<JsonValue> profiles = new ArrayList<>();

        JsonValue profileOne = json(object(
                field("uuid", "UUID1"),
                field("lastSelectedDate", newDate().getTime() - 172800)));
        JsonValue profileTwo = json(object(
                field("uuid", "UUID2"),
                field("lastSelectedDate", newDate().getTime() - 86400)));

        profiles.add(profileOne);
        profiles.add(profileTwo);

        given(mockServiceFactory.create("realm")).willReturn(mockDeviceService);
        given(mockDeviceService.getDeviceSerialisationStrategy()).willReturn(mockDeviceSerialization);
        given(mockDeviceService.getConfigStorageAttributeName()).willReturn("attrName");
        given(mockDeviceSerialization.stringToDeviceProfile(anyString())).willReturn(json(object()));
        given(mockAMIdentity.getAttribute("attrName")).willReturn(Collections.singleton(""));

        //When
        devicePrintDao.saveDeviceProfiles(username, realm, profiles);

        //Then
        verify(mockAMIdentity, times(1)).setAttributes(anyMap());
        verify(mockAMIdentity, times(1)).store();
    }


    @Test(expectedExceptions = DevicePersistenceException.class)
    public void shouldThrowDevicePersistenceExceptionIfNoUserNamer() throws Exception {

        //When
        devicePrintDao.getDeviceProfiles(null, realm);

        // Then - DevicePersistenceException exception;

    }

    @Test(expectedExceptions = DevicePersistenceException.class)
    public void shouldThrowDevicePersistenceExceptionIfNoUserNamerOverloadedMethod() throws Exception {
        // Test the overloaded method.
        //When
        devicePrintDao.getDeviceProfiles(null, realm, Collections.emptySet());

        // Then - DevicePersistenceException exception;

    }

    private class DeviceIdDaoMockedGetIdentity extends DeviceIdDao {

        /**
         * Construct a new DeviceIdDao.
         *
         * @param serviceFactory Factory used to retrieve the Trusted Device Service for this dao.
         */
        DeviceIdDaoMockedGetIdentity(AuthenticatorDeviceServiceFactory<DeviceIdService> serviceFactory) {
            super(serviceFactory);
        }

        @Override
        protected AMIdentity getIdentity(String userName, String realm, Set<String> userSearchAttributes) {
            return mockAMIdentity;
        }

        @Override
        protected Set<String> getUserAliasList(String realm) {
            return Collections.emptySet();
        }
    }
}
