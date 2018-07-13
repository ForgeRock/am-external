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
 * Copyright 2018 ForgeRock AS.
 */

package com.sun.identity.setup;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This filter brings administrator to a configuration page
 * where the product can be configured if the product is not
 * yet configured.
*/
public final class AMSetupFilter implements Filter {
    private boolean initialized;

    public void doFilter(
        ServletRequest request, 
        ServletResponse response, 
        FilterChain filterChain
    ) throws IOException, ServletException 
    {
        filterChain.doFilter(request, response);
    }

    /**
     * Destroy the filter config on sever shutdowm 
     */
    public void destroy() {
    }
    
    /**
     * Initializes the filter.
     *
     * @param filterConfig Filter Configuration.
     */
    public void init(FilterConfig filterConfig) {
        ServletContext cxt = filterConfig.getServletContext();
        Map<String, String> configData = new HashMap<String, String>();
        ResourceBundle res = ResourceBundle.getBundle("configparam");
        for (Enumeration e = res.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String val = res.getString(key);
            configData.put(key, val);
        }
        EmbeddedOpenSSO embOpenSSO = new EmbeddedOpenSSO(
            cxt, System.getProperty("user.home") + "/" + cxt.getContextPath(),
            configData);
        initialized = embOpenSSO.isConfigured();
        if (!initialized) {

            embOpenSSO.configure();
        }
        embOpenSSO.startup();
    }
   
}
