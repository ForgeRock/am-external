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
 * Copyright 2025 Ping Identity Corporation.
 */

package org.forgerock.openam.services.push.utils;

import static org.forgerock.openam.services.push.PushNotificationConstants.COMMUNICATION_ID;
import static org.forgerock.openam.services.push.PushNotificationConstants.COMMUNICATION_TYPE;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_ID;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_NAME;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_TYPE;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.PushNotificationServiceConfigHelperFactory;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.keys.VerificationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * This utility class registers the device (via communication with the push notification service) that is currently
 * talking to the server from the mobile app.
 */
public class PushDeviceUpdater {

    private static final Logger logger = LoggerFactory.getLogger(PushDeviceUpdater.class);

    private final PushNotificationServiceConfigHelperFactory configHelperFactory;
    private final UserPushDeviceProfileManager userPushDeviceProfileManager;

    private final PushResponseUpdater responseUpdater;

    /**
     * Generate a new {@link PushDeviceUpdater}, which will use the supplied realm to read the config to gather the
     * information necessary to communicate with the push notification delegate and update the device.
     *
     * @param configHelperFactory The factory to generate the config helper.
     * @param userPushDeviceProfileManager The manager to retrieve and save device profiles.
     * @param pushNotificationService The service to communicate with the push notification delegate.
     */
    @Inject
    public PushDeviceUpdater(PushNotificationServiceConfigHelperFactory configHelperFactory,
                             UserPushDeviceProfileManager userPushDeviceProfileManager,
                             PushNotificationService pushNotificationService) {
        this.configHelperFactory = configHelperFactory;
        this.userPushDeviceProfileManager = userPushDeviceProfileManager;
        this.responseUpdater = new PushResponseUpdater(pushNotificationService);
    }

    /**
     * Validates that the response to a message is appropriate for the
     * user from whom it claims to be sent, by validating that the JWT sent in is
     * signed by the appropriate user (their shared secret is retrieved from the user store).
     *
     * @param content The content of the response message from the push device.
     * @param verificationKey Used to verify JWT messages and content.
     * @param location JsonPointer used to locate the jwt within the JsonValue passed to perform().
     * @return True if the JWT is valid, false otherwise.
     */
    public boolean validateSignedJwt(JsonValue content, VerificationKey verificationKey, JsonPointer location)
            throws NoSuchSecretException {
        SigningHandler signingHandler = new SigningManager(new SecretsProvider(Time.getClock()))
                .newVerificationHandler(verificationKey);
        SignedJwt signedJwt = new JwtReconstruction().reconstructJwt(content.get(location).asString(),
                SignedJwt.class);
        return signedJwt.verify(signingHandler);
    }

    /**
     * Communicates with the push notification delegate to ensure that the device communicating with us is registered,
     * and to retrieve the appropriate endpoint ARN which will later be used to communicate with this device.
     *
     * @param content The content of the response message from the push device.
     * @param realm The realm in which this device exists.
     * @return True if the device was successfully updated, false otherwise.
     */
    public boolean updateDevice(JsonValue content, String realm) {
        try {
            var config = configHelperFactory.getConfigHelperFor(realm).getConfig();
            responseUpdater.updateResponse(config, content, realm);
        } catch (SSOException | SMSException | PushNotificationException e) {
            logger.debug("Failed to update device", e);
            return false;
        }

        return true;
    }

    /**
     * Retrieves the device settings for the user.
     *
     * @param username The username of the user to generate a device profile for.
     * @param mechanismUid The mechanism UID of the device.
     * @param realm The realm in which this device exists.
     * @return The device settings for the user.
     * @throws DevicePersistenceException if the device profile cannot be retrieved.
     */
    public PushDeviceSettings getDeviceSettings(String username, String mechanismUid, String realm)
            throws DevicePersistenceException {
        return userPushDeviceProfileManager.getDeviceProfile(username, realm, mechanismUid);
    }

    /**
     * Saves the device settings to the user's profile, overwriting any existing device profile.
     *
     * @param pushDeviceSettings The device profile to save.
     * @param deviceResponse The response from the device.
     * @param username The username of the user to generate a device profile for.
     * @param realm The realm in which this device exists.
     * @throws DevicePersistenceException if the device profile cannot be saved.
     */
    public void saveDeviceSettings(PushDeviceSettings pushDeviceSettings,
                                   JsonValue deviceResponse,
                                   String username,
                                   String realm) throws DevicePersistenceException {
        pushDeviceSettings.setCommunicationId(deviceResponse.get(COMMUNICATION_ID).asString());
        pushDeviceSettings.setDeviceId(deviceResponse.get(DEVICE_ID).asString());
        pushDeviceSettings.setCommunicationType(deviceResponse.get(COMMUNICATION_TYPE).asString());
        pushDeviceSettings.setDeviceType(deviceResponse.get(DEVICE_TYPE).asString());

        if (deviceResponse.isDefined(DEVICE_NAME)) {
            pushDeviceSettings.setDeviceName(deviceResponse.get(DEVICE_NAME).asString());
        }

        userPushDeviceProfileManager.saveDeviceProfile(username, realm, pushDeviceSettings);
    }
}