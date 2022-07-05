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
 * Copyright 2020-2021 ForgeRock AS.
 */


package org.forgerock.openam.auth.nodes;

import java.math.BigDecimal;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.DecimalBetweenZeroAndOneValidator;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that checks to see if the device is jail broken.
 */
@Node.Metadata(outcomeProvider = DeviceTamperingVerificationNode.OutcomeProvider.class,
        configClass = DeviceTamperingVerificationNode.Config.class,
        tags = {"contextual"})
public class DeviceTamperingVerificationNode extends AbstractDecisionNode implements DeviceProfile {

    private static final String BUNDLE = DeviceTamperingVerificationNode.class.getName();
    private static final String PLATFORM = "platform";
    private static final String JAIL_BREAK_SCORE = "jailBreakScore";
    private final Logger logger = LoggerFactory.getLogger(DeviceTamperingVerificationNode.class);
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Failed when Jailbreak score received from client is greater than this score. (0 - 1)
         *
         * @return Jailbreak score limit
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, DecimalBetweenZeroAndOneValidator.class})
        default String score() {
            return "0";
        }
    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public DeviceTamperingVerificationNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        logger.debug("DeviceTamperingVerificationNode Started");

        JsonValue metadata = getAttribute(context, METADATA_ATTRIBUTE_NAME);
        JsonValue platform = metadata.get(PLATFORM);

        if (platform != null) {
            if (platform.isDefined(JAIL_BREAK_SCORE)) {
                if (platform.get(JAIL_BREAK_SCORE).asDouble() <= new BigDecimal(config.score()).doubleValue()) {
                    logger.debug("Device Tampering verification passed");
                    return goTo(true).build();
                }
            }
        }
        logger.debug("Device Tampering verification failed or JailBreak score is not captured");
        return goTo(false).build();

    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements
            org.forgerock.openam.auth.node.api.StaticOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    DeviceTamperingVerificationNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString(TRUE_OUTCOME_ID)),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString(FALSE_OUTCOME_ID)));
        }
    }
}
