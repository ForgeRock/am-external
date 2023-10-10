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
 * Copyright 2014-2023 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.modules.scripted.ScriptedPrinciple;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.DNMapper;

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
    private static final String CLAZZ = DeviceIdSave.class.getSimpleName();
    static final int SAVE_PROFILE_STATE = 2;
    static final int NAME_PROFILE_STATE = 3;

    private static final Logger DEBUG = LoggerFactory.getLogger(DeviceIdSave.class);

    private static final IdentityStoreFactory identityStoreFactory =
            InjectorHolder.getInstance(IdentityStoreFactory.class);

    private String userName;
    private PersistModuleProcessor processor;
    private String realm;
    private AMIdentity amIdentityPrincipal;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map config) {
        final String methodName = CLAZZ + ".init";
        int maxProfilesAllowed = Integer.parseInt(CollectionHelper.getMapAttr(config, MAX_PROFILES_ALLOWED_KEY));
        ProfilePersisterFactory profilePersisterFactory = InjectorHolder.getInstance(ProfilePersisterFactory.class);
        userName = (String) sharedState.get(getUserKey());
        if (userName == null){
            DEBUG.warn("{} - userName not present - module will fail", methodName);
            return;
        }
        this.realm = DNMapper.orgNameToRealmName(getRequestOrg());
        IdentityStore identityStore = identityStoreFactory.create(realm);
        amIdentityPrincipal = identityStore.getUserUsingAuthenticationUserAliases(userName);
        String principalUserName = null;
        if (amIdentityPrincipal == null || amIdentityPrincipal.getName() == null) {
            DEBUG.error("{} - unable to find identity for user name: {}", methodName, userName);
            return;
        } else {
            principalUserName = amIdentityPrincipal.getName();
        }
        try {
            Map<String, Object> devicePrintProfile = JsonValueBuilder.getObjectMapper()
                    .readValue((String) sharedState.get(DEVICE_PRINT_PROFILE_KEY), Map.class);
            boolean autoStoreProfiles = Boolean
                    .parseBoolean(CollectionHelper.getMapAttr(config, AUTO_STORE_PROFILES_KEY));
            ProfilePersister profilePersister = profilePersisterFactory
                    .create(maxProfilesAllowed, principalUserName, realm);
            processor = new PersistModuleProcessor(devicePrintProfile, autoStoreProfiles, profilePersister);
            } catch (IOException e) {
                DEBUG.error(methodName + " - Module exception : ", e);
            }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        if (null != processor) {
            return processor.process(callbacks, state);
        } else {
            throw new LoginException("Could not complete module - profilePersister required");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        if (amIdentityPrincipal != null && amIdentityPrincipal.getName() != null) {
            return new ScriptedPrinciple(amIdentityPrincipal.getName());
        }
        return null;
    }
}
