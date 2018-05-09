/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: WebServiceAuthenticatorImpl.java,v 1.4 2008/08/06 17:29:25 exu Exp $
 *
 * Portions Copyrighted 2015-2017 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.soapbinding;

import java.security.AccessController;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.iplanet.am.util.Cache;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

class WebServiceAuthenticatorImpl implements WebServiceAuthenticator {
    private static final String PRINCIPAL_PROP = "Principal";
    private static final String PRINCIPALS_PROP = "Principals";
    private static final String AUTH_TYPE_PROP = "AuthType";
    private static final String AUTH_INSTANT_PROP = "authInstant";
    private static final String ANONYMOUS_PRINCIPAL = "anonymous";
    private static final String SESSION_SERVICE_NAME =
            "iPlanetAMSessionService";
    private static final String MAX_SESSION_TIME =
            "iplanet-am-session-max-session-time";
    private static final String IDLE_TIME =
            "iplanet-am-session-max-idle-time";
    private static final String CACHE_TIME =
            "iplanet-am-session-max-caching-time";
    private static final int DEFAULT_MAX_SESSION_TIME = 120;
    private static final int DEFAULT_IDLE_TIME = 30;
    private static final int DEFAULT_CACHE_TIME = 3;
    private static Cache ssoTokenCache = new Cache(1000);
    private static SSOTokenManager ssoTokenManager = null;
    private static ServiceSchema sessionSchema = null;
    private static String rootSuffix =
            SystemPropertiesManager.get("com.iplanet.am.rootsuffix");
    private static Debug debug = Debug.getInstance("libIDWSF");
    
    static {
        try {
            ssoTokenManager = SSOTokenManager.getInstance();
        } catch (Exception ex) {
            debug.error("WebServiceAuthenticatorImpl.static: " +
                "unable to get SSOTokenManager", ex);
        }
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            ServiceSchemaManager scm = new ServiceSchemaManager(
                    SESSION_SERVICE_NAME, adminToken);
            sessionSchema = scm.getDynamicSchema();
        } catch (Exception ex) {
            debug.error("WebServiceAuthenticatorImpl.static: " +
                "unable to get session schema", ex);
        }
    }
    
    /**
     * Authenticates a web service using its certificates.
     *
     * @param message a Message object that needs authentication.
     * @param request the HttpServletRequest object that comes from the web
     *                service
     * @return a SSOToken Object for the valid certificates after
     *         successful authentication or null if authentication fails.
     * @deprecated ID-WSF is no longer supported.
     */
    @Deprecated
    public Object authenticate(Message message,Subject subject,Map state,
            HttpServletRequest request) {
        throw new UnsupportedOperationException("ID-WSF is no longer supported");
    }
}
