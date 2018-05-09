/*
<<<<<<< HEAD
 * Copyright 2018 ForgeRock AS. All Rights Reserved
||||||| merged common ancestors
 * Copyright 2017 ForgeRock AS. All Rights Reserved
=======
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
>>>>>>> AME-14783 Adding sample tree API and sample trees.
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_TIMESTAMP;

import java.time.Duration;
import java.time.Instant;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node which collects a password from the user via a password callback
 * and then decides if the current auth level is greater than or equal to a fixed, configurable amount.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = OneTimePasswordCollectorDecisionNode.Config.class)
public class OneTimePasswordCollectorDecisionNode extends AbstractDecisionNode {

    /**
     * Configuration for the one time password collector decision node.
     */
    public interface Config {
        /**
         * Default of 5 minutes.
         * @return the password expiry time.
         */
        @Attribute(order = 100)
        default long passwordExpiryTime() {
            return 5;
        }
    }

    private final OneTimePasswordCollectorDecisionNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public OneTimePasswordCollectorDecisionNode(@Assisted OneTimePasswordCollectorDecisionNode.Config config)
            throws NodeProcessException {
        this.config = config;
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/OneTimePasswordCollectorDecisionNode";

    @Override
    public Action process(TreeContext context) {
        logger.debug("OneTimePasswordCollectorDecisionNode started");
        return context.getCallback(PasswordCallback.class)
                .map(PasswordCallback::getPassword)
                .map(String::new)
                .filter(password -> !Strings.isNullOrEmpty(password))
                .map(password -> checkPassword(context, password))
                .orElseGet(() -> collectPassword(context));
    }

    private Action checkPassword(TreeContext context, String password) {
        JsonValue oneTimePassword = context.sharedState.get(ONE_TIME_PASSWORD);
        JsonValue passwordTimestamp = context.sharedState.get(ONE_TIME_PASSWORD_TIMESTAMP);
        logger.debug("oneTimePassword {} \n passwordTimestamp {}", oneTimePassword, passwordTimestamp);

        boolean passwordMatches = oneTimePassword.isString()
                && oneTimePassword.asString().equals(password)
                && passwordTimestamp.isNumber()
                && isWithinExpiryTime(passwordTimestamp.asLong());
        logger.debug("passwordMatches {}", passwordMatches);
        return goTo(passwordMatches).build();
    }

    private boolean isWithinExpiryTime(long passwordTimestamp) {
        Instant previous = Instant.ofEpochSecond(passwordTimestamp);
        Duration passwordExpiry = Duration.ofMinutes(config.passwordExpiryTime());
        Instant now = Time.getClock().instant();
        logger.debug("previous {} \n passwordExpiry {} \n now {}", previous, passwordExpiry, now);
        boolean withinExpiryTime = Duration.between(previous.plus(passwordExpiry), now).isNegative();
        logger.debug("withinExpiryTime {}", withinExpiryTime);
        return withinExpiryTime;
    }

    private Action collectPassword(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        return send(new PasswordCallback(bundle.getString("callback.password"), false)).build();
    }
}
