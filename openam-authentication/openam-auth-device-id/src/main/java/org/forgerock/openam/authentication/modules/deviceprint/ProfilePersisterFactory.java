/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao;

/**
 * Generates a profile persister for for the provided realm/username.
 */
@Singleton
public class ProfilePersisterFactory {

    private final DeviceIdDao devicesDao;

    /**
     * Create a new ProfilePersisterFactory.
     *
     * @param devicesDao DeviceIdDao used to write data.
     */
    @Inject
    public ProfilePersisterFactory(DeviceIdDao devicesDao) {
        this.devicesDao = devicesDao;
    }

    /**
     * Generate a new ProfilePersister for the supplied username and realm with the provided maximum number of devices.
     *
     * @param max Max number of devices this user should have persisted.
     * @param username Username of the user.
     * @param realm Realm in which the user is operating.
     * @return a new ProfilePersister.
     */
    public ProfilePersister create(int max, String username, String realm) {
        return new ProfilePersister(max, username, realm, devicesDao);
    }

}
