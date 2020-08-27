/*
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
 * $Id: SAML2MetaServiceListener.java,v 1.5 2009/08/28 23:42:14 exu Exp $
 *
 * Portions Copyrighted 2019-2020 ForgeRock AS.
 */


package com.sun.identity.saml2.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.SPCache;

/**
 * The <code>SAML2MetaServiceListener</code> implements
 * <code>ConfigurationListener</code> interface and is
 * used for maintaining the metadata cache.
 */
public class SAML2MetaServiceListener implements ConfigurationListener
{
    private static Logger debug = LoggerFactory.getLogger(SAML2MetaServiceListener.class);

    SAML2MetaServiceListener() {
    }

    /**
     * This method will be invoked when a service's organization
     * configuation data has been changed.
     *
     * @param e the configuaration action event
     */
    public void configChanged(ConfigurationActionEvent e) {
        if (debug.isDebugEnabled()) {
            debug.debug("SAML2MetaServiceListener.configChanged: config=" + 
                e.getConfigurationName() + ", component=" + 
                e.getComponentName());
        }
        clearCaches();
    }

    /**
     * Clears the caches used by the SAML2 implementation.
     */
    public void clearCaches() {
        SAML2MetaCache.clear();
        SPCache.clear();
        IDPCache.clear();
        KeyUtil.clear();
    }
}
