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
 * $Id: DefaultSPAccountMapper.java,v 1.3 2008/07/08 23:03:34 hengming Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class <code>DefaultSPAccountMapper</code> is the default implementation of the
 * <code>DefaultLibrarySPAccountMapper</code> that is used to map the <code>SAML</code> protocol objects to the user
 * accounts at the <code>ServiceProvider</code> side of SAML v2 plugin.
 * Custom implementations may extend from this class to override some of these implementations if they choose to do so.
 */
public class DefaultSPAccountMapper extends DefaultLibrarySPAccountMapper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSPAccountMapper.class);

    /**
     * Default constructor
     */
    public DefaultSPAccountMapper() {
        super();
        logger.debug("DefaultSPAccountMapper.constructor: ");
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     *
     * @param realm Realm to check the dynamical profile creation attributes.
     * @return <code>true</code> if dynamical profile creation or ignore profile is enabled, <code>false</code>
     * otherwise.
     */
    @Override
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return SAML2PluginsUtils.isDynamicalOrIgnoredProfile(realm);
    }
}
