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
 * Copyright 2013-2020 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.AUTH_RESOURCE_BUNDLE_NAME;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.COOKIE_DOMAINS_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.COOKIE_NAME_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.ENFORCE_CLIENT_IP_SETTING_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.HTTP_ONLY_COOKIE_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.INSTANCE_NAME_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.OPENAM_CLIENT_IP_CLAIM_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.OPENAM_REALM_CLAIM_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.OPENAM_USER_CLAIM_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.SECURE_COOKIE_KEY;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.authentication.modules.common.JaspiAuthLoginModule;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * Authentication logic for persistent cookie authentication in OpenAM. Making use of the JASPI JwtSessionModule
 * to create and verify the persistent cookie.
 */
public class PersistentCookieAuthModule extends JaspiAuthLoginModule {

    private static final Logger DEBUG = LoggerFactory.getLogger(PersistentCookieAuthModule.class);
    private static final int MINUTES_IN_HOUR = 60;

    private static final String COOKIE_IDLE_TIMEOUT_SETTING_KEY = "openam-auth-persistent-cookie-idle-time";
    private static final String COOKIE_MAX_LIFE_SETTING_KEY = "openam-auth-persistent-cookie-max-life";

    private final CoreWrapper coreWrapper;

    private Integer tokenIdleTime;
    private Integer maxTokenLife;
    private boolean enforceClientIP;
    private boolean secureCookie;
    private boolean httpOnlyCookie;
    private String cookieName;
    private Collection<String> cookieDomains;
    private String instanceName;

    private Principal principal;

    private final PersistentCookieModuleWrapper persistentCookieModuleWrapper;

    /**
     * Constructs an instance of the PersistentCookieAuthModule.
     *
     * Used by the PersistentCookie in a server deployment environment.
     */
    public PersistentCookieAuthModule() {
        this(new CoreWrapper(), new PersistentCookieModuleWrapper());
    }

    /**
     * Constructs an instance of the PersistentCookieAuthModule.
     *
     * Used in a unit test environment.
     *
     * @param coreWrapper An instance of the CoreWrapper.
     * @param persistentCookieModuleWrapper An instance of the wrapper for Persistent Cookie.
     */
    public PersistentCookieAuthModule(CoreWrapper coreWrapper, PersistentCookieModuleWrapper persistentCookieModuleWrapper) {
        super(AUTH_RESOURCE_BUNDLE_NAME, persistentCookieModuleWrapper);
        this.coreWrapper = coreWrapper;
        this.persistentCookieModuleWrapper = persistentCookieModuleWrapper;
    }

    /**
     * Initialises the JwtSessionModule for use by the AM Login Module.
     *
     * @param subject {@inheritDoc}
     * @param sharedState {@inheritDoc}
     * @param options {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException if the configured realm doesn't exist.
     */
    @Override
    protected Map<String, Object> generateConfig(Subject subject, Map sharedState, Map options) throws AuthException {
        String idleTimeString = CollectionHelper.getMapAttr(options, COOKIE_IDLE_TIMEOUT_SETTING_KEY);
        String maxLifeString = CollectionHelper.getMapAttr(options, COOKIE_MAX_LIFE_SETTING_KEY);
        if (StringUtils.isEmpty(idleTimeString)) {
            DEBUG.warn("Cookie Idle Timeout not set. Defaulting to 0");
            idleTimeString = "0";
        }
        if (StringUtils.isEmpty(maxLifeString)) {
            DEBUG.warn("Cookie Max Life not set. Defaulting to 0");
            maxLifeString = "0";
        }
        tokenIdleTime = Integer.parseInt(idleTimeString) * MINUTES_IN_HOUR;
        maxTokenLife = Integer.parseInt(maxLifeString) * MINUTES_IN_HOUR;
        enforceClientIP = CollectionHelper.getBooleanMapAttr(options, ENFORCE_CLIENT_IP_SETTING_KEY, false);
        secureCookie = CollectionHelper.getBooleanMapAttr(options, SECURE_COOKIE_KEY, true);
        httpOnlyCookie = CollectionHelper.getBooleanMapAttr(options, HTTP_ONLY_COOKIE_KEY, true);
        cookieName = CollectionHelper.getMapAttr(options, COOKIE_NAME_KEY);
        cookieDomains = coreWrapper.getCookieDomainsForRequest(getHttpServletRequest());
        instanceName = (String) options.get(ISAuthConstants.MODULE_INSTANCE_NAME);

        try {
            return persistentCookieModuleWrapper.generateConfig(tokenIdleTime.toString(), maxTokenLife.toString(),
                    enforceClientIP, getRequestOrg(), secureCookie, httpOnlyCookie, cookieName, cookieDomains,
                    instanceName);
        } catch (RealmLookupException e) {
            throw new AuthException(e.getLocalizedMessage());
        }
    }

