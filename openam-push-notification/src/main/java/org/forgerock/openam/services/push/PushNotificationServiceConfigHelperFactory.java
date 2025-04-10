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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.services.push;

import java.util.Optional;

import org.forgerock.am.config.ServiceConfigException;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Decouples the PushNotificationService from its config to aid testing.
 */
@Singleton
public class PushNotificationServiceConfigHelperFactory {

    private final PushNotificationServiceConfig config;
    private final Logger debug = LoggerFactory.getLogger(PushNotificationServiceConfigHelperFactory.class);

    /**
     * Construct a new PushNotificationServiceConfigHelperFactory which will produce
     * PushNotificationServiceConfigHelpers for the provided realm.
     *
     * Be sure to call addListener on this object after it's newly constructed to ensure
     * the services are kept up-to-date with any alterations.
     *
     * @param config The Push Notification Service config.
     */
    @Inject
    public PushNotificationServiceConfigHelperFactory(PushNotificationServiceConfig config) {
        this.config = config;
    }

    /**
     * Produces a config helper for a given realm.
     *
     * @param realm The realm under which to read the config from.
     * @return A new PushNotificationServiceConfigHelper for the service's config for the provided realm.
     * @throws SSOException If the user does not have privs to read the org config.
     * @throws SMSException If the retrieved org config was null.
     */
    public PushNotificationServiceConfigHelper getConfigHelperFor(String realm)
            throws SSOException, SMSException {
        try {
            Optional<PushNotificationServiceConfig.Realm> serviceConfig = config.realmSingleton(Realms.of(realm));
            return new PushNotificationServiceConfigHelper(serviceConfig.get());
        } catch (RealmLookupException | ServiceConfigException e) {
            debug.error("Unable to retrieve instance of the ServiceConfig for realm {}.", realm);
            throw new SMSException("Unable to retrieve instance of the ServiceConfig.");
        }
    }
}
