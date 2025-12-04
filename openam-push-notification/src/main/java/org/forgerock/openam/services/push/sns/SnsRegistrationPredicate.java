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
package org.forgerock.openam.services.push.sns;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.PushNotificationServiceConfigHelperFactory;
import org.forgerock.openam.services.push.dispatch.predicates.AbstractPredicate;
import org.forgerock.openam.services.push.utils.PushDeviceUpdater;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This predicate is used to register the device that is currently talking to the server
 * from the mobile app.
 *
 * It does NOT have a listener attached to its config reading subsystem, therefore it will
 * use the values from the config at the point of time the message creates this class.
 */
public class SnsRegistrationPredicate extends AbstractPredicate {

    @JsonIgnore
    private final PushDeviceUpdater pushDeviceUpdater;

    private String realm;

    /**
     * Default constructor for the SnsRegistrationPredicate, used for serialization and deserialization
     * purposes.
     */
    public SnsRegistrationPredicate() {
        PushNotificationService pushNotificationService = InjectorHolder.getInstance(PushNotificationService.class);
        UserPushDeviceProfileManager userPushDeviceProfileManager = InjectorHolder.getInstance(
                UserPushDeviceProfileManager.class);
        PushNotificationServiceConfigHelperFactory configHelperFactory = InjectorHolder.getInstance(
                PushNotificationServiceConfigHelperFactory.class);
        pushDeviceUpdater = new PushDeviceUpdater(configHelperFactory, userPushDeviceProfileManager,
                pushNotificationService);
    }

    /**
     * Generate a new SnsRegistrationPredicate, which will use the supplied realm to read the config
     * to gather the information necessary to communicate with SNS.
     *
     * @param realm The realm in which this predicate exists.
     */
    public SnsRegistrationPredicate(String realm) {
        this();
        this.realm = realm;
    }

    /**
     * Communicates with Amazon to ensure that the device communicating with us is registered with
     * Amazon, and to retrieve the appropriate endpoint ARN which will later be used to
     * communicate with this device.
     *
     * Finally, expands the contents out to be readable by the registration module itself, including
     * the communicationId.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean perform(JsonValue content) {
        return pushDeviceUpdater.updateDevice(content, realm);
    }

    /**
     * Sets the realm for this predicate. Used when deserialized from the CTS.
     * @param realm The location for this predicate.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }
}
