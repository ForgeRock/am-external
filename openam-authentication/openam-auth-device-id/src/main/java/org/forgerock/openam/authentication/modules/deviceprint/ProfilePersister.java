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
 * Copyright 2014-2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.debug.Debug;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao;
import org.forgerock.openam.utils.StringUtils;

/**
 * Persists the user's device print profile in LDAP.
 *
 * @since 12.0.0
 */
class ProfilePersister {

    private static final String DEBUG_NAME = "amAuthDeviceIdSave";
    private static final Debug DEBUG = Debug.getInstance(DEBUG_NAME);

    private final DeviceIdDao devicesDao;

    private final int maxProfilesAllowed;
    private final String username;
    private final String realm;

    /**
     * Constructs a new ProfilePersister instance.
     *
     * @param maxProfilesAllowed The maximum device print profiles a user is allowed.
     * @param username The username.
     * @param realm The realm.
     * @param devicesDao An instance of the DeviceIdDao.
     */
    ProfilePersister(int maxProfilesAllowed, String username, String realm, DeviceIdDao devicesDao) {
        this.maxProfilesAllowed = maxProfilesAllowed;
        this.username = username;
        this.realm = realm;
        this.devicesDao = devicesDao;
    }

    /**
     * Saves the device print as a new profile, with a generated name.
     *
     * @param devicePrint The device print.
     */
    void saveDevicePrint(Map<String, Object> devicePrint) {
        saveDevicePrint(null, devicePrint);
    }

    /**
     * Saves the device print as a new profile, with the specified name.
     *
     * @param devicePrint The device print.
     */
    void saveDevicePrint(String deviceName, Map<String, Object> devicePrint) {

        try {
            List<JsonValue> profiles = devicesDao.getDeviceProfiles(username, realm);

            while (profiles.size() >= maxProfilesAllowed) {
                DEBUG.message("Removing oldest user profile due to maximum profiles stored quantity");
                removeOldestProfile(profiles);
            }

            long lastSelectedDate = currentTimeMillis();
            Map<String, Object> profile = new HashMap<>();
            profile.put("uuid", UUID.randomUUID().toString());
            profile.put("name", StringUtils.isEmpty(deviceName) ?
                    generateProfileName(new Date(lastSelectedDate)) : deviceName);
            profile.put("selectionCounter", 1);
            profile.put("lastSelectedDate", lastSelectedDate);
            profile.put("devicePrint", devicePrint);

            profiles.add(JsonValue.json(profile));

            devicesDao.saveDeviceProfiles(username, realm, profiles);
        } catch (Exception e) {
            DEBUG.error("Cannot get User's Device Print Profiles attribute. " + e);
        }
    }

    /**
     * Generates a profiles name from the specified last selected date.
     *
     * @param lastSelectedDate The last selected date.
     * @return A generated profile name.
     */
    private String generateProfileName(Date lastSelectedDate) {
        return "Profile: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(lastSelectedDate);
    }

    /**
     * Removes the oldest profile.
     *
     * @param profiles The current stored profiles.
     */
    private void removeOldestProfile(List<JsonValue> profiles) {
        JsonValue oldestProfile = null;
        long oldestDate = currentTimeMillis();

        for (JsonValue profile : profiles) {
            long lastSelectedDate = profile.get("lastSelectedDate").asLong();
            if (lastSelectedDate < oldestDate) {
                oldestDate = lastSelectedDate;
                oldestProfile = profile;
            }
        }

        if (oldestProfile != null) {
            profiles.remove(oldestProfile);
        }
    }
}
