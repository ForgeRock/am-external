/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.forgerock.openam.session.SessionConstants.PERSISTENT_COOKIE_SESSION_PROPERTY;

import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.treehook.CreatePersistentCookieTreeHook;
import org.forgerock.openam.auth.nodes.validators.HmacSigningKeyValidator;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.annotations.adapters.TimeUnit;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node to instruct the tree to set a persistent cookie after the session is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetPersistentCookieNode.Config.class)
public class SetPersistentCookieNode extends SingleOutcomeNode {

    private static final Duration JWT_IDLE_TIMEOUT_IN_HOURS = Duration.duration(5, HOURS);
    private static final Duration JWT_EXPIRY_TIME_IN_HOURS = Duration.duration(5, HOURS);
    private static final String DEFAULT_COOKIE_NAME = "session-jwt";
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The idle time out. If the cookie is not used within this time, the jwt becomes invalid.
         *
         * @return the idle time out in hours.
         */
        @Attribute(order = 100)
        @TimeUnit(HOURS)
        default Duration idleTimeout() {
            return JWT_IDLE_TIMEOUT_IN_HOURS;
        }

        /**
         * The max life. The cookie becomes invalid after this amount of time.
         *
         * @return the max life in hours.
         */
        @Attribute(order = 200)
        @TimeUnit(HOURS)
        default Duration maxLife() {
            return JWT_EXPIRY_TIME_IN_HOURS;
        }

        /**
         * If true, instructs the browser to only send the cookie on secure connections.
         *
         * @return true to use secure cookie.
         */
        @Attribute(order = 300)
        default boolean useSecureCookie() {
            return true;
        }

        /**
         * If true, instructs the browser to prevent access to this cookie and only use it for http.
         *
         * @return true to use http only cookie.
         */
        @Attribute(order = 400)
        default boolean useHttpOnlyCookie() {
            return true;
        }

        /**
         * The signing key.
         *
         * @return the hmac signing key.
         */
        @Attribute(order = 500, validators = {RequiredValueValidator.class, HmacSigningKeyValidator.class})
        @Password
        char[] hmacSigningKey();

        /**
         * The name of the persistent cookie.
         *
         * @return the name of the persistent cookie.
         */
        @Attribute(order = 600)
        default String persistentCookieName() {
            return DEFAULT_COOKIE_NAME;
        }
    }

    private final Config config;
    private final UUID nodeId;

    /**
     * A SetPersistentCookieNode constructor.
     *
     * @param config The service config.
     * @param nodeId the uuid of this node instance.
     *
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public SetPersistentCookieNode(@Assisted Config config, @Assisted UUID nodeId) throws NodeProcessException {
        this.config = config;
        this.nodeId = nodeId;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("SetPersistentCookieNode started");
        logger.debug("persistent cookie set");
        return goToNext()
                .putSessionProperty(PERSISTENT_COOKIE_SESSION_PROPERTY, config.persistentCookieName())
                .addSessionHook(CreatePersistentCookieTreeHook.class, nodeId, getClass().getSimpleName())
                .build();
    }
}
