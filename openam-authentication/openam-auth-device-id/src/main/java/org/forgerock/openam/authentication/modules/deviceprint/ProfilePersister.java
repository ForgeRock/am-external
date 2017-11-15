/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists the user's device print profile in LDAP.
 *
 * @since 12.0.0
 */
public class ProfilePersister {

    private static final String DEBUG_NAME = "amAuthDeviceIdSave";
    private static final Debug DEBUG = Debug.getInstance(DEBUG_NAME);

    private final int maxProfilesAllowed;
    private final DevicePrintDao devicePrintDao;
    private final AMIdentityWrapper amIdentity;

    /**
     * Constructs a new ProfilePersister instance.
     *
     * @param maxProfilesAllowed The maximum device print profiles a user is allowed.
     * @param devicePrintDao An instance of the DevicePrintDao.
     * @param amIdentity The user's identity.
     */
    ProfilePersister(int maxProfilesAllowed, DevicePrintDao devicePrintDao, AMIdentityWrapper amIdentity) {
        this.maxProfilesAllowed = maxProfilesAllowed;
        this.devicePrintDao = devicePrintDao;
        this.amIdentity = amIdentity;
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
    void saveDevicePrint(String name, Map<String, Object> devicePrint) {

        try {
            List<Map<String, Object>> profiles = devicePrintDao.getProfiles(amIdentity);

            String uuid = UUID.randomUUID().toString();

            while (profiles.size() >= maxProfilesAllowed) {
                DEBUG.message("Removing oldest user profile due to maximum profiles stored quantity");
                removeOldestProfile(profiles);
            }

            long lastSelectedDate = currentTimeMillis();
            Map<String, Object> profile = new HashMap<String, Object>();
            profile.put("uuid", uuid);
            profile.put("name", (name == null || name.isEmpty()) ? generateProfileName(new Date(lastSelectedDate)) : name);
            profile.put("selectionCounter", 1);
            profile.put("lastSelectedDate", lastSelectedDate);
            profile.put("devicePrint", devicePrint);

            profiles.add(profile);

            devicePrintDao.saveProfiles(amIdentity, profiles);

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
    private void removeOldestProfile(List<Map<String, Object>> profiles) {
        Map<String, Object> oldestProfile = null;
        long oldestDate = currentTimeMillis();

        for (Map<String, Object> profile : profiles) {
            long lastSelectedDate = (Long)profile.get("lastSelectedDate");
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
