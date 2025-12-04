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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.services.push;

/**
 * Helper for reading a PushNotificationService config, to decouple and aid testing.
 */
public class PushNotificationServiceConfigHelper {

    private PushNotificationServiceConfig.Realm serviceConfig;

    /**
     * Produce a new PushNotificationServiceConfigHelper for the provided ServiceConfig.
     *
     * @param serviceConfig The realm-specific service config to read.
     */
    public PushNotificationServiceConfigHelper(PushNotificationServiceConfig.Realm serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    /**
     * Retrieve the factory class used to generate PushNotificationDelegates described by this config.
     * @return A String containing the classname of the PushNotificationDelegateFactory class to use.
     */
    public String getFactoryClass() {
        return serviceConfig.delegateFactory();
    }

    /**
     * Retrieve a new PushNotificationConfig from this Helper.
     * @return A valid PushNotificationConfig for the delegate described by this service config.
     * @throws PushNotificationException if there was an issue building a config object from the service data.
     */
    public PushNotificationServiceConfig.Realm getConfig() {
        return serviceConfig;
    }
}
