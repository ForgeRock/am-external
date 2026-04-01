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
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.common;

import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;

import com.sun.identity.saml2.profile.SPCache;

/**
 * Utility class to access SAML data stored from either the SP Cache or CTS.
 */
public final class SAMLDataAccessor {

    private SAMLDataAccessor() {
    }

    /**
     * Retrieves SAML data from either the SP Cache or CTS.
     * If the data is not found in the SP Cache, it will be retrieved from CTS and removed from there, if it exists.
     *
     * @param key The key to lookup the SAML data.
     * @param <T> The type of SAML data expected.
     * @return The SAML data, or null if not found.
     * @throws SAML2TokenRepositoryException If there is an error retrieving the data from CTS.
     */
    public static <T> T getSAMLData(String key) throws SAML2TokenRepositoryException {
        var cachedData = (T) SPCache.samlDataHash.get(key);
        if (cachedData != null) {
            return cachedData;
        }
        return retrieveAndCacheSamlDataFromCTS(key);
    }

    private static <T> T retrieveAndCacheSamlDataFromCTS(String key) throws SAML2TokenRepositoryException {
        var retrievedData = (T) SAML2FailoverUtils.retrieveSAML2Token(key);
        if (retrievedData != null) {
            SPCache.samlDataHash.put(key, retrievedData);
        }
        return retrievedData;
    }
}
