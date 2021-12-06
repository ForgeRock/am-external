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
 * Copyright 2021 ForgeRock AS.
 */

package org.forgerock.openam.saml2.plugins;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * An HttpServletRequest Wrapper which allows the DefaultWSFedAuthenticator class to add additional parameters
 * to the Http Request.
 */
public class DefaultWsFedAuthHttpRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String[]> parameterMap = new HashMap<>();

    /**
     * Constructs a SelfServiceAuthHttpRequestWrapper.
     *
     * @param request The wrapped HttpServletRequest.
     */
    public DefaultWsFedAuthHttpRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * Gets the parameter with the given name from the underlying HttpServletRequest. If that is null will attempt
     * the same with the local parameter map.
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value == null) {
            String[] values = parameterMap.get(name);
            if (values != null && values.length > 0) {
                value = values[0];
            }
        }

        return value;
    }

    /**
     * Creates a combined parameter map from the underlying HttpServletRequest and the local parameter map.
     * <p>
     * The local parameter map will override any duplicate entries in the underlying HttpServletRequest map.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> combined = new HashMap<>(super.getParameterMap());
        combined.putAll(parameterMap);
        return combined;
    }

    /**
     * Gets the parameter names from the combined parameter maps.
     *
     * @return {@inheritDoc}
     * @see #getParameterMap()
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    /**
     * Gets the parameter values from the combined parameter maps.
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @see #getParameterMap()
     */
    @Override
    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    /**
     * Adds a parameter to the local parameter map.
     *
     * @param key The parameter key.
     * @param value The parameter value.
     */
    public void addParameter(String key, String value) {
        parameterMap.put(key, new String[]{value});
    }
}
