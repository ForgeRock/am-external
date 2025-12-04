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
 * Copyright 2017-2025 Ping Identity Corporation.
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

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.assistedinject.Assisted;

/**
 * A node which collects a password from the user via a password callback
 * and then decides if the current auth level is greater than or equal to a fixed, configurable amount.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = OneTimePasswordCollectorDecisionNode.Config.class,
        tags = {"otp", "mfa", "multi-factor authentication"})
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
    private final Logger logger = LoggerFactory.getLogger(OneTimePasswordCollectorDecisionNode.class);

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

    private static final String BUNDLE = OneTimePasswordCollectorDecisionNode.class.getName();

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
        NodeState nodeState = context.getStateFor(this);
        JsonValue oneTimePassword = nodeState.get(ONE_TIME_PASSWORD);
        JsonValue passwordTimestamp = nodeState.get(ONE_TIME_PASSWORD_TIMESTAMP);

        boolean passwordMatches = oneTimePassword != null && oneTimePassword.isString()
                && oneTimePassword.asString().equals(password)
                && passwordTimestamp != null && passwordTimestamp.isNumber()
                && isWithinExpiryTime(passwordTimestamp.asLong());
        logger.debug("passwordMatches {}", passwordMatches);

        if (passwordMatches) {
            nodeState.remove(ONE_TIME_PASSWORD);
            nodeState.remove(ONE_TIME_PASSWORD_TIMESTAMP);
        }

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

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(ONE_TIME_PASSWORD),
            new InputState(ONE_TIME_PASSWORD_TIMESTAMP)
        };
    }
}
