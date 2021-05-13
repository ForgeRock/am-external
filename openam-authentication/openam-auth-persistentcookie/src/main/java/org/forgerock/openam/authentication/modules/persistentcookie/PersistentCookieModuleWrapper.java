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

import static org.forgerock.util.Reject.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.message.MessageInfo;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.util.Uris;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.authentication.modules.common.JaspiAuthModuleWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Wrapper class for providing Persistent Cookie functionality. Uses the Jaspi ServletJwtSessionModule for this.
 * Provides wrappers over the modules functionality, and hides the underlying code from external users.
 */
public class PersistentCookieModuleWrapper extends JaspiAuthModuleWrapper<ServletJwtSessionModule> {

    public static final String AUTH_RESOURCE_BUNDLE_NAME = "amAuthPersistentCookie";
    public static final String SSO_TOKEN_ORGANIZATION_PROPERTY_KEY = "Organization";
    public static final String ENFORCE_CLIENT_IP_SETTING_KEY = "openam-auth-persistent-cookie-enforce-ip";
    public static final String SECURE_COOKIE_KEY = "openam-auth-persistent-cookie-secure-cookie";
    public static final String HTTP_ONLY_COOKIE_KEY = "openam-auth-persistent-cookie-http-only-cookie";
    public static final String COOKIE_NAME_KEY = "openam-auth-persistent-cookie-name";
    public static final String COOKIE_DOMAINS_KEY = "openam-auth-persistent-cookie-domains";
    public static final String INSTANCE_NAME_KEY = "openam-auth-persistent-cookie-instance-name";

    public static final String OPENAM_USER_CLAIM_KEY = "openam.usr";
    public static final String OPENAM_AUTH_TYPE_CLAIM_KEY = "openam.aty";
    public static final String OPENAM_REALM_CLAIM_KEY = "openam.rlm";
    public static final String OPENAM_CLIENT_IP_CLAIM_KEY = "openam.clientip";

    private final RealmLookup realmLookup;

    /**
     * Creates the wrapper with default values for the dependencies.
     */
    public PersistentCookieModuleWrapper() {
        this(new ServletJwtSessionModule(new SecretsApiJwtCryptographyHandler()),
                InjectorHolder.getInstance(RealmLookup.class));
    }

    /**
     * Creates the wrapper with selectable dependencies for testing.
     * @param module The underlying module.
     */
    @VisibleForTesting
    protected PersistentCookieModuleWrapper(ServletJwtSessionModule module, RealmLookup realmLookup) {
        super(module);
        this.realmLookup = checkNotNull(realmLookup, "realm lookup");
    }

    /**
     * Creates a Map of configuration information required to configure the JwtSessionModule.
     *
     * @param tokenIdleTime   The number of seconds the JWT can be not used for before becoming invalid.
     * @param maxTokenLife    The number of seconds the JWT can be used for before becoming invalid.
     * @param enforceClientIP The enforcement client IP.
     * @param realm           The realm for the persistent cookie.
     * @param secureCookie    {@code true} if the persistent cookie should be set as secure.
     * @param httpOnlyCookie  {@code true} if the persistent cookie should be set as http only.
     * @param instanceName The name of this persistent cookie auth module instance.
     * @return A Map containing the configuration information for the JWTSessionModule.
     * @throws RealmLookupException if the realm doesn't exist.
     */
    public Map<String, Object> generateConfig(final String tokenIdleTime, final String maxTokenLife,
            final boolean enforceClientIP, final String realm, boolean secureCookie, boolean httpOnlyCookie,
            String cookieName, Collection<String> cookieDomains, String instanceName) throws RealmLookupException {
        Map<String, Object> config = new HashMap<>();
        config.put(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, tokenIdleTime);
        config.put(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, maxTokenLife);
        config.put(JwtSessionModule.SECURE_COOKIE_KEY, secureCookie);
        config.put(JwtSessionModule.HTTP_ONLY_COOKIE_KEY, httpOnlyCookie);
        config.put(ENFORCE_CLIENT_IP_SETTING_KEY, enforceClientIP);
        config.put(JwtSessionModule.SESSION_COOKIE_NAME_KEY, cookieName);
        config.put(JwtSessionModule.COOKIE_DOMAINS_KEY, cookieDomains);
        config.put(INSTANCE_NAME_KEY, instanceName);
        config.put(SSO_TOKEN_ORGANIZATION_PROPERTY_KEY, realm);
        config.put(JwtSessionModule.JWT_ISSUER, issuer(realm, instanceName));
        return config;
    }

    /**
     * Validates if the Jwt Session Cookie is valid and the idle timeout or max life has expired. Calls into the
     * underlying Jaspi module.
     *
     * @param messageInfo The MessageInfo instance.
     * @return The Jwt if successfully validated otherwise null.
     */
    public Jwt validateJwtSessionCookie(MessageInfo messageInfo) {
        return getServerAuthModule().validateJwtSessionCookie(messageInfo);
    }

    /**
     * Ensures the context map exists within the messageInfo object, and then returns the context map to be used.
     * Calls into the underlying Jaspi module.
     *
     * @param messageInfo The MessageInfo instance.
     * @return The context map internal to the messageInfo's map.
     */
    public Map<String, Object> getContextMap(MessageInfo messageInfo) {
        return getServerAuthModule().getContextMap(messageInfo);
    }

    /**
     * Provides a way to delete the Jwt Session Cookie, by setting a new cookie with the same name, null value and
     * max age 0.
     * Calls into the underlying Jaspi module.
     *
     * @param messageInfo The {@code MessageInfo} which contains the Response with the Jwt Session Cookie.
     */
    public void deleteSessionJwtCookie(MessageInfo messageInfo) {
        getServerAuthModule().deleteSessionJwtCookie(messageInfo);
    }

    private String issuer(String realmName, String instanceName) throws RealmLookupException {
        Realm realmObj = realmLookup.lookup(realmName);
        return realmObj.asRoutingPath() + "/modules/" + Uris.urlEncodePathElement(instanceName);
    }
}
