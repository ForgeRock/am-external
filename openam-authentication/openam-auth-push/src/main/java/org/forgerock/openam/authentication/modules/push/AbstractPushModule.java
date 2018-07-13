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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.push;

import java.io.IOException;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessagePromise;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.utils.CollectionUtils;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * An abstract push module holding the necessary json serialization classes to communicate with the
 * core token service.
 */
public abstract class AbstractPushModule extends AMLoginModule {

    /** Used to make the polling occur every second. Not recommended to be set in production. */
    protected final String nearInstantProperty = "com.forgerock.openam.authentication.push.nearinstant";

    /** Necessary to read data from the appropriate user's attribute. **/
    protected final UserPushDeviceProfileManager userPushDeviceProfileManager =
            InjectorHolder.getInstance(UserPushDeviceProfileManager.class);

    /** Used to communicate messages out from OpenAM through a configurable Push delegate. */
    protected final PushNotificationService pushService
            = InjectorHolder.getInstance(PushNotificationService.class);

    /** Used to understand what loadbalancer cookie we should inform the remote device of. */
    protected final SessionCookies sessionCookies = InjectorHolder.getInstance(SessionCookies.class);

    /** The message promise that this module instance will use to track its mesage through the message dispatcher. */
    protected MessagePromise messagePromise;

    /**
     * Retrieves a Push Device for a user in a realm.
     *
     * @param username Name of the user whose device to retrieve.
     * @param realm Realm in which the user is operating.
     * @return The user's PushDeviceSettings, or null if none exist.
     * @throws AuthLoginException If we were unable to read the user's profile.
     */
    protected PushDeviceSettings getDevice(String username, String realm) throws AuthLoginException {

        try {
            PushDeviceSettings firstDevice
                    = CollectionUtils.getFirstItem(userPushDeviceProfileManager.getDeviceProfiles(username, realm));
            if (null == firstDevice) {
                setFailureID(username);
                throw new AuthLoginException(Constants.AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
            }
            return firstDevice;
        } catch (IOException e) {
            setFailureID(username);
            throw new AuthLoginException(Constants.AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

}
