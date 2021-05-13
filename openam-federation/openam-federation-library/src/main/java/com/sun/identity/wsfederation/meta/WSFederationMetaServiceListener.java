/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSFederationMetaServiceListener.java,v 1.5 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2019 ForgeRock AS.
 */


package com.sun.identity.wsfederation.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.wsfederation.profile.SPCache;

/**
 * The <code>WSFederationMetaServiceListener</code> implements
 * <code>ConfigurationListener</code> interface and is
 * used for maintaining the metadata cache.
 */
class WSFederationMetaServiceListener implements ConfigurationListener
{
    private static Logger debug = LoggerFactory.getLogger(WSFederationMetaServiceListener.class);

    WSFederationMetaServiceListener() {
    }

    /**
     * This method will be invoked when a service's organization
     * configuation data has been changed.
     *
     * @param e the configuaration action event
     */
    public void configChanged(ConfigurationActionEvent e) {
        if (debug.isDebugEnabled()) {
            debug.debug("WSFederationMetaServiceListener.configChanged: "
                + "component=" + e.getComponentName() + ", config="
                + e.getConfigurationName());
        }
        WSFederationMetaCache.clear();
        if (e.getRealm() == null) {
            SPCache.clear();
        } else {
            SPCache.clear(e.getRealm());
        }
    }
}
