/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.saml2;

import com.sun.identity.saml2.profile.FederatedSSOException;

import java.io.IOException;

/**
 * An Authenticator is an object used to provide authentication to the session.
 */
public interface SAMLAuthenticator {

    /**
     * Perform authentication tasks.
     *
     * @throws FederatedSSOException If federation authentication was not successful.
     * @throws IOException if the Federation request fails because of communication problems.
     */
    void authenticate() throws FederatedSSOException, IOException;

}
