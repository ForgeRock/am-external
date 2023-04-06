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
 * Copyright 2020-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.services.push.PushNotificationConstants.COMMUNICATION_ID;
import static org.forgerock.openam.services.push.PushNotificationConstants.COMMUNICATION_TYPE;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_ID;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_TYPE;
import static org.forgerock.openam.services.push.PushNotificationConstants.MECHANISM_UID;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorDeviceProfileHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.utils.RecoveryCodeGenerator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * Helper methods to deal with PushDeviceSettings.
 */
public class PushDeviceProfileHelper extends MultiFactorDeviceProfileHelper<PushDeviceSettings> {

    /**
     * The constructor for this helper.
     *
     * @param realm the realm of the requesting tree.
     * @param deviceJsonUtils conversion utility for DeviceSettings objects and Json.
     * @param userPushDeviceProfileManager managers user's device profiles.
     * @param recoveryCodeGenerator generates recovery codes.
     */
    @Inject
    public PushDeviceProfileHelper(@Assisted Realm realm,
                                   DeviceJsonUtils<PushDeviceSettings> deviceJsonUtils,
                                   UserPushDeviceProfileManager userPushDeviceProfileManager,
                                   RecoveryCodeGenerator recoveryCodeGenerator) {
        super(realm, deviceJsonUtils, userPushDeviceProfileManager, recoveryCodeGenerator);
    }

    /**
     * Creates and saves a fresh device profile.
     *
     * @param issuer The issuer.
     * @return the generated device profile.
     */
    public PushDeviceSettings createDeviceSettings(String issuer) {
        PushDeviceSettings pushDeviceSettings =
                ((UserPushDeviceProfileManager) deviceProfileManager).createDeviceProfile();
        pushDeviceSettings.setCommunicationType("placeholder");
        pushDeviceSettings.setCommunicationId("placeholder");
        pushDeviceSettings.setDeviceType("placeholder");
        pushDeviceSettings.setDeviceMechanismUID("placeholder");
        pushDeviceSettings.setDeviceId("placeholder");
        pushDeviceSettings.setIssuer(issuer);
        return pushDeviceSettings;
    }

    @Override
    public List<String> saveDeviceSettings(
            PushDeviceSettings pushDeviceSettings,
            JsonValue deviceResponse,
            AMIdentity identity,
            boolean generateRecoveryCodes) throws NodeProcessException {
        try {
            pushDeviceSettings.setCommunicationId(deviceResponse.get(COMMUNICATION_ID).asString());
            pushDeviceSettings.setDeviceMechanismUID(deviceResponse.get(MECHANISM_UID).asString());
            pushDeviceSettings.setCommunicationType(deviceResponse.get(COMMUNICATION_TYPE).asString());
            pushDeviceSettings.setDeviceType(deviceResponse.get(DEVICE_TYPE).asString());
            pushDeviceSettings.setDeviceId(deviceResponse.get(DEVICE_ID).asString());

            List<String> recoveryCodes = Collections.emptyList();
            if (generateRecoveryCodes) {
                recoveryCodes = generateRecoveryCodes();
                pushDeviceSettings.setRecoveryCodes(recoveryCodes);
            }

            deviceProfileManager.saveDeviceProfile(identity.getName(), realm.toString(), pushDeviceSettings);
            return recoveryCodes;
        } catch (DevicePersistenceException e) {
            throw new NodeProcessException("Unable to store device profile", e);
        }
    }

    /**
     * Create a number of random bytes to be used as challenge secret.
     *
     * @return the challenge as a Base64 encoded String.
     */
    public String createChallenge() {
        return ((UserPushDeviceProfileManager) deviceProfileManager).createRandomBytes();
    }
}

