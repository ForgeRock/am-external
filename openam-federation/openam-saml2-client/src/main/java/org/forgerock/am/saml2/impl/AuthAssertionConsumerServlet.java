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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.am.saml2.impl;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provide the AuthAssertion endpoint.
 */
@Singleton
public class AuthAssertionConsumerServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthAssertionConsumerServlet.class);
    private final Saml2Proxy saml2Proxy;

    @Inject
    AuthAssertionConsumerServlet(Saml2Proxy saml2Proxy) {
        this.saml2Proxy = saml2Proxy;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.debug("AuthAssertionConsumerServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        doRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        doRequest(request, response);
    }

    private void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            saml2Proxy.processSamlResponse(request, response);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
