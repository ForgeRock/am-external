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
 * Copyright 2020-2024 ForgeRock AS.
 */

package org.forgerock.openam.radius.server.config;

import static org.forgerock.openam.annotations.sm.Config.Scope.GLOBAL;
import static org.forgerock.openam.annotations.sm.Config.Scope.SERVICE;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.Set;

import org.forgerock.am.config.GlobalConfiguration;
import org.forgerock.am.config.ServiceComponentConfig;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;
import org.forgerock.openam.annotations.sm.I18nKey;
import org.forgerock.openam.annotations.sm.SubConfig;
import org.forgerock.openam.sm.annotations.adapters.NumberRange;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;

/**
 * Service interface for configuring Radius Server Service.
 */
@Config(scope = SERVICE,
        name = "RadiusServerService",
        i18nFile = "radiusServer",
        descriptionKey = "radius-server-service-description")
public interface RadiusServerServiceConfig
        extends ServiceComponentConfig,
        GlobalConfiguration<RadiusServerServiceConfig.Global> {

    /**
     * Global config interface holding the config for the Radius Server service attributes.
     */
    @Config(scope = GLOBAL)
    interface Global {

        /**
         * Returns whether the Radius Listener is enabled.
         *
         * @return whether the Radius Listener is enabled.
         */
        @Attribute(order = 200,
                name = "radiusListenerEnabled",
                i18nKey = "a-radius-listener-enabled-label")
        default RadiusServerServiceConfig.YesNoChoice radiusListenerEnabled() {
            return RadiusServerServiceConfig.YesNoChoice.NO;
        }

        /**
         * Returns the Radius Server Port.
         *
         * @return the Radius Server Port.
         */
        @Attribute(order = 400,
                name = "radiusServerPort",
                i18nKey = "b-radius-port")
        @NumberRange(rangeStart = 1025, rangeEnd = 65535)
        default Integer radiusServerPort() {
            return 1812;
        }

        /**
         * Returns the Radius thread pool core size.
         *
         *  @return the Radius thread pool core size.
         */
        @Attribute(order = 600,
                name = "radiusThreadPoolCoreSize",
                i18nKey = "c-radius-thread-pool-core-size")
        @NumberRange(rangeStart = 1, rangeEnd = 100)
        default Integer radiusThreadPoolCoreSize() {
            return 1;
        }

        /**
         * Returns the Radius thread pool max size.
         *
         * @return the Radius thread pool max size.
         */
        @Attribute(order = 800,
                name = "radiusThreadPoolMaxSize",
                i18nKey = "d-radius-thread-pool-max-size")
        @NumberRange(rangeStart = 1, rangeEnd = 100)
        default Integer radiusThreadPoolMaxSize() {
            return 10;
        }

        /**
         * Returns the Radius thread pool keep alive seconds.
         *
         * @return the Radius thread pool keep alive seconds.
         */
        @Attribute(order = 1000,
                name = "radiusThreadPoolKeepaliveSeconds",
                i18nKey = "e-radius-thread-pool-keepalive-seconds")
        @NumberRange(rangeStart = 1, rangeEnd = 3600)
        default Integer radiusThreadPoolKeepaliveSeconds() {
            return 10;
        }

        /**
         * Returns the Radius thread pool queue size.
         *
         * @return the Radius thread pool queue size.
         */
        @Attribute(order = 1100,
                name = "radiusThreadPoolQueueSize",
                i18nKey = "f-radius-thread-pool-queue-size")
        @NumberRange(rangeStart = 1, rangeEnd = 1000)
        default Integer radiusThreadPoolQueueSize() {
            return 20;
        }

        /**
         * Returns the Radius client(s).
         *
         * @return the Radius client(s).
         */
        @SubConfig(
                name = "radiusClient",
                container = false
        )
        Multiple<RadiusClient> radiusClient();
    }

    /**
     * Secondary config interface holding the config for the Radius Server service attributes.
     */
    interface RadiusClient {
        /**
         * Returns the Radius client IP address.
         *
         * @return the Radius client IP address.
         */
        @Attribute(
                order = 100,
                i18nKey = "a-client-ip-address-label",
                name = "clientIpAddress"
        )
        default String clientIpAddress() {
            return "/127.0.0.1";
        }

        /**
         * Returns the Radius client secret.
         *
         * @return the Radius client secret.
         */
        @Attribute(
                order = 300,
                i18nKey = "b-client-secret-label",
                name = "clientSecret",
                requiredValue = true
        )
        @Password
        char[] clientSecret();

        /**
         * Returns whether the Radius client packets are logged.
         *
         * @return whether the Radius client packets are logged.
         */
        @Attribute(
                order = 500,
                i18nKey = "c-client-log-packets",
                name = "clientPacketsLogged"
        )
        default YesNoChoice clientPacketsLogged() {
            return YesNoChoice.NO;
        }

        /**
         * Returns the Radius client handler class.
         *
         * @return the Radius client handler class.
         */
        @Attribute(
                order = 700,
                i18nKey = "d-handler-class",
                name = "handlerClass"
        )
        default String handlerClass() {
            return "org.forgerock.openam.radius.server.spi.handlers.OpenAMAuthHandler";
        }

        /**
         * Returns the Radius client handler config.
         *
         * @return the Radius client handler config.
         */
        @Attribute(
                order = 900,
                i18nKey = "e-handler-config-params",
                name = "handlerConfig"
        )
        default Set<String> handlerConfig() {
            return asSet("realm=/", "chain=ldapService");
        }
    }

    /**
     * Choice values for Listener enabled.
     */
    enum YesNoChoice {
        /** No Choice. */
        @I18nKey("choiceNO")
        NO("NO"),
        /** Yes Choice. */
        @I18nKey("choiceYES")
        YES("YES");
        private final String name;

        YesNoChoice(String name) {
            this.name = name;
        }
    }
}

