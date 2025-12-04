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
 * Copyright 2013-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.common;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * The idea behind this interface is that implementations of this interface will also extend the AMLoginModule
 * class and be instantiated with an instance of the AuthLoginModule interface and the implementation of this
 * interface will then delegate the AMLoginModule calls to the AuthLoginModule instance.
 *
 * This allows the implementation of the AuthLoginModule to be unit testable as the AMLoginModule instance is abstracted
 * away.
 *
 * @see AbstractLoginModuleBinder
 */
public interface AMLoginModuleBinder {

    /**
     * Returns the CallbackHandler object for the module.
     *
     * @return CallbackHandler for this request, returns null if the CallbackHandler object could not be obtained.
     */
    CallbackHandler getCallbackHandler();

    /**
     * Returns the HttpServletRequest object that initiated the call to this module.
     *
     * @return HttpServletRequest for this request, returns null if the HttpServletRequest object could not be obtained.
     */
    HttpServletRequest getHttpServletRequest();

    /**
     * Returns the HttpServletResponse object for the servlet request that initiated the call to this module. The
     * servlet response object will be the response to the HttpServletRequest received by the authentication module.
     *
     * @return HttpServletResponse for this request, returns null if the HttpServletResponse object could not be
     * obtained.
     */
    HttpServletResponse getHttpServletResponse();

    /**
     * Returns the organization DN for this authentication session.
     *
     * @return The organization DN.
     */
    String getRequestOrg();

    /**
     * Sets a property in the user session. If the session is being force upgraded then set on the old session
     * otherwise set on the current session.
     *
     * @param name The property name.
     * @param value The property value.
     * @throws AuthLoginException If the user session is invalid.
     */
    void setUserSessionProperty(String name, String value) throws AuthLoginException;

    /**
     * Returns the username of the currently authenticating user, if known. This can be set by authentication modules
     * using the {@link #setAuthenticatingUserName(String)} method to communicate the username with subsequent
     * modules in the authentication chain.
     * <p>
     * Note that the username returned here is based on user input, and it may not correspond to the user's actual
     * username determined by the data store.
     *
     * @return the name of the user currently authenticating, if known, or {@code null} if not supplied.
     * @see ISAuthConstants#SHARED_STATE_USERNAME
     */
    String getAuthenticatingUserName();

    /**
     * Sets the username of the user that is currently authenticating as determined by the current login module.
     *
     * @param username the name of the currently authenticating user.
     * @see AMLoginModule#storeUsername(String)
     */
    void setAuthenticatingUserName(String username);

    /**
     * Returns a Callback array for a specific state.
     * <p>
     * This method can be used to retrieve Callback[] for any state. All previous submitted Callback[] information are
     * kept until the login process is completed.
     *
     * @param state
     *         The state for which the callback to be returned.
     *
     * @return Callback array for this state, return 0-length Callback array if there is no Callback defined for this
     * state.
     *
     * @throws AuthLoginException
     *         if unable to read the callbacks.
     * @see AMLoginModule#getCallback(int)
     */
    Callback[] getCallback(int state) throws AuthLoginException;

    /**
     * Replace Callback object for a specific state.
     *
     * @param state
     *         The order of login state.
     * @param index
     *         The index of Callback in the Callback array to be replaced for the specified state. Here index starts
     *         with 0, i.e. 0 means the first Callback in the Callback[], 1 means the second callback.
     * @param callback
     *         The Callback instance to be replaced
     *
     * @throws AuthLoginException
     *         if state or index is out of bound, or callback instance is null.
     */
    void replaceCallback(int state, int index, Callback callback) throws AuthLoginException;

    /**
     * Stores user name and password into shared state map.
     * This method should be called after successful authentication by each individual module
     * if both a username and a password were supplied in that module.
     *
     * @param username
     *         user name.
     * @param password
     *         user password.
     */
    void storeUsernamePasswd(String username, String password);

    /**
     * Returns the Login <code>Locale</code> for this session
     * @return <code>Locale</code> used for localizing text
     */
    Locale getLoginLocale();

    /**
     * Use this method to replace the header text from the XML file with new
     * text. This method can be used multiple times on the same state replacing
     * text with new text each time. Useful for modules that control their own
     * error handling.
     *
     * @param state state state in which the Callback[] to be reset
     * @param header The text of the header to be replaced
     * @throws AuthLoginException if state is out of bounds
     */
    void substituteHeader(int state, String header) throws AuthLoginException;
}
