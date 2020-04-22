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
 * Copyright 2014-2019 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import javax.inject.Inject;
import java.net.URL;
import java.security.AccessController;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.oauth.resolvers.OpenIdResolverFactory;
import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;

/**
 * @see org.forgerock.openam.authentication.modules.oidc.OpenIdResolverCache
 */
public class OpenIdResolverCacheImpl implements OpenIdResolverCache {

    private static Debug logger = Debug.getInstance("amAuth");
    private final OpenIdResolverFactory openIdResolverFactory;
    private final ConcurrentMap<String, ConcurrentMap<String, OpenIdResolver>> resolverMap;

    @Inject
    OpenIdResolverCacheImpl(OpenIdResolverFactory openIdResolverFactory) {
        this.openIdResolverFactory = openIdResolverFactory;
        resolverMap = new ConcurrentHashMap<>();
        addServiceListener();
    }

    private void addServiceListener() {
        try {
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token,
                    "sunAMAuthOAuthService", "1.0");
            if (serviceConfigManager.addListener(new OpenIDResolveCacheChangeListener()) == null) {
                logger.error("Could not add listener to ServiceConfigManager instance. OpenID Authentication Module " +
                        "changes will not be dynamically updated.");
            }
        } catch (Exception e) {
            String message = "OpenIDResolverCacheImpl::Unable to construct ServiceConfigManager: " + e;
            logger.error(message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public OpenIdResolver getResolverForIssuer(String issuer, String cryptoContextDefinitionValue) {
        ConcurrentMap<String, OpenIdResolver> issuerMap = resolverMap.get(issuer);
        if (issuerMap == null) {
            return null;
        }
        return issuerMap.get(cryptoContextDefinitionValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenIdResolver createResolver(String issuerFromJwk, String cryptoContextType, String cryptoContextValue,
                                         URL cryptoContextValueUrl) throws FailedToLoadJWKException {

        ConcurrentMap<String, OpenIdResolver> issuerMap = resolverMap.get(issuerFromJwk);
        if (issuerMap == null) {
            issuerMap = new ConcurrentHashMap<>();
            ConcurrentMap<String, OpenIdResolver> existingIssuerMap = resolverMap.putIfAbsent(issuerFromJwk, issuerMap);
            if (existingIssuerMap != null) {
                issuerMap = existingIssuerMap;
            }
        }

        OpenIdResolver existingResolver = issuerMap.get(cryptoContextValue);
        if (existingResolver != null) {
            return existingResolver;
        }

        OpenIdResolver newResolver =
                createNewResolver(issuerFromJwk, cryptoContextType, cryptoContextValue, cryptoContextValueUrl);
        existingResolver = issuerMap.putIfAbsent(cryptoContextValue, newResolver);
        if (existingResolver != null) {
            return existingResolver;
        } else {
            return newResolver;
        }
    }


    /**
     * Create a new resolver.
     * @param issuerFromJwk Issuer.
     * @param cryptoContextType The crypto context value type.
     * @param cryptoContextValue The crypto context value.
     * @param cryptoContextValueUrl The crypto context url.
     * @return OpenIdResolver.
     * @throws FailedToLoadJWKException
     */
    private OpenIdResolver createNewResolver(String issuerFromJwk, String cryptoContextType, String cryptoContextValue,
                                         URL cryptoContextValueUrl) throws FailedToLoadJWKException {
        if (OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(cryptoContextType)) {
            return openIdResolverFactory.createSharedSecretResolver(issuerFromJwk, cryptoContextValue);

        } else if (OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType)) {
            OpenIdResolver newResolver = openIdResolverFactory.createFromOpenIDConfigUrl(cryptoContextValueUrl);
            //check is only relevant in this block, as issuer is specified in the json blob referenced by url.
            if (!issuerFromJwk.equals(newResolver.getIssuer())) {
                throw new IllegalStateException("The specified issuer, " + issuerFromJwk + ", does not match the issuer, "
                        + newResolver.getIssuer() + " referenced by the configuration url, " + cryptoContextValue);
            }
            return newResolver;

        } else if (OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType)) {
            return openIdResolverFactory.createJWKResolver(issuerFromJwk, cryptoContextValueUrl,
                    HttpURLConnectionManager.getReadTimeout(), HttpURLConnectionManager.getConnectTimeout());

        } else {
            /*
            Should not enter this block, as the cryptoContextType was validated to be of the three expected types in
            OpenIdModule.init, but all bases should be covered. This exception is not caught by the OpenIdConnect caller.
             */
            throw new IllegalArgumentException("The specified cryptoContextType, " + cryptoContextType + " was unexpected!");
        }

    }

    /**
     * ServiceListener implementation to clear cache when it changes.
     */
    private final class OpenIDResolveCacheChangeListener implements ServiceListener {

        public void schemaChanged(String serviceName, String version) {
            logger.warning("The schemaChanged ServiceListener method was invoked for service " + serviceName
                    + ". This is unexpected.");
        }

        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                int type) {
            logger.warning("The globalConfigChanged ServiceListener method was invoked for service " + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
            if (logger.messageEnabled()) {
                logger.message("Clearing OpenId Resolver Cache.");
            }
            resolverMap.clear();
        }
    }
}
