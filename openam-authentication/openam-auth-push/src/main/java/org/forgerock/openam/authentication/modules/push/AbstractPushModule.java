/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.push;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.rest.authn.mobile.TwoFactorAMLoginModule;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessagePromise;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.utils.CollectionUtils;

import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * An abstract push module holding the necessary json serialization classes to communicate with the
 * core token service.
 */
public abstract class AbstractPushModule extends TwoFactorAMLoginModule {

    /** Used to make the polling occur every second. Not recommended to be set in production. */
    protected final String nearInstantProperty = "com.forgerock.openam.authentication.push.nearinstant";

    /** Necessary to read data from the appropriate user's attribute. **/
    protected final UserPushDeviceProfileManager userPushDeviceProfileManager =
            InjectorHolder.getInstance(UserPushDeviceProfileManager.class);

    /** Used to communicate messages out from OpenAM through a configurable Push delegate. */
    protected final PushNotificationService pushService
            = InjectorHolder.getInstance(PushNotificationService.class);

    /**
     * Used to create message Keys.
     */
    protected final MessageIdFactory messageIdFactory = InjectorHolder.getInstance(MessageIdFactory.class);

    /** Used to understand what loadbalancer cookie we should inform the remote device of. */
    protected final SessionCookies sessionCookies = InjectorHolder.getInstance(SessionCookies.class);

    /** The message promise that this module instance will use to track its message through the message dispatcher. */
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
        } catch (DevicePersistenceException dpe) {
            setFailureID(username);
            throw new AuthLoginException(Constants.AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

}
