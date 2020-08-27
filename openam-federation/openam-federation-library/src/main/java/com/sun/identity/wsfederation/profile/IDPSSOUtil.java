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
 * $Id: IDPSSOUtil.java,v 1.3 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2018-2019 ForgeRock AS.
 */
package com.sun.identity.wsfederation.profile;

import static com.sun.identity.saml2.common.SAML2Constants.AUTH_URL;
import static com.sun.identity.wsfederation.common.WSFederationUtils.getMetaManager;
import static com.sun.identity.wsfederation.meta.WSFederationMetaUtils.getAttribute;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;

/**
 * The utility class is used by the identity provider to process
 * the authentication request from a service provider and send back
 * a proper response.
 * The identity provider can also send unsolicited response to a service
 * provider to do single sign on and/or federation.
 */
public class IDPSSOUtil {
    private static Logger debug = LoggerFactory.getLogger(IDPSSOUtil.class);

    /**
     * Returns the authentication service <code>URL</code> of the
     * identity provider
     *
     * @param realm the realm name of the identity provider
     * @param hostEntityId the entity id of the identity provider
     * @param request the <code>HttpServletRequest</code> object
     *
     * @return the authentication service <code>URL</code> of the
     * identity provider
     */
    public static String getAuthenticationServiceURL(String realm, String hostEntityId, HttpServletRequest request) {
        String classMethod = "IDPSSOUtil.getAuthenticationServiceURL: ";

        String authUrl = null;
        try {
            IDPSSOConfigElement config = getMetaManager().getIDPSSOConfig(realm, hostEntityId);
            authUrl = getAttribute(config.getValue(), AUTH_URL);
        } catch (WSFederationMetaException sme) {
            debug.warn("{}get IDPSSOConfig failed:", classMethod, sme);
        }

        if (isBlank(authUrl)) {
            StringBuilder sb = new StringBuilder(100);
            sb.append(request.getScheme()).append("://")
              .append(request.getServerName()).append(":")
              .append(request.getServerPort())
              .append(request.getContextPath())
              .append("/UI/Login?realm=").append(realm);
            authUrl = sb.toString();
        }
        debug.debug("{}auth url='{}'", classMethod, authUrl);
        return authUrl;
    }

    /**
     * Returns the assertion consumer service (ACS) URL for the entity.
     * @param entityId entity ID of provider
     * @param realm realm of the provider
     * @param wreply the ACSURL supplied by the requestor. If supplied, this is
     * checked against the URLs registered for the provider.
     * @return assertion consumer service (ACS) URL for the entity.
     */
    public static String getACSurl(String entityId, String realm,
        String wreply) throws WSFederationMetaException
    {
        WSFederationMetaManager metaManager = getMetaManager();
        FederationElement sp = metaManager.getEntityDescriptor(realm, entityId);
        Set<String> tokenEndpoints = metaManager.getTokenIssuerEndpoints(sp);
        if (wreply == null) {
            // Get first ACS URL for this SP, matches order of list in stored metadata.
            if (tokenEndpoints.size() > 1) {
                debug.debug("No wreply provided and more than one token endpoint issuer, returning first in list");
            }
            return CollectionUtils.getFirstItem(tokenEndpoints);
        }

        try {
            new URL(wreply);
        } catch (MalformedURLException murle) {
            debug.error("Invalid token issuer endpoint URL {}", wreply, murle);
            return null;
        }

        // Check that wreply matches one of the valid token issuer endpoints on this SP
        if (tokenEndpoints.contains(wreply)) {
            return wreply;
        }
        debug.error("No matching token issuer endpoint found for {}", wreply);
        return null;
    }    
}
