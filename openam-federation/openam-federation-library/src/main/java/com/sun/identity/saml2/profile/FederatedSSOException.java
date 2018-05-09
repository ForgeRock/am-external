/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import static org.forgerock.util.Reject.checkNotNull;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;

/**
 * Checked exception for errors that occur during federated single sign-on (SSO).
 *
 * @since 13.0.0
 * @see ServerFaultException
 * @see ClientFaultException
 */
public abstract class FederatedSSOException extends Exception {
    private final String messageCode;
    private final String detail;
    private final SAML2IdentityProviderAdapter idpAdapter;

    /**
     * Constructs the FederatedSSOException with the given parameters.
     *
     * @param idpAdapter the identity provider adapter, if resolved - may be null.
     * @param messageCode the message code of the error that occurred.
     * @param detail the detail of the exception.
     */
    public FederatedSSOException(final SAML2IdentityProviderAdapter idpAdapter, final String messageCode,
                                 final String detail) {
        super();
        this.messageCode = checkNotNull(messageCode, "Message code is null");
        this.detail = detail;
        this.idpAdapter = idpAdapter;
    }

    @Override
    public String getMessage() {
        return SAML2Utils.bundle.getString(messageCode) + (detail != null ? " (" + detail  +")" : "");
    }

    /**
     * Returns the message code of this error.
     *
     * @return the message code. Never null.
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * Returns the detail message of this error, if provided.
     *
     * @return the detail message - may be null.
     */
    public String getDetail() {
        return detail;
    }

    /**
     * The IDP adapter. This can be used to invoke hooks during error processing.
     *
     * @return the idp adapter. May be null.
     */
    public SAML2IdentityProviderAdapter getIdpAdapter() {
        return idpAdapter;
    }

    /**
     * The SOAP fault code of the error.
     *
     * @return one of {@link SAML2Constants#SERVER_FAULT} or {@link SAML2Constants#CLIENT_FAULT}. Never null.
     */
    public abstract String getFaultCode();
}
