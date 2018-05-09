/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;

import java.net.URL;

/**
 * Interface consumed by the OpenIdConnect authN module. It provides thread-safe access and creation to OpenIdResolver
 * instances, the interface defining the verification of OpenID Connect ID Tokens. Fundamentally, OpenIdResolver instances
 * may need to pull web-based key state to verify ID Tokens, and the latency of this initialization should not be incurred
 * by each instantiation of the OpenIdConnect authN module.
 */
public interface OpenIdResolverCache {
    /**
     * @param cryptoContextDefinitionValue Either the discovery url, jwk url, or client_secret used to configure the authN module.
     *                                     This value is the key into the Map of OpenIdResolver instances.
     * @return The OpenIdResolver instance which can validate jwts issued by the specified issuer. If no issuer has
     * been configured, null will be returned.
     */
    OpenIdResolver getResolverForIssuer(String cryptoContextDefinitionValue);

    /**
     * @param  issuerFromJwk The string corresponding to the issuer. This information is present in the OIDC discovery data, but
     *                it is used to insure that the issuer string configured for the login module, and the issuer string
     *                pulled from the configuration url, match.
     * @param cryptoContextType Identifies the manner in which the crypto context was defined (discovery url, jwk url, or client_secret)
     * @param cryptoContextValue The specific value of the discovery url, jwk url, or client_secret
     * @param cryptoContextValueUrl If the cryptoContextType corresponds to the discovery or jwk url, the URL format of the
     *                              string. Passed so that the implementation does not need to handle the MalformedURLException
     * @return The OpenIdResolver instantiated with this OIDC discovery data.
     * @throws IllegalStateException if the issuer parameter does not match the discovery document referenced by the discovery url.
     *         FailedToLoadJWKException If the jwk descriptor could not be loaded from url referenced by the discovery or jwk url
     *         IllegalArgumentException if the cryptoContextType specification is unknown
     */
    OpenIdResolver createResolver(String issuerFromJwk, String cryptoContextType, String cryptoContextValue, URL cryptoContextValueUrl)
            throws FailedToLoadJWKException;
}
