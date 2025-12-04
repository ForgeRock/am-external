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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.script;

import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;

/**
 * This class provides a wrapper for the DeviceProfilesDao class.
 * It is used to interact with device profiles in a script-friendly way.
 */
@Supported(scriptingApi = true, javaApi = false)
public class DeviceProfilesDaoScriptWrapper {

    private final DeviceProfilesDao deviceProfilesDao;

    /**
     * Constructs a new instance of the DeviceProfilesDaoScriptWrapper.
     * @param deviceProfilesDao the DeviceProfilesDao instance to wrap
     */
    public DeviceProfilesDaoScriptWrapper(DeviceProfilesDao deviceProfilesDao) {
        this.deviceProfilesDao = deviceProfilesDao;
    }

    /**
     * Gets the device profiles for a user.
     *
     * @param username the username of the user
     * @param realm the realm of the user
     * @return a list of device profiles
     * @throws ScriptedDeviceException if an error occurs while getting the device profiles
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, Object>> getDeviceProfiles(String username, String realm)
            throws ScriptedDeviceException {
        try {
            return deviceProfilesDao.getDeviceProfiles(username, realm).stream()
                    .map(JsonValue::asMap)
                    .toList();
        } catch (DevicePersistenceException e) {
            throw new ScriptedDeviceException("Error getting device profiles", e);
        }
    }

    /**
     * Saves the device profiles for a user.
     *
     * @param username the username of the user
     * @param realm the realm of the user
     * @param deviceProfiles a list of device profiles
     * @throws ScriptedDeviceException if an error occurs while saving the device profiles
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void saveDeviceProfiles(String username, String realm, List<Map<String, Object>> deviceProfiles)
            throws ScriptedDeviceException {
        try {
            deviceProfilesDao.saveDeviceProfiles(username, realm, deviceProfiles.stream()
                    .map(JsonValue::json)
                    .toList());
        } catch (DevicePersistenceException e) {
            throw new ScriptedDeviceException("Error deleting device profile", e);
        }
    }

}
