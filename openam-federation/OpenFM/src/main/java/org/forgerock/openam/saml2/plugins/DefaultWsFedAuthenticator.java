/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.saml2.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;

import org.forgerock.openam.wsfederation.common.ActiveRequestorException;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * The default {@link WsFedAuthenticator} implementation that just authenticates using the default authentication chain
 * in the selected realm.
 */
public class DefaultWsFedAuthenticator implements WsFedAuthenticator {

    private static final Debug DEBUG = Debug.getInstance("libWSFederation");

    @Override
    public SSOToken authenticate(HttpServletRequest request, HttpServletResponse response, SOAPMessage soapMessage,
            String realm, String username, char[] password) throws ActiveRequestorException {
        ActiveRequestorException exception = null;
        try {
            AuthContext authContext = new AuthContext(realm);
            authContext.login(request, response);

            while (authContext.hasMoreRequirements()) {
                Callback[] callbacks = authContext.getRequirements();
                if (callbacks == null || callbacks.length == 0) {
                    continue;
                }
                List<Callback> missing = new ArrayList<>();
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callback;
                        nc.setName(username);
                    } else if (callback instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callback;
                        pc.setPassword(password);
                    } else {
                        missing.add(callback);
                    }
                }

                if (missing.size() > 0) {
                    throw newSenderException();
                }
                authContext.submitRequirements(callbacks);
            }

            if (AuthContext.Status.SUCCESS.equals(authContext.getStatus())) {
                return authContext.getSSOToken();
            } else if (AuthContext.Status.FAILED.equals(authContext.getStatus())) {
                exception = newSenderException().setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                // Server-side error
                exception = newReceiverException();
            }
        } catch (AuthLoginException ale) {
            DEBUG.error("An error occurred while trying to authenticate the end-user", ale);
        } catch (L10NMessageImpl l10nm) {
            DEBUG.error("An error occurred while trying to obtain the session ID during authentication", l10nm);
        }

        throw (exception != null)? exception : newSenderException();
    }

    private ActiveRequestorException newSenderException() {
        return ActiveRequestorException.newSenderException("unableToAuthenticate");
    }

    private ActiveRequestorException newReceiverException() {
        return ActiveRequestorException.newReceiverException("unableToAuthenticate");
    }
}