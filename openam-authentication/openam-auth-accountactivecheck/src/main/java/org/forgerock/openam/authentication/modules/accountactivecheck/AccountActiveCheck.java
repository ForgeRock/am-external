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
 * Copyright 2020-2023 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.accountactivecheck;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.spi.AuthenticationException;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * Authentication module that checks whether the username being authenticated is active and unlocked.
 * Can be used in an authentication chain once the username is identified, e.g. after DataStore/LDAP module.
 * Provides a means to perform account lock checking earlier than at the end of the chain.
 * On discovering that the user account is inactive or locked, InvalidPasswordException is thrown.
 */
public class AccountActiveCheck extends AMLoginModule {
    private static final String AM_AUTH = "amAuth";

    private Map sharedState;
    
    private final Logger debug = LoggerFactory.getLogger(AccountActiveCheck.class);
    protected Principal userPrincipal;

    /**
     * Initialize this authentication module.
     * @param subject {@inheritDoc}
     * @param sharedState {@inheritDoc}
     * @param options {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.sharedState = sharedState;
    }

    /**
     * Perform the processing required for this authentication module.
     * Verify that a userName is present and then perform an account active and locked check.
     * @param callbacks {@inheritDoc}
     * @param state {@inheritDoc}
     * @throws AuthLoginException if the user account is not active or locked.
     */
    @Override
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {

        String userName = (String) sharedState.get(getUserKey());
        if (StringUtils.isEmpty(userName)) {
            return ISAuthConstants.LOGIN_IGNORE;
        }

        try {
            if (!isAccountActive(userName)) {
                setFailureID(userName);
                throw new InvalidPasswordException(AM_AUTH, AMAuthErrorCode.AUTH_USER_INACTIVE, null, userName, 
                        false, null);
            }

            if (isAccountLocked(userName)) {
                setFailureID(userName);
                throw new InvalidPasswordException(AM_AUTH, AMAuthErrorCode.AUTH_USER_LOCKED, null, userName, 
                        false, null);
            }
        } catch (AuthenticationException ex) {
            debug.debug("AuthenticationException: {}", ex.getMessage());
            setFailureID(userName);
            throw new InvalidPasswordException(AM_AUTH, "invalidPasswd", null);
        }
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        }
        String userName = (String) sharedState.get(getUserKey());
        if (userName != null) {
            userPrincipal = new AccountActiveCheckPrincipal(userName);
            return userPrincipal;
        } else {
            return null;
        }
    }
    
    // cleanup state fields
    public void destroyModuleState() {
        userPrincipal = null;
    }
    
    public void nullifyUsedVars() {
        sharedState = null ;
    }
    
}
