/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.amster;

import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_START;

import java.util.Map;

import javax.security.auth.Subject;

import org.forgerock.openam.authentication.modules.common.AbstractLoginModuleBinder;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * The Amster auth module - delegates to {@link AmsterAuthLoginModule} via the {@link AbstractLoginModuleBinder}.
 */
public class Amster extends AbstractLoginModuleBinder<AmsterAuthLoginModule> {

    /**
     * Construct the {@link AbstractLoginModuleBinder} with the
     * {@link org.forgerock.openam.authentication.modules.common.AuthLoginModule} delegate.
     */
    public Amster() {
        super(new AmsterAuthLoginModule());
    }

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        super.init(subject, sharedState, options);
        HiddenValueCallback callback = new HiddenValueCallback("jwt", authLoginModule.getNonce());
        try {
            forceCallbacksInit();
            replaceCallback(LOGIN_START, 0, callback);
        } catch (AuthLoginException e) {
            throw new IllegalArgumentException("State, index or callback was invalid", e);
        }
    }
}
