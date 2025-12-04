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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import java.util.Set;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.DeviceService;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.TwoFactorEncryptedDeviceStorage;

import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.SMSException;

/**
 * Set of utility methods which are shared amongst second factor node implementations.
 *
 * @param <T>
 * {@see org.forgerock.openam.core.rest.authn.mobile.TwoFactorAMLoginModule}
 */
public final class MultiFactorNodeDelegate<T extends TwoFactorEncryptedDeviceStorage & DeviceService> {

    private final AuthenticatorDeviceServiceFactory<T> deviceServiceFactory;

    /**
     * The constructor of this utility class.
     *
     * @param deviceServiceFactory the device factory service.
     */
    @Inject
    public MultiFactorNodeDelegate(AuthenticatorDeviceServiceFactory<T> deviceServiceFactory) {
        this.deviceServiceFactory = deviceServiceFactory;
    }

    /**
     * Determines if the current device should be skipped.
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     * @return the user's skippable attribute.
     * @throws NodeProcessException If anything goes wrong.
     */
    public SkipSetting shouldSkip(AMIdentity amIdentity, String realm) throws NodeProcessException {
        return userConfiguredSkippable(amIdentity, getDeviceStorage(realm));
    }

    /**
     * Sets user's skippable attribute.
     *
     * @param identity the identity of the user.
     * @param realm the realm.
     * @param skippable the user's skippable attribute
     * @throws NodeProcessException if not able to update the user profile with the skippable setting.
     */
    public void setUserSkip(AMIdentity identity, String realm, SkipSetting skippable) throws NodeProcessException {
        try {
            T deviceService = getDeviceStorage(realm);
            deviceService.setUserSkip(identity, skippable);
        } catch (IdRepoException | SSOException e) {
            throw new NodeProcessException("Unable to set skippable value on user.", e);
        }
    }

    /**
     * Returns whether or not it is necessary to run this module through to completion.
     *
     * @param identity Identity of the user attempting to login.
     * @param realmService the service used to verify the user's skippable attribute.
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
            return SkipSetting.getSettingFor(Integer.parseInt(tmp));
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
