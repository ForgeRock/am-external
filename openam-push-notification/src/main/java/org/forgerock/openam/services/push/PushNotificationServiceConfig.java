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
 * Copyright 2019-2024 ForgeRock AS.
 */

package org.forgerock.openam.services.push;


import static org.forgerock.openam.annotations.sm.Config.Scope.REALM;
import static org.forgerock.openam.annotations.sm.Config.Scope.SERVICE;

import java.util.Map;

import org.forgerock.am.config.ChoiceValues;
import org.forgerock.am.config.RealmConfiguration;
import org.forgerock.am.config.ServiceComponentConfig;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;
import org.forgerock.openam.sm.annotations.adapters.ExampleValue;
import org.forgerock.openam.sm.annotations.adapters.NumberRange;

import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.sm.annotations.adapters.Password;

/**
 * Service interface containing interfaces holding the config for the Push Notification service.
 */
@Config(scope = SERVICE, name = "PushNotificationService", i18nFile = "amPushNotification",
        resourceName = "pushNotification", descriptionKey = "push-notification-description")
public interface PushNotificationServiceConfig extends ServiceComponentConfig,
        RealmConfiguration<PushNotificationServiceConfig.Realm> {

    /**
     * Realm config interface holding the config for the Push Notification service attributes.
     */
    @Config(scope = REALM)
    interface Realm {
        /**
         * Returns the access key for the Push Notification service.
         *
         * @return the access key for the Push Notification service.
         */
        @Attribute(order = 100, i18nKey = "a010", requiredValue = true)
        @ExampleValue("AKIAIOSFODNN7EXAMPLE")
        String accessKey();

        /**
         * Returns the secret for the Push Notification service.
         *
         * @return the secret for the Push Notification service.
         */
        @Attribute(order = 200, i18nKey = "a020")
        @Password
        char[] secret();

        /**
         * Returns the Apple endpoint for the Push Notification service.
         *
         * @return the Apple endpoint for the Push Notification service.
         */
        @Attribute(order = 300, i18nKey = "a030", requiredValue = true)
        @ExampleValue("arn:aws:sns:us-east-1:1234567890:app/APNS/production")
        String appleEndpoint();

        /**
         * Returns the Google endpoint for the Push Notification service.
         *
         * @return the Google endpoint for the Push Notification service.
         */
        @Attribute(order = 400, i18nKey = "a040", requiredValue = true)
        @ExampleValue("arn:aws:sns:us-east-1:1234567890:app/GCM/production")
        String googleEndpoint();

        /**
         * Returns the region for the Push Notification service.
         *
         * @return the region for the Push Notification service.
         */
        @Attribute(order = 500, i18nKey = "a045", requiredValue = true, choiceValuesClass = RegionChoiceValues.class)
        default String region() {
            return "us-east-1";
        }

        /**
         * Returns the delegate factory for the Push Notification service.
         *
         * @return the delegate factory for the Push Notification service.
         */
        @Attribute(order = 600, i18nKey = "a050", requiredValue = true)
        default String delegateFactory() {
            return "org.forgerock.openam.services.push.sns.SnsHttpDelegateFactory";
        }

        /**
         * Returns the MD duration for the Push Notification service.
         *
         * @return the MD duration for the Push Notification service.
         */
        @Attribute(order = 700, i18nKey = "a060", requiredValue = true)
        @NumberRange
        default Integer mdDuration() {
            return 120;
        }

        /**
         * Returns the MD concurrency for the Push Notification service.
         *
         * @return the MD concurrency for the Push Notification service.
         */
        @Attribute(order = 800, i18nKey = "a070", requiredValue = true)
        @NumberRange(rangeStart = 1, rangeEnd = 16)
        default Integer mdConcurrency() {
            return 16;
        }

        /**
         * Returns the MD cache size for the Push Notification service.
         *
         * @return the MD cache size for the Push Notification service.
         */
        @Attribute(order = 900, i18nKey = "a080", requiredValue = true)
        @NumberRange
        default Integer mdCacheSize() {
            return 10000;
        }

    }

    /**
     * Choice values for the region configuration.
     */
    class RegionChoiceValues implements ChoiceValues {

        @Override
        public Map<String, String> getChoiceValues() {
            return ImmutableMap.<String, String>builder()
                .put("us-gov-west-1", "us-gov-west-1")
                .put("us-east-1", "us-east-1")
                .put("us-west-1", "us-west-1")
                .put("us-west-2", "us-west-2")
                .put("eu-west-1", "eu-west-1")
                .put("eu-central-1", "eu-central-1")
                .put("ap-southeast-1", "ap-southeast-1")
                .put("ap-southeast-2", "ap-southeast-2")
                .put("ap-southeast-3", "ap-southeast-3")
                .put("ap-northeast-1", "ap-northeast-1")
                .put("ap-northeast-2", "ap-northeast-2")
                .put("sa-east-1", "sa-east-1")
                .put("ca-central-1", "ca-central-1")
                .put("n-north-1", "n-north-1")
                .build();
        }
    }
}
