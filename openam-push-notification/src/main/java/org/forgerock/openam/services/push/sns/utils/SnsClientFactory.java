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
 * Copyright 2016-2023 ForgeRock AS.
 */
package org.forgerock.openam.services.push.sns.utils;

import java.util.Objects;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;

/**
 * Factory to generate new Amazon SNS clients for a supplied config. Helps decoupling and unit tests.
 */
public class SnsClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(SnsClientFactory.class);

    private final Secrets secrets;
    private final RealmLookup realmLookup;

    /**
     * Constructor.
     *
     * @param secrets     Global Secrets object to be injected or obtained from Guice.
     * @param realmLookup Realm lookup object to be injected or obtained from Guice.
     */
    public SnsClientFactory(Secrets secrets, RealmLookup realmLookup) {
        this.secrets = secrets;
        this.realmLookup = realmLookup;
    }

    /**
     * Generate a new Amazon SNS Client for the given config.
     * <p>
     * The access key for the client is obtained by first querying Secrets for a mapping with label
     * {@link Labels#PUSH_NOTIFICATION_PASSWORD}, and if this doesn't exist then uses the password from the service
     * configuration.
     *
     * @param config      An Amazon SNS client configured using the supplied config.
     * @param realmString A string representing the realm.
     * @return a constructed Amazon SNS client.
     */
    public AmazonSNS produce(PushNotificationServiceConfig.Realm config, String realmString) {
        try {
            Realm realm = realmLookup.lookup(realmString);
            char[] secretPassword = null;
            try {
                secretPassword = secrets.getRealmSecrets(realm).getActiveSecret(
                        Purpose.purpose(Labels.PUSH_NOTIFICATION_PASSWORD, GenericSecret.class)
                ).getOrThrowIfInterrupted().revealAsUtf8AndDestroy(char[]::clone);

            } catch (NoSuchSecretException ex) {
                logger.warn("Problem finding secret with mapping '" + Labels.PUSH_NOTIFICATION_PASSWORD + "'", ex);
            }
            char[] configPassword = config.secret() != null ? config.secret() : null;
            char[] snsPassword = Objects.requireNonNullElse(secretPassword, configPassword);
            BasicAWSCredentials credentials = new BasicAWSCredentials(config.accessKey(), String.valueOf(snsPassword));

            return AmazonSNSClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(config.region())
                    .build();
        } catch (RealmLookupException e) {
            throw new RuntimeException(e);
        }
    }

}
