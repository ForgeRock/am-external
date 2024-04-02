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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.saml2.soap;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.saml2.Saml2EntityRole;

import com.sun.identity.saml2.common.SOAPCommunicator;

/**
 * Simple SOAPConnectionFactory implementation for the fedlet, relies on {@link SOAPCommunicator} to create the
 * SOAPConnection
 */
public class SimpleSOAPConnectionFactory implements SOAPConnectionFactory {

    @Override
    public SOAPConnection create(Realm realm, SOAPCommunicator soapCommunicator, String entityId,Saml2EntityRole role)
            throws SOAPException {
        return soapCommunicator.openSOAPConnection();
    }
}
