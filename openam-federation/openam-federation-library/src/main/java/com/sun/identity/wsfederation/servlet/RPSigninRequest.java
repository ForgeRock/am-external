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
 * $Id: RPSigninRequest.java,v 1.9 2009/11/03 00:48:54 madan_ranganath Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */

package com.sun.identity.wsfederation.servlet;

import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;

/**
 * This class implements the sign-in request for the service provider.
 */
public class RPSigninRequest extends WSFederationAction {
    private static Logger debug = LoggerFactory.getLogger(RPSigninRequest.class);
    
    String whr;
    String wreply;
    String wctx;
    String wct;
            
    /**
     * Creates a new instance of RPSigninRequest
     * @param request HTTPServletRequest for this interaction
     * @param response HTTPServletResponse for this interaction
     * @param whr the whr parameter from the signin request
     * @param wct the wct parameter from the signin request
     * @param wctx the wctx parameter from the signin request
     * @param wreply the wreply parameter from the signin request
     */
    public RPSigninRequest(HttpServletRequest request,
        HttpServletResponse response, String whr, 
        String wct, String wctx, String wreply) {
        super(request,response);
        this.whr = whr;
        this.wct = wct;
        this.wctx = wctx;
        this.wreply = wreply;
    }
    
    /**
     * Processes the sign-in request, redirecting the browser to the identity
     * provider via the HttpServletResponse passed to the constructor.
     */
    public void process() throws WSFederationException, IOException
    {
        String classMethod = "RPSigninRequest.process: ";
        
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"entered method");
        }

        if (wctx == null || wctx.length() == 0){
            // Exchange reply URL for opaque identifier
            wctx = (wreply != null && (wreply.length() > 0)) ? 
                WSFederationUtils.putReplyURL(wreply) : null;
        }

        String spMetaAlias = WSFederationMetaUtils.getMetaAliasByUri(
                                            request.getRequestURI());

        if ( spMetaAlias==null || spMetaAlias.length()==0 ) {
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("MetaAliasNotFound"));
        }

        String spRealm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
        
        WSFederationMetaManager metaManager = 
            WSFederationUtils.getMetaManager();
        String spEntityId = 
            metaManager.getEntityByMetaAlias(spMetaAlias);        
        if ( spEntityId==null || spEntityId.length()==0 )
        {
            String[] args = {spMetaAlias, spRealm};
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                "invalidMetaAlias", args);
        }

        SPSSOConfigElement spConfig = 
            metaManager.getSPSSOConfig(spRealm,spEntityId);
        if ( spConfig==null ) {
            String[] args = {spEntityId, spRealm};
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                "badSPEntityID",args);
        }

        Map<String,List<String>> spConfigAttributes = 
            WSFederationMetaUtils.getAttributes(spConfig.getValue());

        String accountRealmSelection = 
                CollectionUtils.getFirstItem(spConfigAttributes.get(
                com.sun.identity.wsfederation.common.WSFederationConstants.ACCOUNT_REALM_SELECTION));
        if ( accountRealmSelection == null )
        {
            accountRealmSelection = 
                WSFederationConstants.ACCOUNT_REALM_SELECTION_DEFAULT;
        }
        String accountRealmCookieName =
                CollectionUtils.getFirstItem(spConfigAttributes.get(WSFederationConstants.ACCOUNT_REALM_COOKIE_NAME));
        if ( accountRealmCookieName == null )
        {
            accountRealmCookieName = 
                WSFederationConstants.ACCOUNT_REALM_COOKIE_NAME_DEFAULT;
        }
        String homeRealmDiscoveryService = 
            CollectionUtils.getFirstItem(spConfigAttributes.get(WSFederationConstants.HOME_REALM_DISCOVERY_SERVICE));

        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"account realm selection method is " + 
                accountRealmSelection);
        }

        String idpIssuerName = null;
        if (whr != null && whr.length() > 0)
        {
            // whr parameter overrides other mechanisms...
            idpIssuerName = whr;

            if (accountRealmSelection.equals(WSFederationConstants.COOKIE)) 
            {
                // ...and overwrites cookie
                Cookie cookie = new Cookie(accountRealmCookieName,whr);
                // Set cookie to persist for a year
                cookie.setMaxAge(60*60*24*365);
		CookieUtils.addCookieToResponse(response, cookie);
            }
        }
        else
        {
            if (accountRealmSelection.equals(
                WSFederationConstants.USERAGENT)) {
                String uaHeader = 
                    request.getHeader(WSFederationConstants.USERAGENT);
                if (debug.isDebugEnabled()) {
                    debug.debug(classMethod+"user-agent is :" + uaHeader);
                }
                idpIssuerName = 
                    WSFederationUtils.accountRealmFromUserAgent(uaHeader, 
                    accountRealmCookieName);
            } else if (accountRealmSelection.equals(
                WSFederationConstants.COOKIE)) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (int i = 0; i < cookies.length; i++) {
                        if (cookies[i].getName().equals(
                            accountRealmCookieName)) {
                            idpIssuerName = cookies[i].getValue();
                            break;
                        }
                    }
                }
            } else {
                debug.error(classMethod+"unexpected value for " + 
                    WSFederationConstants.ACCOUNT_REALM_SELECTION + " : " + 
                    accountRealmSelection);
                throw new WSFederationException(
                    WSFederationUtils.bundle.getString("badAccountRealm"));
            }
        }

        FederationElement sp =
            metaManager.getEntityDescriptor(spRealm,spEntityId);
        String spIssuerName = 
            metaManager.getTokenIssuerName(sp);
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"SP issuer name:" + spIssuerName);
        }

        String idpEntityId = null;
        if (idpIssuerName != null && idpIssuerName.length() > 0)
        {
            // Got the issuer name from the cookie/UA string - let's see if 
            // we know the entity ID
            idpEntityId = 
                metaManager.getEntityByTokenIssuerName(spRealm,
                idpIssuerName);
        }

        if (idpEntityId == null) {
            // See if there is only one trusted IdP configured...
            List<String> allRemoteIdPs = 
                metaManager.getAllRemoteIdentityProviderEntities(spRealm);
            ArrayList<String> trustedRemoteIdPs = new ArrayList<String>();
            
            for ( String idp : allRemoteIdPs )
            {
                if ( metaManager.isTrustedProvider(spRealm, 
                    spEntityId, idp) ) {
                    trustedRemoteIdPs.add(idp);
                }
            }

            if ( trustedRemoteIdPs.size() == 0 )
            {
                // Misconfiguration!
                throw new WSFederationException(
                    WSFederationUtils.bundle.getString("noIDPConfigured"));
            }
            else if ( trustedRemoteIdPs.size() == 1 )
            {
                idpEntityId = trustedRemoteIdPs.get(0);
            }
        }

        FederationElement idp = null;
        if ( idpEntityId != null )
        {
            idp = metaManager.getEntityDescriptor(spRealm,
                idpEntityId);
        }
        
        // Set LB cookie here so it's done regardless of which redirect happens
        // We want response to come back to this instance
        WSFederationUtils.sessionProvider.setLoadBalancerCookie(request, response);

        // If we still don't know the IdP, redirect to home realm discovery
        if (idp == null) {
            if (StringUtils.isEmpty(homeRealmDiscoveryService)) {
                debug.error("Invalid Home Realm Discovery Service specified");
                throw new WSFederationException("invalidHomeRealmDiscoveryService");
            }
            StringBuffer url = new StringBuffer(homeRealmDiscoveryService);
            url.append("?wreply=");
            url.append(urlEncodeQueryParameterNameOrValue(request.getRequestURL().toString()));
            if (wctx != null) {
                url.append("&wctx=");
                url.append(urlEncodeQueryParameterNameOrValue(wctx));
            }
            if (debug.isDebugEnabled()) {
                debug.debug(classMethod + 
                    "no account realm - redirecting to :" + url);
            }
            response.sendRedirect(url.toString());
            return;
        }
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"account realm:" + idpEntityId);
        }

        String endpoint = CollectionUtils.getFirstItem(metaManager.getTokenIssuerEndpoints(idp));
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"endpoint:" + endpoint);
        }
        if (StringUtils.isEmpty(endpoint)) {
            debug.error("Invalid Token Issuer Endpoint specified");
            throw new WSFederationException(WSFederationUtils.bundle.getString("invalidTokenIssuerEndpoint"));
        }
        String replyURL = CollectionUtils.getFirstItem(metaManager.getTokenIssuerEndpoints(sp));
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"replyURL:" + replyURL);
        }
        StringBuffer url = new StringBuffer(endpoint);
        url.append("?wa=");
        url.append(urlEncodeQueryParameterNameOrValue(WSFederationConstants.WSIGNIN10));
        if ( wctx != null )
        {
            url.append("&wctx=");
            url.append(urlEncodeQueryParameterNameOrValue(wctx));
        }
        if (StringUtils.isNotEmpty(replyURL)) {
            url.append("&wreply=");
            url.append(urlEncodeQueryParameterNameOrValue(replyURL));
        }
        url.append("&wct=");
        url.append(urlEncodeQueryParameterNameOrValue(DateUtils.toUTCDateFormat(newDate())));
        url.append("&wtrealm=");
        url.append(urlEncodeQueryParameterNameOrValue(spIssuerName));
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod+"Redirecting to:" + url);
        }
        response.sendRedirect(url.toString());
    }
}
