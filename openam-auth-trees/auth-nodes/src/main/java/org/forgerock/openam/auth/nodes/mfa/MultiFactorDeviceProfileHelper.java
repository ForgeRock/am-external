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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.DeviceProfileManager;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.utils.Alphabet;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * Helper methods to deal with second factor DeviceSettings.
 * @param <T> The device profile settings.
 */
public abstract class MultiFactorDeviceProfileHelper<T extends DeviceSettings> {

    private static final Logger logger = LoggerFactory.getLogger(MultiFactorDeviceProfileHelper.class);

    /** Realm of the requesting tree. */
    protected final Realm realm;
    /** Conversion utility for device settings objects and Json. */
    protected final DeviceJsonUtils<T> deviceJsonUtils;
    /** Manages user's device profiles. */
    protected final DeviceProfileManager<T> deviceProfileManager;
    /** Generates recovery codes. */
    protected final RecoveryCodeGenerator recoveryCodeGenerator;

    /**
     * The constructor for this helper.
     *
     * @param realm the realm of the requesting tree.
     * @param deviceJsonUtils conversion utility for device settings objects and Json.
     * @param deviceProfileManager manages user's device profiles.
     * @param recoveryCodeGenerator generates recovery codes.
     */
    @Inject
    public MultiFactorDeviceProfileHelper(@Assisted Realm realm,
            DeviceJsonUtils<T> deviceJsonUtils,
            DeviceProfileManager<T> deviceProfileManager,
            RecoveryCodeGenerator recoveryCodeGenerator) {
        this.realm = realm;
        this.deviceJsonUtils = deviceJsonUtils;
        this.deviceProfileManager = deviceProfileManager;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
    }

    /**
     * Encode a payload for inclusion in a shared state.
     *
     * @param payload the payload to be encrypted.
     * @return the encrypted payload.
     */
    public String encode(JsonValue payload) {
        byte[] payloadBytes = payload.toString().getBytes();
        return Base64.encode(payloadBytes);
    }

    /**
     * Decode an payload from a shared state.
     *
     * @param payload the payload to be decoded.
     * @return the encoded payload.
     */
    public JsonValue decode(String payload) {
        String json = new String(Base64.decode(payload));
        JsonValue jsonValue = JsonValueBuilder.toJsonValue(json);
        return jsonValue;
    }

    /**
     * Returns the realm of the requesting tree.
     *
     * @param context the tree context.
     * @return the realm as string.
     */
    public String getRealm(TreeContext context) {
        return context.sharedState.get(REALM).asString();
    }

    /**
     * Return the device settings object stored on the shared state.
     *
     * @param context the tree context.
     * @param key the key used to store the device profile on the shared state.
     * @return the device settings object.
     */
    public T getDeviceProfileFromSharedState(TreeContext context, String key) {
        JsonValue deviceProfileJsonNode = context.sharedState.get(key);

        if (deviceProfileJsonNode.isNull()) {
            logger.debug("No device profile found in shared state");
            return null;
        }

        logger.debug("Retrieving device profile found in shared state");

        T deviceProfile;
        try {
            deviceProfile = decodeDeviceSettings(deviceProfileJsonNode.asString());
        } catch (IOException e) {
            logger.error("Cannot deserialize device profile from shared state", e);
            return null;
        }

        return deviceProfile;
    }

    /**
     * Encode an device settings object as a {@link JsonValue} object which represents it.
     *
     * @param settings the device settings to encode.
     * @return the encoded {@link JsonValue} object.
     * @throws IOException if the device settings object could not be encoded.
     */
    public String encodeDeviceSettings(T settings) throws IOException {
        return encode(deviceJsonUtils.toJsonValue(settings));
    }

    private T decodeDeviceSettings(String payload) throws IOException {
        return deviceJsonUtils.toDeviceSettingValue(decode(payload));
    }

    /**
     * Check if device settings is stored.
     *
     * @param username the username of user.
     * @return Indicator if device is stored.
     */
    public boolean isDeviceSettingsStored(String username) {
        boolean stored = false;
        try {
            stored = !deviceProfileManager.getDeviceProfiles(username, realm.toString()).isEmpty();
        } catch (DevicePersistenceException e) {
            logger.error("Unable to talk to datastore.", e);
        }
        return stored;
    }

    /**
     * Retrieves the device settings of the user's profile.
     *
     * @param realm the realm.
     * @param username the username.
     * @return the device settings.
     * @throws NodeProcessException if device settings could not be retrieved.
     */
    public T getDeviceSettings(String realm, String username) throws NodeProcessException {
        try {
            return CollectionUtils.getFirstItem(deviceProfileManager.getDeviceProfiles(username, realm));
        } catch (DevicePersistenceException dpe) {
            throw new NodeProcessException(dpe);
        }
    }

    /**
     * Save the device's settings on the user's profile.
     *
     * @param deviceSettings the device's settings.
     * @param deviceResponse the device data.
     * @param identity the user name.
     * @param generateRecoveryCodes indicator if recovery codes should be generated.
     * @return a set of recovery codes.
     * @throws NodeProcessException if unable to store device profile or generate the recovery codes.
     */
    public abstract List<String> saveDeviceSettings(
            T deviceSettings,
            JsonValue deviceResponse,
            AMIdentity identity,
            boolean generateRecoveryCodes) throws NodeProcessException;

    /**
     * Generate recovery codes.
     *
     * @return List of recovery codes.
     * @throws NodeProcessException if unable to generate recovery codes
     */
    protected List<String> generateRecoveryCodes() throws NodeProcessException {
        try {
            return recoveryCodeGenerator.generateCodes(MultiFactorConstants.NUM_RECOVEY_CODES,
                    Alphabet.ALPHANUMERIC, false);
        } catch (CodeException e) {
            throw new NodeProcessException("Failed to generate recovery codes", e);
        }
    }
}

