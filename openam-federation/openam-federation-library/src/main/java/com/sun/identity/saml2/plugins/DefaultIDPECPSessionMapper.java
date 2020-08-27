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
 * $Id: DefaultIDPECPSessionMapper.java,v 1.2 2008/06/25 05:47:50 qcheng Exp $
 *
 * Portions Copyrighted 2019 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class is the default implementation of <code>IDPECPSessionMapper</code>.
 */ 
public class DefaultIDPECPSessionMapper implements IDPECPSessionMapper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIDPECPSessionMapper.class);

    /**
     * Returns user valid session.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return a vaild user session or null if not found
     * @exception SAML2Exception if error occurs. 
     */
    public Object getSession(HttpServletRequest request,
        HttpServletResponse response) throws SAML2Exception {

        Object session = null;

        try {
            session = SessionManager.getProvider().getSession(request);
        } catch (SessionException se) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "DefaultIDPECPSessionMapper.getSession:", se);
            }
        }

        return session;
    }
}
