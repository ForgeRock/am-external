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
