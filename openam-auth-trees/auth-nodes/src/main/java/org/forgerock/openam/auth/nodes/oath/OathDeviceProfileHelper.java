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

package org.forgerock.openam.auth.nodes.oath;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorDeviceProfileHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.UserOathDeviceProfileManager;
import org.forgerock.openam.utils.RecoveryCodeGenerator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * Helper methods to deal with OathDeviceSettings.
 */
public class OathDeviceProfileHelper extends MultiFactorDeviceProfileHelper<OathDeviceSettings> {

    private final UserOathDeviceProfileManager userOathDeviceProfileManager;

    /**
     * The constructor for this helper.
     *
     * @param realm the realm of the requesting tree.
     * @param deviceJsonUtils conversion utility for DeviceSettings objects and Json.
     * @param userOathDeviceProfileManager managers user's device profiles.
     * @param recoveryCodeGenerator generates recovery codes.
     */
    @Inject
    public OathDeviceProfileHelper(@Assisted Realm realm,
            DeviceJsonUtils<OathDeviceSettings> deviceJsonUtils,
            UserOathDeviceProfileManager userOathDeviceProfileManager,
            RecoveryCodeGenerator recoveryCodeGenerator) {
        super(realm, deviceJsonUtils, userOathDeviceProfileManager, recoveryCodeGenerator);
        this.userOathDeviceProfileManager = userOathDeviceProfileManager;
    }

    /**
     * Creates and saves a fresh device profile.
     *
     * @param minSharedSecretLength the minimum size of the shared secret.
     * @param addChecksum indicator to add checksum.
     * @param truncationOffset the truncation offset.
     * @return the generated device profile.
     */
    public OathDeviceSettings createDeviceSettings(int minSharedSecretLength,
            boolean addChecksum, int truncationOffset) {
        OathDeviceSettings oathDeviceSettings = ((UserOathDeviceProfileManager) deviceProfileManager)
                .createDeviceProfile(minSharedSecretLength);
        oathDeviceSettings.setChecksumDigit(addChecksum);
        oathDeviceSettings.setTruncationOffset(truncationOffset);
        return oathDeviceSettings;
    }

    @Override
    public List<String> saveDeviceSettings(OathDeviceSettings deviceSettings, JsonValue deviceResponse,
            AMIdentity identity, boolean generateRecoveryCodes) throws NodeProcessException {
        try {
            List<String> recoveryCodes = Collections.emptyList();
            if (generateRecoveryCodes) {
                recoveryCodes = generateRecoveryCodes();
                deviceSettings.setRecoveryCodes(recoveryCodes);
            }

            deviceProfileManager.saveDeviceProfile(identity.getName(), realm.toString(), deviceSettings);
            return recoveryCodes;
        } catch (NullPointerException e) {
            throw new NodeProcessException("Blank value for necessary data from device response", e);
        } catch (DevicePersistenceException e) {
            throw new NodeProcessException("Unable to store device profile", e);
        }
    }

    /**
     * Save the device's settings on the user's profile.
     *
     * @param realm the realm.
     * @param username the user name.
     * @param deviceSettings the device's settings.
     * @throws DevicePersistenceException if unable to save device profile .
     */
    public void saveDeviceSettings(String realm, String username, OathDeviceSettings deviceSettings)
            throws DevicePersistenceException {
        userOathDeviceProfileManager.saveDeviceProfile(username, realm, deviceSettings);
    }
}
