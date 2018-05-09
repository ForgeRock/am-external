/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package com.sun.identity.wsfederation.plugins;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import org.forgerock.openam.saml2.plugins.WsFedAuthenticator;
import org.forgerock.openam.wsfederation.common.ActiveRequestorException;

import com.iplanet.sso.SSOToken;

/**
 * Used in test cases that want to simply return a fixed ssoToken value.
 */
public class TestWsFedAuthenticator implements WsFedAuthenticator {

    private static SSOToken ssoToken;

    /**
     * Set ssoToken to a fixed value for a test run.
     * @param newSsoToken The fixed value to use during a test run.
     */
    public static void setSsoToken(SSOToken newSsoToken) {
        ssoToken = newSsoToken;
    }

    @Override
    public SSOToken authenticate(HttpServletRequest request, HttpServletResponse response, SOAPMessage soapMessage,
                                 String realm, String username, char[] password) throws ActiveRequestorException {
        return ssoToken;
    }
}
