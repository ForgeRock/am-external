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
 * $Id: MultiProtocolUtils.java,v 1.4 2009/03/20 21:06:32 weisun2 Exp $
 *
 * Portions Copyrighted 2018-2019 ForgeRock AS.
 */
package com.sun.identity.multiprotocol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;

/**
 * The <code>MultiProtocolUtils</code> class provides some utility methods.
 *
 */
public class MultiProtocolUtils {

    private static final Logger logger = LoggerFactory.getLogger(MultiProtocolUtils.class);

    /** Creates a new instance of MultiProtocolUtils */
    private MultiProtocolUtils() {
    }
    
    /**
     * Returns base URL for the server, the URL contains the protocol, server
     * name, server port and deployment URI. For example:
     * http://www.sun.com:80/amserver
     * @param request HttpServlet request object.
     * @return URL string containing the server protocol, name, port and 
     * deployment uri.
     */
    public static String geServerBaseURL(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf("/", 1);
        if (index != -1) {
            uri = uri.substring(0, index);
        }
        return request.getScheme() + "://" + request.getServerName() + ":" +
            request.getServerPort() + uri;
    }

    /**
     * Updates session property (<code>SingleLogoutManager.FEDERATION_PROTOCOLS
     *  </code>) with the new protocol information.
     * @param session Session Object to be updated
     * @param protocol Name of the Federation protocol to be added.
     */
    public static void addFederationProtocol(Object session, String protocol) {
        if (logger.isDebugEnabled()) {
            logger.debug("MPUtils.addFedProtocol:"
                + " protocol=" + protocol + ", session=" + session);
        }
        try {
            SessionProvider provider = SessionManager.getProvider();
            String[] values = provider.getProperty(session, 
                SingleLogoutManager.FEDERATION_PROTOCOLS);
            if (logger.isDebugEnabled()) {
                logger.debug("MPUtils.addFedProtocol:"
                + " current protocols=" + Arrays.toString(values));
            }
            if ((values == null) || (values.length == 0)) {
                values = new String[] {protocol};
                provider.setProperty(session, 
                    SingleLogoutManager.FEDERATION_PROTOCOLS, values);
            } else {
                Set set = new HashSet();
                for (int i = 0; i < values.length; i++) {
                    set.add(values[i]);
                }
                if (!set.contains(protocol)) {
                    set.add(protocol);
                    String[] newVals = new String[set.size()];
                    set.toArray(newVals);
                    provider.setProperty(session,
                    SingleLogoutManager.FEDERATION_PROTOCOLS, newVals);
                }
           }
        } catch (UnsupportedOperationException ex) {
            logger.warn("MPUtils.addFedProtocol", ex);
        } catch (SessionException ex) {
            logger.warn("MPUtils.addFedProtocol2", ex);
        }
    }
    
        /**
     * Updates session property (<code>SingleLogoutManager.FEDERATION_PROTOCOLS
     *  </code>) with the new protocol information.
     * @param session Session Object to be updated
     * @param protocol Name of the Federation protocol to be added.
     */
    public static void removeFederationProtocol(Object session, 
        String protocol) {
        if (logger.isDebugEnabled()) {
            logger.debug("MPUtils.removeFedProtocol:"
                + " protocol=" + protocol + ", session=" + session);
        }
        try {
            SessionProvider provider = SessionManager.getProvider();
            String[] values = provider.getProperty(session, 
                SingleLogoutManager.FEDERATION_PROTOCOLS);
            if (logger.isDebugEnabled()) {
                logger.debug("MPUtils.removeFedProtocol:"
                + " current protocols=" + Arrays.toString(values));
            }
            if ((values == null) || (values.length == 0)) {
                return;
            } else {
                Set set = new HashSet();
                for (int i = 0; i < values.length; i++) {
                    set.add(values[i]);
                }
                set.remove(protocol);
                String[] newVals = new String[set.size()];
                set.toArray(newVals);
                provider.setProperty(session,
                    SingleLogoutManager.FEDERATION_PROTOCOLS, newVals);
            }
        } catch (UnsupportedOperationException ex) {
            logger.warn("MPUtils.addFedProtocol", ex);
        } catch (SessionException ex) {
            logger.warn("MPUtils.addFedProtocol2", ex);
        }
    }
    
    /**
     * Returns true if the session is used in other federation protocols.
     * @param request HttpServlet object
     * @param protocol Protocol of the caller. Value is one of the following:
     *   <code>SingleLogoutManager.SAML2</code>
     *   <code>SingleLogoutManager.WS_FED</code>
     * @return true if the session is used in other federation protocols, 
     *  false otherwise.
     */
    public static boolean isMultipleProtocolSession(HttpServletRequest request,
        String protocol){
        try {
            SessionProvider provider = SessionManager.getProvider();
            Object session = provider.getSession(request);
            return isMultipleProtocolSession(session, protocol);
        } catch (SessionException ex) {
            logger.debug("MPUtils.isMPSession?", ex);
            return false;
        }
    }
            
