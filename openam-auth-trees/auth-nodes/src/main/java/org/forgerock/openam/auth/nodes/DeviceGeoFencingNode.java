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

package org.forgerock.openam.auth.nodes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

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
import org.forgerock.util.Pair;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Check if the current device is within a set of trusted location.
 */
@Node.Metadata(outcomeProvider = DeviceGeoFencingNode.OutcomeProvider.class,
        configClass = DeviceGeoFencingNode.Config.class,
        tags = {"contextual"})
public class DeviceGeoFencingNode extends AbstractDecisionNode implements DeviceProfile {

    private static final String BUNDLE = DeviceGeoFencingNode.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceGeoFencingNode.class);
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The Latitude & Longitude of trusted locations.
         *
         * @return A set of trusted locations with format <latitude>,<longitude>
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, GeoFencingValidator.class})
        default Set<String> locations() {
            return Collections.emptySet();
        }

        /**
         * Distance range within the defined trusted location(s) (Use km as Unit).
         *
         * @return The distance
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class, DecimalValidator.class})
        default String distance() {
            return "10";
        }

    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public DeviceGeoFencingNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        LOGGER.debug("DeviceGeoFencingNode Started");

        JsonValue location = getAttribute(context, LOCATION_ATTRIBUTE_NAME);

        for (String loc : config.locations()) {
            Pair<Double, Double> fence = parseLocation(loc);
            if (GeoCoordinate.distance(location.get(LATITUDE).asDouble(),
                    location.get(LONGITUDE).asDouble(),
                    fence.getFirst(),
                    fence.getSecond())
                    < new BigDecimal(config.distance()).doubleValue()) {
                LOGGER.debug("Device location within trusted zone.");
                return goTo(true).build();
            }
        }
        LOGGER.debug("Device location out of the trusted zone.");
        return goTo(false).build();
    }

    static Pair<Double, Double> parseLocation(String location) {
        String[] loc = location.split(",");
        if (loc.length != 2) {
            throw new IllegalArgumentException("Invalid location format, "
                    + "please provide format <latitude>,<longitude>");
        }
        try {
            return Pair.of(Double.parseDouble(loc[0].trim()), Double.parseDouble(loc[1].trim()));
        } catch (NumberFormatException e) {
            LOGGER.debug("Failed to parse location with latitude:{}, longitude {}", loc[0], loc[1]);
            throw new IllegalArgumentException("Failed to parse location, "
                    + "please provide location format as <latitude>,<longitude>");
        }
    }

    /**
     * Validates a value can convert to a Latitude and longitude value, string representation is <latitude>,<longitude>.
     */
    public static class GeoFencingValidator implements ServiceAttributeValidator {

        @Override
        public boolean validate(Set<String> values) {
            if (values.isEmpty()) {
                return false;
            }

            for (String value : values) {
                try {
                    parseLocation(value);
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements
            org.forgerock.openam.auth.node.api.OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    DeviceGeoFencingNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString(TRUE_OUTCOME_ID)),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString(FALSE_OUTCOME_ID)));
        }
    }
}
