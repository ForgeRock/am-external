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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.annotations.Supported;

/**
 * Provides helper functions for accessing HttpServletRequest properties in scripts.
 */
public class HttpServletRequestHelper {

    private final HttpServletRequest request;

    /**
     * Constructor that accepts the HttpServletRequest.
     *
     * @param request the HttpServletRequest
     */
    public HttpServletRequestHelper(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Gets a request attribute.
     *
     * @param name the attribute name
     * @return the attribute value, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public Object getAttribute(String name) {
        if (request != null && name != null) {
            return request.getAttribute(name);
        }
        return null;
    }

    /**
     * Sets a request attribute.
     *
     * @param name the attribute name
     * @param value the attribute value
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void setAttribute(String name, Object value) {
        if (request != null && name != null) {
            request.setAttribute(name, value);
        }
    }

    /**
     * Gets a request header value.
     *
     * @param name the header name
     * @return the header value, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getHeader(String name) {
        if (request != null && name != null) {
            return request.getHeader(name);
        }
        return null;
    }

    /**
     * Gets all values for a request header.
     *
     * @param name the header name
     * @return a list of header values, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getHeaders(String name) {
        if (request != null && name != null) {
            Enumeration<String> headers = request.getHeaders(name);
            if (headers != null) {
                List<String> result = new ArrayList<>();
                while (headers.hasMoreElements()) {
                    result.add(headers.nextElement());
                }
                return result;
            }
        }
        return null;
    }

    /**
     * Gets a request parameter value.
     *
     * @param name the parameter name
     * @return the parameter value, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getParameter(String name) {
        if (request != null && name != null) {
            return request.getParameter(name);
        }
        return null;
    }

    /**
     * Gets all values for a request parameter.
     *
     * @param name the parameter name
     * @return an array of parameter values, or null if not found
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String[] getParameterValues(String name) {
        if (request != null && name != null) {
            return request.getParameterValues(name);
        }
        return null;
    }
}

