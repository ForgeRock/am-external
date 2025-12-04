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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.soap;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.saml2.Saml2EntityRole;

import com.sun.identity.saml2.common.SOAPCommunicator;

/**
 * Guice-injectable factory for creating SOAPConnection instances.
 */
public interface SOAPConnectionFactory {

    /**
     * Create instance of {@link SOAPConnection}
     *
     * @param realm the realm the entity that requires the connection resides in
     * @param soapCommunicator the {@link SOAPCommunicator} instance
     * @param entityId the SAML entity id that requires the connection
     * @param role the {@link Saml2EntityRole} of the entity requiring the connection
     *
     * @return a SOAPConnection instance
     * @throws SOAPException if there was an error creating the SOAPConnection
     */
    SOAPConnection create(Realm realm, SOAPCommunicator soapCommunicator, String entityId, Saml2EntityRole role)
            throws SOAPException;
}
