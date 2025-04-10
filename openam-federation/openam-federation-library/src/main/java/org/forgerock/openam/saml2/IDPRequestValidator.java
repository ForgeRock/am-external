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

import com.sun.identity.saml2.plugins.DefaultIDPAdapter;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.ServerFaultException;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.saml2.plugins.IDPAdapter;

/**
 * Responsible for validating and providing a handful of parameters required for
 * processing the IDP requests.
 *
 * Note: Currently all supporting classes have extensive static initialisation which
 * is preventing this class from providing more immutability.
 */
public interface  IDPRequestValidator {

    /**
     * The meta alias is used to locate the provider's entity identifier and the
     * organization in which it is located.
     *
     * @return A non null string closely resembling the entities realm.
     *
     * @throws ClientFaultException If the meta alias was not provided in the request
     * or could not be parsed out of the request URI.
     */
    String getMetaAlias(HttpServletRequest request) throws ClientFaultException ;

    /**
     * The entity identifier for the IDP.
     *
     * @param idpMetaAlias Non null meta alias.
     * @param isFromECP whether the validator will be validating a request from an ecp
     * @return Non null String
     * @throws ServerFaultException If unable to read the IDP Entity ID from the realm meta.
     * @throws ClientFaultException If the client requested an invalid binding for this IDP.
     */
    String getIDPEntity(String idpMetaAlias, String realm, boolean isFromECP)
            throws ServerFaultException, ClientFaultException ;

    /**
     * Loads the {@link org.forgerock.openam.saml2.plugins.IDPAdapter} IDP adapter which will be called as part
     * of IDP processing.
     *
     * @param realm Possibly null realm.
     * @param idpEntityID Non null idpEntityID.
     *
     * @return The loaded {@link org.forgerock.openam.saml2.plugins.IDPAdapter} if it could be loaded otherwise
     * the default implementation {@link DefaultIDPAdapter}.
     */
    IDPAdapter getIDPAdapter(String realm, String idpEntityID);

    /**
     * Gets the realm for the entity from the IDP Meta Alias.
     *
     * @param idpMetaAlias the IDP Meta Alias
     * @return the realm that the idp belongs to
     */
    String getRealmByMetaAlias(String idpMetaAlias);
}
