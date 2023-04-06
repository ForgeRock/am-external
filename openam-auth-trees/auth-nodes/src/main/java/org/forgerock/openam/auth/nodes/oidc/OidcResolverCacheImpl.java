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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oidc;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.oauth.resolvers.OpenIdResolverFactory;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * A class that implements the OidcResolverCache interface.
 *
 * @see OidcResolverCache
 */
public class OidcResolverCacheImpl implements OidcResolverCache {
    private static final String SUN_AM_AUTH_O_AUTH_SERVICE = "sunAMAuthOAuthService";
    private static final Logger logger = LoggerFactory.getLogger(OidcResolverCacheImpl.class);

    private final OpenIdResolverFactory openIdResolverFactory;
    private final ConcurrentMap<String, ConcurrentMap<String, OpenIdResolver>> resolverMap = new ConcurrentHashMap<>();

    /**
     * Guice provisioned initializer.
     * <p>
     * As part of this request this {@link org.forgerock.openam.authentication.modules.oidc.OpenIdResolverCache}
     * instance will register a listener to the {@link #SUN_AM_AUTH_O_AUTH_SERVICE} service.
     */
    @Inject
    OidcResolverCacheImpl(@Nonnull OpenIdResolverFactory openIdResolverFactory, CoreWrapper coreWrapper) {
        this(openIdResolverFactory, getServiceConfigManager(coreWrapper));
    }

    /**
     * Dependencies constructor intended for test purposes only.
     *
     * @param openIdResolverFactory A notnull OpenIdResolverFactory instance.
     * @param manager               A possibly null {@link ServiceConfigManager} instance.
     */
    protected OidcResolverCacheImpl(@Nonnull OpenIdResolverFactory openIdResolverFactory,
                                    ServiceConfigManager manager) {
        Reject.ifNull(openIdResolverFactory);
        this.openIdResolverFactory = openIdResolverFactory;

        if (manager != null) {
            manager.addListener(new OpenIDResolveCacheChangeListener());
        }
    }

    private static ServiceConfigManager getServiceConfigManager(CoreWrapper coreWrapper) {
        try {
            SSOToken token = coreWrapper.getAdminToken();
            return coreWrapper.getServiceConfigManager(SUN_AM_AUTH_O_AUTH_SERVICE, token);
        } catch (SMSException | SSOException e) {
            String message = "OpenIDResolverCacheImpl::Unable to construct ServiceConfigManager: " + e;
            logger.error(message, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     *
     * @param issuerFromJwk         Issuer.
     * @param cryptoContextType     The crypto context value type.
     * @param cryptoContextValue    The crypto context value.
     * @param cryptoContextValueUrl The crypto context url.
     * @return OpenIdResolver.
     * @throws FailedToLoadJWKException
     */
    private OpenIdResolver createNewResolver(String issuerFromJwk, String cryptoContextType, String cryptoContextValue,
                                             URL cryptoContextValueUrl) throws FailedToLoadJWKException {
        OidcNode.OpenIdValidationType openIdValidationType = OidcNode.OpenIdValidationType.valueOf(cryptoContextType);
        switch (openIdValidationType) {
        case CLIENT_SECRET:
            return openIdResolverFactory.createSharedSecretResolver(issuerFromJwk, cryptoContextValue);
        case WELL_KNOWN_URL:
            OpenIdResolver newResolver = openIdResolverFactory
                    .createFromOpenIDConfigUrl(issuerFromJwk, cryptoContextValueUrl);
            //check is only relevant in this block, as issuer is specified in the json blob referenced by url.
            if (!issuerFromJwk.equals(newResolver.getIssuer())) {
                throw new IllegalStateException("The specified issuer, " + issuerFromJwk
                        + ", does not match the issuer, " + newResolver.getIssuer()
                        + " referenced by the configuration url, " + cryptoContextValue);
            }
            return newResolver;
        case JWK_URL:
            return openIdResolverFactory.createJWKResolver(issuerFromJwk, cryptoContextValueUrl);
        default:
            throw new IllegalArgumentException("The specified cryptoContextType, " + cryptoContextType
                        + " was unexpected!");
        }

    }

    /**
     * ServiceListener implementation to clear cache when it changes.
     */
    private final class OpenIDResolveCacheChangeListener implements ServiceListener {

        public void schemaChanged(String serviceName, String version) {
            logger.warn("The schemaChanged ServiceListener method was invoked for service " + serviceName
                    + ". This is unexpected.");
        }

        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                                        int type) {
            logger.warn("The globalConfigChanged ServiceListener method was invoked for service " + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                              String serviceComponent, int type) {
            if (logger.isDebugEnabled()) {
                logger.debug("Clearing OpenId Resolver Cache.");
            }
            resolverMap.clear();
        }
    }
}
