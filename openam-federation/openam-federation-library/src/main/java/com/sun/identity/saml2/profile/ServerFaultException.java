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

package com.sun.identity.saml2.profile;

import org.forgerock.openam.saml2.plugins.IDPAdapter;

import com.sun.identity.saml2.common.SAML2Constants;

/**
 * Indicates a server fault occurred during federated SSO.
 *
 * @since 13.0.0
 */
public class ServerFaultException extends FederatedSSOException {

    /**
     * Constructs the server fault exception with the given message code.
     *
     * @param messageCode the message code. May not be null.
     */
    public ServerFaultException(final String messageCode) {
        super(null, messageCode, null);
    }

    /**
     * Constructs the server fault exception with the given message code and detail message.
     *
     * @param messageCode the message code. May not be null.
     * @param detail the detail message. May be null.
     */
    public ServerFaultException(final String messageCode, final String detail) {
        super(null, messageCode, detail);
    }

    /**
     * Constructs the server fault exception with the given IDP adapter and message code.
     *
     * @param idpAdapter the identity provider adapter. May be null.
     * @param messageCode the message code. May not be null.
     */
    public ServerFaultException(final IDPAdapter idpAdapter, final String messageCode) {
        super(idpAdapter, messageCode, null);
    }

    /**
     * Constructs the client fault exception with the given IDP adapter, message code, and detail message.
     *
     * @param idpAdapter the identity provider adapter. May be null.
     * @param messageCode the message code. May not be null.
     * @param detail the detail message. May be null.
     */
    public ServerFaultException(final IDPAdapter idpAdapter, final String messageCode,
                                final String detail) {
        super(idpAdapter, messageCode, detail);
    }

    @Override
    public String getFaultCode() {
        return SAML2Constants.SERVER_FAULT;
    }
}
