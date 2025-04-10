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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepository;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;

import java.util.List;

import com.sun.identity.saml2.profile.SPCache;

/**
 * Provides helper methods specifically around using SAML2 Failover and the SAML2 Token Repository.
 * Users of these methods much ensure that SAML2 Failover is enabled by checking the result of the
 * {@link #isFailoverEnabled()} call before accessing the token repository.
 */
public class SAML2FailoverUtils {

    private SAML2FailoverUtils() {
    }

    /**
     * Checks whether SAML2 failover is enabled.
     * @return true if SAML2 failover is enabled otherwise false.
     */
    public static boolean isFailoverEnabled() {
        return !SPCache.isFedlet;
    }

    /**
     * Helper method for accessing the SAML2 Token Repository, should only be used when SAML2 failover is enabled.
     * @param primaryKey The primary key of SAML2 object to save
     * @param samlObj The SAML2 object to save
     * @param expirationTime Expiration time in seconds from epoch.
     * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
     */
    public static void saveSAML2TokenWithoutSecondaryKey(String primaryKey, Object samlObj, long expirationTime)
            throws SAML2TokenRepositoryException {

        saveSAML2Token(primaryKey, null, samlObj, expirationTime);
    }

    /**
     * Helper method for accessing the SAML2 Token Repository, should only be used when SAML2 failover is enabled.
     * Persists token with default expiry time
     * @param primaryKey The primary key of SAML2 object to save
     * @param samlObj The SAML2 object to save
     * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
     */
    public static void saveSAML2TokenWithoutSecondaryKey(String primaryKey, Object samlObj)
            throws SAML2TokenRepositoryException {
        // Cache survival time is 10 mins
        final long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval; //counted in seconds
        saveSAML2TokenWithoutSecondaryKey(primaryKey, samlObj, sessionExpireTime);
    }

    /**
     * Helper method for accessing the SAML2 Token Repository, should only be used when SAML2 failover is enabled.
     * @param primaryKey The primary key of the SAML2 object to save
     * @param secondaryKey Secondary key, can be null
     * @param samlObj The SAML2 object to save
     * @param expirationTime Expiration time in seconds from epoch.
     * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
     */
    public static void saveSAML2Token(String primaryKey, String secondaryKey, Object samlObj, long expirationTime)
            throws SAML2TokenRepositoryException {

        SAML2TokenRepositoryHolder.getRepo().saveSAML2Token(primaryKey, secondaryKey, samlObj, expirationTime);
    }

    /**
     * Helper method for accessing the SAML2 Token Repository, should only be used when SAML2 failover is enabled.
     * @param primaryKey The primary key of SAML2 object to retrieve
     * @return An object representing the SAML2 object put into the repository using the key or null if not found.
     * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
     */
    public static Object retrieveSAML2Token(String primaryKey) throws SAML2TokenRepositoryException {
        return SAML2TokenRepositoryHolder.getRepo().retrieveSAML2Token(primaryKey);
    }

    /**
     * Helper method for accessing the SAML2 Token Repository, should only be used when SAML2 failover is enabled.
     * @param secondaryKey Secondary key to use when searching for matching tokens
     * @return A non null, but possibly empty collection of SAML2 objects.
     * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
     */
    public static List retrieveSAML2TokensWithSecondaryKey(String secondaryKey) throws SAML2TokenRepositoryException {
        return SAML2TokenRepositoryHolder.getRepo().retrieveSAML2TokensWithSecondaryKey(secondaryKey);
    }

    /**
     * Helper method for accessing the SAML2 Token Repository, should only be used when SAML2 failover is enabled.
     * @param primaryKey The primary key of SAML2 object to delete
     * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
     */
    public static void deleteSAML2Token(String primaryKey) throws SAML2TokenRepositoryException {
        SAML2TokenRepositoryHolder.getRepo().deleteSAML2Token(primaryKey);
    }

    /**
     * Enum to lazy init the SAML2TokenRepository variable in a thread safe manner.
     */
    private enum SAML2TokenRepositoryHolder {
        INSTANCE;

        private final SAML2TokenRepository repository;

        private SAML2TokenRepositoryHolder() {
            repository = InjectorHolder.getInstance(SAML2TokenRepository.class);
        }

        static SAML2TokenRepository getRepo() {
            return INSTANCE.repository;
        }
    }
}
