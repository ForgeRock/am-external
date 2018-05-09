/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.plugins.DefaultIDPAdapter;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.ServerFaultException;

import javax.servlet.http.HttpServletRequest;

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
     * @return Non null String
     * @throws ServerFaultException If unable to read the IDP Entity ID from the realm meta.
     * @throws ClientFaultException If the client requested an invalid binding for this IDP.
     */
    String getIDPEntity(String idpMetaAlias, String realm)
            throws ServerFaultException, ClientFaultException ;

    /**
     * Loads the {@link SAML2IdentityProviderAdapter} IDP adapter which will be called as part
     * of IDP processing.
     *
     * @param realm Possibly null realm.
     * @param idpEntityID Non null idpEntityID.
     *
     * @return The loaded {@link SAML2IdentityProviderAdapter} if it could be loaded otherwise
     * the default implementation {@link DefaultIDPAdapter}.
     */
    SAML2IdentityProviderAdapter getIDPAdapter(String realm, String idpEntityID);

    /**
     * Gets the realm for the entity from the IDP Meta Alias.
     *
     * @param idpMetaAlias the IDP Meta Alias
     * @return the realm that the idp belongs to
     */
    String getRealmByMetaAlias(String idpMetaAlias);
}