    /**
     * Returns true if the session is used in other federation protocols.
     * @param session Session object
     * @param protocol Protocol of the caller. Value is one of the following:
     *   <code>SingleLogoutManager.SAML2</code>
     *   <code>SingleLogoutManager.WS_FED</code>
     * @return true if the session is used in other federation protocols, 
     *  false otherwise.
     */
    public static boolean isMultipleProtocolSession(Object session,
        String protocol) {
        logger.debug("MultiProtocolUtils.isMPSession");
        if ((session == null) || (protocol == null)) {
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("MultiProtocolUtils.isMPSession:"
                + " protocol=" + protocol + ", session=" + session);
        }
        try {
            SessionProvider provider = SessionManager.getProvider();
            String[] vals = provider.getProperty(session, 
                SingleLogoutManager.FEDERATION_PROTOCOLS);
            if ((vals != null) && logger.isDebugEnabled()) {
                logger.debug(
                         "MultiProtocolUtils.isMPSession: size=" + vals.length);
                for (int i = 0; i < vals.length; i++) {
                    logger.debug(
                        "MultiProtocolUtils.isMPSession: protocols=" + vals[i]);
                }
            }
            if ((vals == null) || (vals.length == 0)) {
                return false;
            } else if (vals.length > 1) {
                return true;
            } else if (protocol.equals(vals[0])) {
                return false;
            } else {
                return true;
            }
        } catch (SessionException ex) {
            logger.debug("MPUtils.isMPSession", ex);
        } catch (UnsupportedOperationException ex) {
            logger.debug("MPUtils.isMPSession2", ex);
        }
        return false;
    }
    
    /**
     * Returns true if the session is used in the specified federation protocol.
     * @param request HttpServlet object
     * @param protocol Protocol of the caller. Value is one of the following:
     *   <code>SingleLogoutManager.SAML2</code>
     *   <code>SingleLogoutManager.WS_FED</code>
     * @return true if the session is used in this federation protocol, 
     *  false otherwise.
     */
    public static boolean usedInProtocol(HttpServletRequest request,
        String protocol){
        try {
            SessionProvider provider = SessionManager.getProvider();
            Object session = provider.getSession(request);
            return usedInProtocol(session, protocol);
        } catch (SessionException ex) {
            logger.debug("MPUtils.usedInProtocol?", ex);
            return false;
        }
    }
            
    /**
     * Returns true if the session is used in the specified federation protocol.
     * @param session Session object
     * @param protocol Protocol of the caller. Value is one of the following:
     *   <code>SingleLogoutManager.SAML2</code>
     *   <code>SingleLogoutManager.WS_FED</code>
     * @return true if the session is used in this federation protocol, 
     *  false otherwise.
     */
    public static boolean usedInProtocol(Object session,
        String protocol) {
        logger.debug("MultiProtocolUtils.usedInProtocol");
        if ((session == null) || (protocol == null)) {
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("MultiProtocolUtils.usedInProto:"
                + " protocol=" + protocol + ", session=" + session);
        }
        try {
            SessionProvider provider = SessionManager.getProvider();
            String[] vals = provider.getProperty(session, 
                SingleLogoutManager.FEDERATION_PROTOCOLS);
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "MultiProtocolUtils.usedInProtocol: protocols=" + Arrays.toString(vals));
            }
            if ((vals != null) && (vals.length != 0)) {
                for (int i = 0; i < vals.length; i++) {
                    if (protocol.equals(vals[i])) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SessionException ex) {
            logger.debug("MPUtils.usedInProtocol", ex);
        } catch (UnsupportedOperationException ex) {
            logger.debug("MPUtils.usedInProtocol", ex);
        }
        return false;
    }

    /**
     * Invalidates session for a specific protocol.  
     * This method invaldates the session if it is not used in any other
     * federation protocol, otherwise modifies session property to remove
     * the sepcified protocol from the session. 
     * @param session the session object to be invalidated.
     * @param request HttpServletRequest object. 
     * @param response HttpServletResponse object. 
     * @param protocol the federaion protocol to be checked.
     */
    public static void invalidateSession(Object session, 
        HttpServletRequest request, 
        HttpServletResponse response,
        String protocol) throws SessionException {
        SessionProvider provider = SessionManager.getProvider();
        if (!isMultipleProtocolSession(session, protocol)) {
            provider.invalidateSession(session, request, response);
        } else {
            removeFederationProtocol(session, protocol);
        }
    }

    /**
     * Returns logout status in string form.
     * @param status Single Logout Status. Possible values:
     *         <code>LOGOUT_SUCCEEDED_STATUS</code> - single logout succeeded.
     *         <code>LOGOUT_FAILED_STATUS</code>    - single logout failed.
     *         <code>LOGOUT_PARTIAL_STATUS</code>   - single logout partially 
     *                                                succeeded.
     *         <code>LOGOUT_REDIRECTED_STATUS</code> - single logout request 
     *                                                redirected.
     *         <code>LOGOUT_NO_ACTION_STATUS</code>  - single loglout not
     *                                                 performed.
     * @return single logout status in string form. Possible values:
     *         <code>IFSConstants.LOGOUT_SUCCESS<code>,
     *         <code>IFSConstants.LOGOUT_FAILURE</code>
     */
    public static String getLogoutStatus(int status) {
        switch (status) {
            case SingleLogoutManager.LOGOUT_FAILED_STATUS:
            case SingleLogoutManager.LOGOUT_PARTIAL_STATUS:
                return IFSConstants.LOGOUT_FAILURE;
            default:
                return IFSConstants.LOGOUT_SUCCESS;
        }
    }
}
