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


package org.forgerock.openam.auth.nodes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.GeoCoordinate;
import org.forgerock.openam.auth.nodes.validators.DecimalValidator;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Check if the current device location is within the distance range from last stored location.
 */
@Node.Metadata(outcomeProvider = DeviceLocationMatchNode.OutcomeProvider.class,
        configClass = DeviceLocationMatchNode.Config.class,
        tags = {"contextual"})
public class DeviceLocationMatchNode extends AbstractDecisionNode implements DeviceProfile {

    private static final String BUNDLE = DeviceLocationMatchNode.class.getName();
    private static final String UNKNOWN_DEVICE_OUTCOME_ID = "unknownDevice";
    private final Logger logger = LoggerFactory.getLogger(DeviceLocationMatchNode.class);

    private final CoreWrapper coreWrapper;
    private final IdentityUtils identityUtils;
    private final Config config;
    private final Realm realm;
    private final DeviceProfilesDao deviceProfilesDao;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Distance range from last stored location.
         *
         * @return Distance Range
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class,
                DecimalValidator.class})
        default String distance() {
            return "10";
        }

    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param deviceProfilesDao A DeviceProfilesDao Instance
     * @param coreWrapper       A CoreWrapper Instance
     * @param identityUtils     An IdentityUtils Instance
     * @param config            The Node Config
     * @param realm             The Realm
     */
    @Inject
    public DeviceLocationMatchNode(DeviceProfilesDao deviceProfilesDao,
            CoreWrapper coreWrapper, IdentityUtils identityUtils, @Assisted Config config,
            @Assisted Realm realm) {
        this.deviceProfilesDao = deviceProfilesDao;
        this.coreWrapper = coreWrapper;
        this.identityUtils = identityUtils;
        this.config = config;
        this.realm = realm;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        logger.debug("DeviceLocationMatchNode Started");

        JsonValue location = getAttribute(context, LOCATION_ATTRIBUTE_NAME);
        String identifier = getAttribute(context, IDENTIFIER_ATTRIBUTE_NAME).asString();
        Double latitude = null;
        Double longitude = null;

        try {

            AMIdentity identity = getUserIdentity(context.universalId, context.getStateFor(this), coreWrapper,
                    identityUtils);
            List<JsonValue> devices = deviceProfilesDao
                    .getDeviceProfiles(identity.getName(), realm.asPath());
            Optional<JsonValue> result = devices.stream()
                    //Find matching device with same identifier and has location
                    .filter(s -> {
                        if (identifier
                                .equals(s.get(IDENTIFIER_ATTRIBUTE_NAME).asString())) {
                            JsonValue loc = s.get(LOCATION_ATTRIBUTE_NAME);
                            return loc.isDefined(LONGITUDE) && loc.isDefined(LATITUDE);
                        }
                        return false;
                    })
                    .findFirst();

            if (result.isPresent()) {
                logger.debug("Found device with stored location");
                JsonValue device = result.get();
                JsonValue storedLocation = device.get(LOCATION_ATTRIBUTE_NAME);
                device.put(LAST_SELECTED_DATE, Time.currentTimeMillis());
                deviceProfilesDao.saveDeviceProfiles(identity.getName(), realm.asPath(), devices);
                latitude = storedLocation.get(LATITUDE).asDouble();
                longitude = storedLocation.get(LONGITUDE).asDouble();
                if (GeoCoordinate.distance(location.get(LATITUDE).asDouble(),
                        location.get(LONGITUDE).asDouble(),
                        latitude,
                        longitude)
                        < new BigDecimal(config.distance()).doubleValue()) {
                    logger.debug("Device within range with the last stored location");
                    return goTo(true).build();
                } else {
                    logger.debug("Device out of range with the last stored location");
                    return goTo(false).build();
                }
            } else {
                logger.debug("Device location not found");
                return Action.goTo(UNKNOWN_DEVICE_OUTCOME_ID).build();
            }

        } catch (Exception e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements
            org.forgerock.openam.auth.node.api.StaticOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    DeviceLocationMatchNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString(TRUE_OUTCOME_ID)),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString(FALSE_OUTCOME_ID)),
                    new Outcome(UNKNOWN_DEVICE_OUTCOME_ID, bundle.getString(UNKNOWN_DEVICE_OUTCOME_ID)));
        }
    }
}
