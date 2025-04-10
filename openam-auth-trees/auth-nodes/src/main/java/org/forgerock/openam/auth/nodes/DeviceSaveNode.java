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
 * Copyright 2020-2025 Ping Identity Corporation.
 */


package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.validation.PositiveIntegerValidator;

/**
 * A node that persist the device metadata and location to the data store.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = DeviceSaveNode.Config.class,
        tags = {"contextual"})
public class DeviceSaveNode extends SingleOutcomeNode implements DeviceProfile {

    private final Logger logger = LoggerFactory.getLogger(DeviceSaveNode.class);
    private final Config config;
    private final Realm realm;
    private final DeviceProfilesDao deviceProfilesDao;
    private final NodeUserIdentityProvider identityProvider;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The shared state variable name to store the device name.
         *
         * @return the variable name to store the device name
         */
        @Attribute(order = 100)
        default String variableName() {
            return "";
        }

        /**
         * Maximum stored profile quantity.
         *
         * @return the maximum stored profile quantity
         */
        @Attribute(order = 200, requiredValue = true, validators = {PositiveIntegerValidator.class})
        default int maxSavedProfiles() {
            return 5;
        }

        /**
         * Specify whether device metadata should be saved.
         *
         * @return true to store device metadata.
         */
        @Attribute(order = 300)
        default boolean saveDeviceMetadata() {
            return true;
        }

        /**
         * Specify whether device location should be saved.
         *
         * @return true to store device location.
         */
        @Attribute(order = 400)
        default boolean saveDeviceLocation() {
            return true;
        }

    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param config            The service config.
     * @param realm             The realm the node is in.
     * @param deviceProfilesDao The Device DAO to access device profile
     * @param identityProvider  The identity provider
     */
    @Inject
    public DeviceSaveNode(@Assisted Config config,
            @Assisted Realm realm, DeviceProfilesDao deviceProfilesDao, NodeUserIdentityProvider identityProvider) {
        this.config = config;
        this.realm = realm;
        this.deviceProfilesDao = deviceProfilesDao;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("DeviceProfileSaveNode Started");
        try {
            AMIdentity identity = getUserIdentity(context.universalId, context.getStateFor(this), identityProvider);
            save(context, identity);
        } catch (IdRepoException | SSOException e) {
            throw new NodeProcessException(e);
        }

        return goToNext().build();

    }

    /**
     * Persist the attribute to user identity.
     *
     * @param identity The user identity
     */
    private void save(TreeContext context, AMIdentity identity)
            throws NodeProcessException {

        final JsonValue[] device = {JsonValueBuilder.jsonValue().build()};
        String identifier = getAttribute(context, IDENTIFIER_ATTRIBUTE_NAME).asString();

        //Remove and overwrite the existing one base on the "identifier"
        try {
            List<JsonValue> devices = deviceProfilesDao
                    .getDeviceProfiles(identity.getName(), realm.asPath());
            //Find if the device already exist, if found update it.
            List<JsonValue> updatedDevices = devices.stream().filter(s -> {
                if (identifier.equals(s.get(IDENTIFIER_ATTRIBUTE_NAME).asString())) {
                    device[0] = s;
                    return false;
                }
                return true;
            }).collect(Collectors.toList());

            device[0].put(IDENTIFIER_ATTRIBUTE_NAME, identifier);

            if (config.saveDeviceMetadata()) {
                setAttribute(context, device[0], METADATA_ATTRIBUTE_NAME);
            }
            if (config.saveDeviceLocation()) {
                setAttribute(context, device[0], LOCATION_ATTRIBUTE_NAME);
            }

            device[0].put(LAST_SELECTED_DATE, currentTimeMillis());
            setDeviceAlias(context, device[0]);
            updatedDevices.add(device[0]);

            filter(updatedDevices);

            deviceProfilesDao.saveDeviceProfiles(identity.getName(), realm.asPath(), updatedDevices);
        } catch (DevicePersistenceException e) {
            throw new NodeProcessException(e);
        }
    }

    private void setAttribute(TreeContext context, JsonValue source, String attribute) {
        try {
            JsonValue value = getAttribute(context, attribute);
            source.put(attribute, value);
        } catch (NodeProcessException e) {
            //ignore if not defined.
        }
    }

    /**
     * Set the device alias from shared state or from device metadata.
     *
     * @param context The TreeContext
     * @param device  The device profile as JSON
     */
    private void setDeviceAlias(TreeContext context, JsonValue device) {
        String alias = "";

        if (StringUtils.isNotEmpty(config.variableName())) {
            alias = context.sharedState.get(config.variableName()).asString();
        }
        if (StringUtils.isEmpty(alias)) {
            if (!device.isDefined(ALIAS)) {
                //Extract from metadata data
                JsonValue deviceName = device.get(new JsonPointer(METADATA_ATTRIBUTE_NAME, "platform", "deviceName"));
                if (deviceName != null) {
                    device.put(ALIAS, deviceName.asString());
                }
            }
        } else {
            device.put(ALIAS, alias);
        }
    }

    private void filter(List<JsonValue> devices) {

        while (devices.size() > config.maxSavedProfiles()) {

            JsonValue oldestProfile = null;
            long oldestDate = currentTimeMillis();

            for (JsonValue profile : devices) {
                long lastSelectedDate = profile.get(LAST_SELECTED_DATE).asLong();
                if (lastSelectedDate < oldestDate) {
                    oldestDate = lastSelectedDate;
                    oldestProfile = profile;
                }
            }

            if (oldestProfile != null) {
                devices.remove(oldestProfile);
            }
        }
    }
}
