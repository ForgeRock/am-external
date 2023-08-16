/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LogManager.java,v 1.4 2008/08/06 17:28:14 exu Exp $
 *
 * Portions Copyrighted 2017-2019 ForgeRock AS.
 */

package com.sun.identity.plugin.log;

import org.slf4j.LoggerFactory;

import com.sun.identity.common.SystemConfigurationUtil;

/**
 * The <code>LogManager</code> is used to get an instance of
 * the <code>Logger</code> implementation class.
 * If the system property com.sun.identity.plugin.log.class is set, this class
 * will be used as log provider instead of the default implementation.
 *
 * @deprecated since 14.0.0, use Common Audit Service
 */
@Deprecated
public final class LogManager {
    
    public static final String LOG_PROVIDER_NAME = 
            "com.sun.identity.plugin.log.class";
    private static final org.slf4j.Logger debug = LoggerFactory.getLogger(LogManager.class);

    /**
     * Returns an instance of the <code>Logger</code> object.
     *
     * @param componentName name of the component .
     * @return instance of <code>Logger</code> object.
     * @exception LogException if there is an error.
     */
    public static Logger getLogger(String componentName)
    throws LogException {
        Logger logProvider = null;
        String pluginName = SystemConfigurationUtil.getProperty(
            LOG_PROVIDER_NAME, "com.sun.identity.plugin.log.impl.LogProvider");

        try {
            if (pluginName != null && pluginName.length() > 0) {
                Class logProviderClass =
                        Class.forName(pluginName.trim());
                logProvider = (Logger) logProviderClass.newInstance();
                logProvider.init(componentName);
            }
        } catch (Exception e) {
            debug.error("Error creating class instance : ", e);
            throw new LogException(e);
        }
        return logProvider;
    }
}