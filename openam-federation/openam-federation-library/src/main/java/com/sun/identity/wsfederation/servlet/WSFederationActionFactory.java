/**
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
 * $Id: WSFederationActionFactory.java,v 1.3 2008/08/27 19:00:07 superpat7 Exp $
 *
 * Portions copyright 2015-2019 ForgeRock AS.
 */

package com.sun.identity.wsfederation.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationUtils;

/**
 * This class creates WSFederationAction instances according to the parameters
 * sent via HTTP.
 */
public class WSFederationActionFactory {
    private static Logger debug = LoggerFactory.getLogger(WSFederationActionFactory.class);
    
    /*
     * Private constructor ensure that no instance is ever created
     */
    private WSFederationActionFactory() {
    }

    /**
     * Factory method creates a WSFederationAction instance according to the 
     * parameters sent via request.
     * @param request HTTPServletRequest for this interaction.
     * @param response HTTPServletResponse for this interaction.
     * @return a WSFederationAction to handle the interaction.
     */
    public static WSFederationAction createAction(HttpServletRequest request, 
        HttpServletResponse response)
    {
        String classMethod = "WSFederationActionFactory.createAction: ";
        WSFederationAction action = null;

        // If this is an Active Requestor Profile request, then we must not parse the request parameters, only the
        // request InputStream should be processed.
        if (request.getRequestURI().startsWith(request.getContextPath() + WSFederationConstants.MEX_ENDPOINT_PREFIX)) {
            return new MexRequest(request, response);
        } else if ("POST".equals(request.getMethod()) && request.getRequestURI().startsWith(request.getContextPath()
                + WSFederationConstants.STS_ENDPOINT_PREFIX)) {
            return new ActiveRequest(request, response);
        }

        String wa = request.getParameter(WSFederationConstants.WA);
        if (wa!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WA + "="+wa);
        }

        String wresult = request.getParameter(WSFederationConstants.WRESULT);
        if (wresult!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WRESULT + "=" + 
                wresult);
        }

        String whr = request.getParameter(WSFederationConstants.WHR);
        if (whr!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WHR + "="+whr);
        }

        String wtrealm = request.getParameter(WSFederationConstants.WTREALM);
        if (wtrealm!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WTREALM + "=" + 
                wtrealm);
        }

        // Accept goto or wreply for target URL
        // goto takes precendence
        String wreply = request.getParameter(SAML2Constants.GOTO);
        if ( wreply==null || wreply.length()==0 )
        {
            wreply = request.getParameter(WSFederationConstants.WREPLY);
        }

        if (!ESAPI.validator().isValidInput("HTTP URL: " + wreply, wreply, "URL", 2000, false)) {
            wreply = null;
        }

        if (!WSFederationUtils.isWReplyURLValid(request, wreply)) {
            wreply = null;
        }

        if (wreply!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WREPLY + "=" + 
                wreply);
        }

        String wct = request.getParameter(WSFederationConstants.WCT);
        if (wct!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WCT + "="+wct);
        }

        String wctx = request.getParameter(WSFederationConstants.WCTX);
        if (wctx!=null && debug.isDebugEnabled()) {
            debug.debug(classMethod + WSFederationConstants.WCTX + "="+wctx);
        }

        if ( request.getMethod().equals("GET")){
            if (request.getRequestURI().
                startsWith(request.getContextPath() + WSFederationConstants.METADATA_URL_PREFIX)) {
                // Metadata request
                action = new MetadataRequest(request, response);
            } else if (wa == null || wa.equals(WSFederationConstants.WSIGNIN10)) {
                // We allow missing wa for RP signin request to accomodate
                // agents etc
                if ( wtrealm != null && (wtrealm.length()>0)) {
                    // GET with wa == wsignin1.0 and wtrealm 
                    //     => IP signin request
                    if ( debug.isDebugEnabled() ) {
                        debug.debug(classMethod + 
                            "initiating IP signin request");
                    }
                    action = new IPSigninRequest(request, response, whr, 
                        wtrealm, wct, wctx, wreply);
                } else {                
                    // GET with wa == wsignin1.0 and no wtrealm 
                    //     => RP signin request
                    if ( debug.isDebugEnabled() ) {
                        debug.debug(classMethod + 
                            "initiating SP signin request");                
                    }

                    action = new RPSigninRequest(request, response, whr, wct, 
                        wctx, wreply);
                }                
            } else if (wa.equals(WSFederationConstants.WSIGNOUT10) ||
                wa.equals(WSFederationConstants.WSIGNOUTCLEANUP10)) {
                // GET with wa == wsignout1.0 or wsignoutcleanup1.0
                //     => signout request
                if ( debug.isDebugEnabled() ) {
                    debug.debug(classMethod + 
                        "initiating signout request");                
                }
                action = new IPRPSignoutRequest(request, response, wreply);
            }
        } else if ( request.getMethod().equals("POST")) {
            if ( wa.equals(WSFederationConstants.WSIGNIN10) && 
                wresult != null )
            {
                if ( debug.isDebugEnabled() ) {
                    debug.debug(classMethod + 
                        "initiating SP signin response");
                }
                action = new RPSigninResponse(request,response,wresult,wctx);
            }
        }

        return action;
    }
}
