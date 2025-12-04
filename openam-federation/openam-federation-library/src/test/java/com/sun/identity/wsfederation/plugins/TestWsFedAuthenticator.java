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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package com.sun.identity.wsfederation.plugins;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
