/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAO for getting and storing Device Print Profiles for a given user.
 *
 * @since 12.0.0
 */
public class DevicePrintDao {

    private static final String DEBUG_NAME = "amAuthDeviceIdSave";
    private static final Debug DEBUG = Debug.getInstance(DEBUG_NAME);

    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String LDAP_DEVICE_PRINT_ATTRIBUTE_NAME = "devicePrintProfiles";

    /**
     * Gets the Device Print Profiles for the specified user.
     *
     * @param amIdentity The user's identity.
     * @return A {@code List} of the user's device print profiles.
     * @throws IdRepoException If there is a problem getting the device print profiles from LDAP.
     * @throws SSOException If there is a problem getting the device print profiles from LDAP.
     * @throws IOException If there is a problem parsing the device print profiles.
     */
    List<Map<String, Object>> getProfiles(AMIdentityWrapper amIdentity) throws IdRepoException, SSOException,
            IOException {

        Set<String> set = (Set<String>) amIdentity.getAttribute(LDAP_DEVICE_PRINT_ATTRIBUTE_NAME);
        List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
        if (null != set) {
            for (String profile : set) {
                profiles.add(MAPPER.readValue(profile, Map.class));
            }
        }
        return profiles;
    }

    /**
     * Stores the given Device Print Profiles for the specified user.
     *
     * @param amIdentity The user's identity.
     * @param profiles The {@code List} of the user's device print profiles.
     */
    void saveProfiles(AMIdentityWrapper amIdentity, List<Map<String, Object>> profiles) {
        try {
            Set<String> vals = new HashSet<String>();
            for (Map<String, Object> profile : profiles) {
                StringWriter writer = new StringWriter();
                MAPPER.writeValue(writer, profile);
                vals.add(writer.toString());
            }

            Map<String, Set> profilesMap = new HashMap<String, Set>();
            profilesMap.put(LDAP_DEVICE_PRINT_ATTRIBUTE_NAME, vals);

            amIdentity.setAttributes(profilesMap);
            amIdentity.store();
            DEBUG.message("Profiles stored");
        } catch (Exception e) {
            DEBUG.error("Could not store profiles. " + e);
        }
    }
}
