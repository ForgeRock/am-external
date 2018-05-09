/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;

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
    public ServerFaultException(final SAML2IdentityProviderAdapter idpAdapter, final String messageCode) {
        super(idpAdapter, messageCode, null);
    }

    /**
     * Constructs the client fault exception with the given IDP adapter, message code, and detail message.
     *
     * @param idpAdapter the identity provider adapter. May be null.
     * @param messageCode the message code. May not be null.
     * @param detail the detail message. May be null.
     */
    public ServerFaultException(final SAML2IdentityProviderAdapter idpAdapter, final String messageCode,
                                final String detail) {
        super(idpAdapter, messageCode, detail);
    }

    @Override
    public String getFaultCode() {
        return SAML2Constants.SERVER_FAULT;
    }
}
