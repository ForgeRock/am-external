/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.scripted;

import org.forgerock.util.Reject;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Wraps the http servlet request object so to expose script
 * friendly methods that allow access to the underlying data.
 *
 * @since 12.0.0
 */
public final class ScriptHttpRequestWrapper {

    private final HttpServletRequest request;

    public ScriptHttpRequestWrapper(final HttpServletRequest request) {
        Reject.ifNull(request);
        this.request = request;
    }

    /**
     * Gets the parameter associated with the passed name.
     *
     * @param name
     *         the parameter name
     *
     * @return the parameter value
     */
    public String getParameter(final String name) {
        return request.getParameter(name);
    }

    /**
     * Gets the parameters associated with the passed name.
     *
     * @param name
     *         the parameter name
     *
     * @return the parameter values
     */
    public String[] getParameters(final String name) {
        return request.getParameterValues(name);
    }

    /**
     * Gets the header associated with the passed name.
     *
     * @param name
     *         the header name
     *
     * @return the header value
     */
    public String getHeader(final String name) {
        return request.getHeader(name);
    }

    /**
     * Gets the headers associated with the passed name.
     *
     * @param name
     *         the header name
     *
     * @return the header values
     */
    public String[] getHeaders(final String name) {
        @SuppressWarnings("unchecked")
        final Enumeration<String> headerEnum = request.getHeaders(name);
        final List<String> headers = Collections.list(headerEnum);
        return headers.toArray(new String[headers.size()]);
    }

}
