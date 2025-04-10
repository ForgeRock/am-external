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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
     * Retrieves and processes the authentication details for the session from the cache.  This also sends the
     * appropriate response to the user, such as returning their SAML response to the ACS URL, or redirecting to
     * authenticate again if needed.
     *
     * @throws SessionException if there is a problem retrieving the session.
     * @throws ServerFaultException if there was a problem with the authentication
     * @throws ClientFaultException if there is a problem with the provided data.
     */
    void retrieveAuthenticationFromCache() throws SessionException, ServerFaultException, ClientFaultException;
}
