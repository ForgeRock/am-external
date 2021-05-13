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
 * Copyright 2019 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Provides a wrapper for the HttpServletResponse that is made available to the ServiceProviderAdapter methods
 * {@link SAML2ServiceProviderAdapter} so that any calls to sendRedirect can be delayed until after any
 * processing (e.g. setting of session cookies) has been completed.
 */
public class SPAdapterHttpServletResponseWrapper extends HttpServletResponseWrapper {

    /**
     * Constructs a response adapter wrapping the given response.
     * @throws java.lang.IllegalArgumentException if the response is null
     */
    public SPAdapterHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * Set the redirect location but do not commit the response
     */
    @Override
    public void sendRedirect(String location) throws IOException {
        setHeader("Location", location);
        setStatus(302);
        resetBuffer();
    }
}