    /**
     * Overridden as to call different method on underlying JASPI JwtSessionModule.
     *
     * @param callbacks {@inheritDoc}
     * @param state {@inheritDoc}
     * @return {@inheritDoc}
     * @throws LoginException {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {

        switch (state) {
        case ISAuthConstants.LOGIN_START: {
            setUserSessionProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, tokenIdleTime.toString());
            setUserSessionProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, maxTokenLife.toString());
            setUserSessionProperty(ENFORCE_CLIENT_IP_SETTING_KEY, Boolean.toString(enforceClientIP));
            setUserSessionProperty(SECURE_COOKIE_KEY, Boolean.toString(secureCookie));
            setUserSessionProperty(HTTP_ONLY_COOKIE_KEY, Boolean.toString(httpOnlyCookie));
            if (cookieName != null) {
                setUserSessionProperty(COOKIE_NAME_KEY, cookieName);
            }
            setUserSessionProperty(INSTANCE_NAME_KEY, instanceName);
            String cookieDomainsString = "";
            for (String cookieDomain : cookieDomains) {
                if (StringUtils.isNotEmpty(cookieDomain)) {
                    cookieDomainsString += cookieDomain + ",";
                }
            }
            setUserSessionProperty(COOKIE_DOMAINS_KEY, cookieDomainsString);
            final Subject clientSubject = new Subject();
            MessageInfo messageInfo = persistentCookieModuleWrapper.prepareMessageInfo(getHttpServletRequest(),
                    getHttpServletResponse());
            if (process(messageInfo, clientSubject, callbacks)) {
                if (principal != null) {
                    setAuthenticatingUserName(principal.getName());
                }
                return ISAuthConstants.LOGIN_SUCCEED;
            }
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "cookieNotValid", null);
        }
        default: {
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "incorrectState", null);
        }
        }
    }

    /**
     * If Jwt is invalid then throws LoginException, otherwise Jwt is valid and the realm is check to ensure
     * the user is authenticating in the same realm.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param callbacks {@inheritDoc}
     * @return {@inheritDoc}
     * @throws LoginException {@inheritDoc}
     */
    @Override
    protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks)
            throws LoginException {

        final Jwt jwt = persistentCookieModuleWrapper.validateJwtSessionCookie(messageInfo);

        if (jwt == null) {
            //BAD
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "cookieNotValid", null);
        } else {
            //GOOD

            final Map<String, Object> claimsSetContext =
                    jwt.getClaimsSet().getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class);
            if (claimsSetContext == null) {
                throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "jaspiContextNotFound", null);
            }

            // Need to check realm
            final String jwtRealm = (String) claimsSetContext.get(OPENAM_REALM_CLAIM_KEY);
            if (!getRequestOrg().equals(jwtRealm)) {
                throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedDiffRealm", null);
            }

            final String storedClientIP = (String) claimsSetContext.get(OPENAM_CLIENT_IP_CLAIM_KEY);
            if (enforceClientIP) {
                enforceClientIP(storedClientIP);
            }

            // Need to get user from jwt to use in Principal
            final String username = (String) claimsSetContext.get(OPENAM_USER_CLAIM_KEY);
            principal = new Principal() {
                public String getName() {
                    return username;
                }
            };

            setUserSessionProperty(JwtSessionModule.JWT_VALIDATED_KEY, Boolean.TRUE.toString());

            return true;
        }
    }

    /**
     * Enforces that the client IP that the request originated from matches the stored client IP that the
     * persistent cookie was issued to.
     *
     * @param storedClientIP The stored client IP.
     * @throws AuthLoginException If the client IP on the request does not match the stored client IP.
     */
    private void enforceClientIP(final String storedClientIP) throws AuthLoginException {
        final String clientIP = ClientUtils.getClientIPAddress(getHttpServletRequest());
        if (storedClientIP == null || storedClientIP.isEmpty()) {
            DEBUG.debug("Client IP not stored when persistent cookie was issued.");
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedClientIPDifferent", null);
        } else if (clientIP == null || clientIP.isEmpty()) {
            DEBUG.debug("Client IP could not be retrieved from request.");
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedClientIPDifferent", null);
        } else if (!storedClientIP.equals(clientIP)) {
            DEBUG.debug("Client IP not the same, original: " + storedClientIP + ", request: " + clientIP);
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedClientIPDifferent", null);
        }
        // client IP is valid
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        return principal;
    }
}
