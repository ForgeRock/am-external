/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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

import com.sun.identity.authentication.spi.AuthLoginException;

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
    private static final String BUNDLE_NAME = "amAuthDeviceIdSave";
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
     * @throws AuthLoginException If the profile save fails.
     */
    void saveDevicePrint(Map<String, Object> devicePrint) throws AuthLoginException {
        saveDevicePrint(null, devicePrint);
    }

    /**
     * Saves the device print as a new profile, with the specified name.
     *
     * @param devicePrint The device print.
     * @throws AuthLoginException If the profile save fails.
     */
    void saveDevicePrint(String deviceName, Map<String, Object> devicePrint) throws AuthLoginException  {

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
            throw new AuthLoginException(BUNDLE_NAME, "deviceprofilesavefail", null, e);
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
