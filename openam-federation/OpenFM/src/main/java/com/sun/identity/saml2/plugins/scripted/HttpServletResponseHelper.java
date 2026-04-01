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
 * Copyright 2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.annotations.Supported;

/**
 * Provides helper functions for accessing HttpServletResponse properties in scripts.
 */
public class HttpServletResponseHelper {

    private final HttpServletResponse response;

    /**
     * Constructor that accepts the HttpServletResponse.
     *
     * @param response the HttpServletResponse
     */
    public HttpServletResponseHelper(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * Gets a response header value.
     *
     * @param name the header name
     * @return the header value, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getHeader(String name) {
        if (response != null && name != null) {
            return response.getHeader(name);
        }
        return null;
    }

    /**
     * Gets all values for a response header.
     *
     * @param name the header name
     * @return a list of header values, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getHeaders(String name) {
        return Optional.ofNullable(response)
                .map(r -> r.getHeaders(name))
                .map(headers -> (List<String>) new ArrayList<>(headers))
                .orElse(emptyList());
    }

    /**
     * Gets all response header names.
     *
     * @return a list of all response header names, or null if none present
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getHeaderNames() {
        return Optional.ofNullable(response)
                .map(HttpServletResponse::getHeaderNames)
                .map(names -> (List<String>) new ArrayList<>(names))
                .orElse(emptyList());
    }

    /**
     * Adds a response header. Can be called multiple times for the same header name.
     *
     * @param name the header name
     * @param value the header value
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void addHeader(String name, String value) {
        if (response != null && name != null && value != null) {
            response.addHeader(name, value);
        }
    }

    /**
     * Sets a response header. Overwrites any existing headers with the same name.
     *
     * @param name the header name
     * @param value the header value
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void setHeader(String name, String value) {
        if (response != null && name != null && value != null) {
            response.setHeader(name, value);
        }
    }

    /**
     * Sends a redirect response.
     * Usage: responseHelper.sendRedirect("https://example.com")
     *
     * @param location the redirect URL
     * @throws IOException if an I/O error occurs
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void sendRedirect(String location) throws IOException {
        if (response != null && location != null) {
            response.sendRedirect(location);
        }
    }
}

