/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.push;

import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.DeviceService;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.TwoFactorEncryptedDeviceStorage;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.SMSException;

/**
 * Set of utility methods which are shared amongst second factor node implementations.
 *
 * {@see org.forgerock.openam.core.rest.authn.mobile.TwoFactorAMLoginModule}
 */
final class SecondFactorNodeDelegate<T extends TwoFactorEncryptedDeviceStorage & DeviceService> {

    private AuthenticatorDeviceServiceFactory<T> deviceServiceFactory;

    @Inject
    public SecondFactorNodeDelegate(AuthenticatorDeviceServiceFactory<T> deviceServiceFactory) {
        this.deviceServiceFactory = deviceServiceFactory;
    }

    /**
     * Determines if the current device should be skipped.
     * @param amIdentity The identity of the user.
     * @param realm The realm.
     * @return True if the step should be skipped, false otherwise.
     * @throws NodeProcessException If anything goes wrong.
     */
    SkipSetting shouldSkip(AMIdentity amIdentity, String realm) throws NodeProcessException {
        return userConfiguredSkippable(amIdentity, getDeviceStorage(realm));
    }

    /**
     * Returns whether or not it is necessary to run this module through to completion.
     *
     * @param identity Identity of the user attempting to login.
     * @param realmService The service used to verify the user's skippable attribute.
     * @return  0 ({@link SkipSetting#NOT_SET}), or
     *          1 ({@link SkipSetting#SKIPPABLE}), or
     *          2 ({@link SkipSetting#NOT_SKIPPABLE})
     */
    private SkipSetting userConfiguredSkippable(AMIdentity identity, T realmService)
            throws NodeProcessException {

        Set<String> response;
        try {
            response = identity.getAttribute(realmService.getSkippableAttributeName());
        } catch (IdRepoException | SSOException e) {
            throw new NodeProcessException(e);
        }
        if (response != null && !response.isEmpty()) {
            String tmp = response.iterator().next();
            return SkipSetting.getSettingFor(Integer.valueOf(tmp));
        } else {
            return SkipSetting.NOT_SET;
        }
    }

    private T getDeviceStorage(String realm) throws NodeProcessException {
        try {
            return deviceServiceFactory.create(realm);
        } catch (SMSException | SSOException e) {
            throw new NodeProcessException(e);
        }
    }
}