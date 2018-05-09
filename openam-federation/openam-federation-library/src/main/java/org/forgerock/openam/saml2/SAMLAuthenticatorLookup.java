/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.ServerFaultException;

/**
 * The SAMLAuthenticatorLookup is used to lookup existing authentication details for a session.
 */
public interface SAMLAuthenticatorLookup {

    /**
     * Retrieves the authentication details for the session from the cache.
     * @throws SessionException if there is a problem retrieving the session.
     * @throws ServerFaultException if there was a problem with the authentication
     * @throws ClientFaultException if there is a problem with the provided data.
     */
    void retrieveAuthenticationFromCache() throws SessionException, ServerFaultException, ClientFaultException;
}
