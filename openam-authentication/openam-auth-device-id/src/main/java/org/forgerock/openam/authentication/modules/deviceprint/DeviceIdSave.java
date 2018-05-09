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
 * Copyright 2014-2017 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.modules.scripted.ScriptedPrinciple;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * AM Login Module which presents the user with a UI to choose whether to save the device print profile of the device
 * they are using to authenticate with.
 *
 * @since 12.0.0
 */
public class DeviceIdSave extends AMLoginModule {

    private static final String AUTO_STORE_PROFILES_KEY = "iplanet-am-auth-device-id-save-auto-store-profile";
    private static final String MAX_PROFILES_ALLOWED_KEY = "iplanet-am-auth-device-id-save-max-profiles-allowed";
    private static final String DEVICE_PRINT_PROFILE_KEY = "devicePrintProfile";
    static final int SAVE_PROFILE_STATE = 2;
    static final int NAME_PROFILE_STATE = 3;

    private static final String DEBUG_NAME = "amAuthDeviceIdSave";
    private static final Debug DEBUG = Debug.getInstance(DEBUG_NAME);

    private String userName;
    private PersistModuleProcessor processor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map config) {
        int maxProfilesAllowed = Integer.parseInt(CollectionHelper.getMapAttr(config, MAX_PROFILES_ALLOWED_KEY));
        ProfilePersisterFactory profilePersisterFactory = InjectorHolder.getInstance(ProfilePersisterFactory.class);
        userName = (String) sharedState.get(getUserKey());
        try {
            Map<String, Object> devicePrintProfile =
                    JsonValueBuilder.getObjectMapper()
                            .readValue((String) sharedState.get(DEVICE_PRINT_PROFILE_KEY), Map.class);
            boolean autoStoreProfiles = Boolean.parseBoolean(CollectionHelper.getMapAttr(config, AUTO_STORE_PROFILES_KEY));
            ProfilePersister profilePersister = profilePersisterFactory.create(maxProfilesAllowed, userName,
                    DNMapper.orgNameToRealmName(getRequestOrg()));
            processor = new PersistModuleProcessor(devicePrintProfile, autoStoreProfiles, profilePersister);
        } catch (IOException e) {
            DEBUG.error("DeviceIdSave.init : Module exception : ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        return processor.process(callbacks, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        return new ScriptedPrinciple(userName);
    }
}
