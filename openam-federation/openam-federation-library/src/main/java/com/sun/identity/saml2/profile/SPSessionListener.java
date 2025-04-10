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
 * $Id: SPSessionListener.java,v 1.6 2009/09/23 22:28:32 bigfatrat Exp $
 *
 * Portions Copyrighted 2014-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;


/**
 * The class <code>SPSessionListener</code> implements
 * SessionListener interface and is used for maintaining the 
 * SP session cache.
 */

public class SPSessionListener implements SessionListener {

    private static final Logger logger = LoggerFactory.getLogger(SPSessionListener.class);
    private static SAML2MetaManager sm = null;
    private static Logger debug = LoggerFactory.getLogger(SPSessionListener.class);
    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;
    static {
        try {
            sm = new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata",sme);
        }
        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }
    
    private String infoKeyString = null;
    private String sessionID = null;

    /**
     *  Constructor of <code>SPSessionListener</code>.
     */
    public SPSessionListener(String infoKeyString, String sessionID) {
        this.infoKeyString = infoKeyString;
        this.sessionID = sessionID;
    }

    /**
     *  Callback for SessionListener.
     *  It is used for cleaning up the SP session cache.
     *  
     *  @param session The session object
     */
    public void sessionInvalidated(Object session)
    {
        String classMethod = "SPSessionListener.sessionInvalidated: ";
        HashMap paramsMap = new HashMap();
        NameIDInfoKey nameIdInfoKey = null;
        
        if (session == null || infoKeyString == null ||
            sessionID == null) {
            return;
        }
        SessionProvider sessionProvider = null;
        SPFedSession fedSession = null;

        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            return;
        }
        if (!sessionID.equals(sessionProvider.getSessionID(session)))
        {
            return;
        }
        List fedSessionList = (List)
            SPCache.fedSessionListsByNameIDInfoKey.get(infoKeyString);
        if (fedSessionList == null) {
            return;
        }

        try {
            Iterator iter = fedSessionList.iterator();
            while (iter.hasNext()) {
                fedSession = (SPFedSession) iter.next();
                if (fedSession.spTokenID.equals(sessionID)) {

                    paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);

                    String metaAlias = fedSession.metaAlias;

                    nameIdInfoKey = NameIDInfoKey.parse(infoKeyString);

                    String spEntityID = sm.getEntityByMetaAlias(metaAlias);

                    String realm =
                            SAML2Utils.getRealm(
                                SAML2MetaUtils.getRealmByMetaAlias(metaAlias));

                    SPSSOConfigElement spConfig =
                                        sm.getSPSSOConfig(realm, spEntityID);
                    if (spConfig != null) {
                        List<String> spSessionSyncList =
                            SAML2MetaUtils.getAttributes(spConfig).
                                get(SAML2Constants.SP_SESSION_SYNC_ENABLED);

                        if (spEntityID != null &&
                            spSessionSyncList != null &&
                            (spSessionSyncList.size() != 0)) {
                         
                             boolean spSessionSyncEnabled =
                                spSessionSyncList.get(0).
                                      equals(SAML2Constants.TRUE)? true : false;
                             // Initiate SP SLO on SP Idle/Max
                             // session timeout only when Session Sync flag
                             // is enabled
                             if (spSessionSyncEnabled) {
                                 if (logger.isDebugEnabled()) {
                                     logger.debug(
                                         classMethod +
                                         "SP Session Synchronization flag " +
                                         "is enabled, initiating SLO to IDP");
                                 }
                                 initiateSPSingleLogout(metaAlias,
                                                        realm,
                                                        SAML2Constants.SOAP,
                                                        nameIdInfoKey,
                                                        fedSession,
                                                        paramsMap);
                             }
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                         classMethod +
                                         "Unable to retrieve the SP config" +
                                         " data, spConfig is null");
                        }
                    }
                }
            }
        } catch (SAML2MetaException sme) {
                logger.error(
                    "SPSessionListener.sessionInvalidated:", sme);
        } catch (SAML2Exception se) {
                logger.error(
                    "SPSessionListener.sessionInvalidated:", se);
        } catch (SessionException s) {
                logger.error(
                           "IDPSessionListener.sessionInvalidated:", s);
        }
        
        synchronized (fedSessionList) {
            Iterator iter = fedSessionList.iterator();
            while (iter.hasNext()) {
                fedSession = (SPFedSession) iter.next();
                if (fedSession.spTokenID.equals(sessionID)) {
                    iter.remove();
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml2Svc != null))
                    {
                        saml2Svc.setFedSessionCount(
		            (long)SPCache.fedSessionListsByNameIDInfoKey.
				size());
                    }
                }
            }
            if (fedSessionList.isEmpty()) {
                SPCache.fedSessionListsByNameIDInfoKey.remove(infoKeyString);
            }
        }
    }

    /**
     * Performs an SP initiated SLO against the remote IdP using SOAP binding.
     *
     * @param metaAlias SP meta alias
     * @param realm Realm
     * @param binding Binding used
     * @param nameIdInfoKey the nameIdInfoKey
     * @param fedSession SP Federated session
     * @param paramsMap parameters map
     * @throws SAML2MetaException If there was an error while retrieving the metadata.
     * @throws SAML2Exception If there was an error while initiating SLO.
     * @throws SessionException If there was a problem with the session.
     */
    private static void initiateSPSingleLogout(String metaAlias, String realm, String binding,
            NameIDInfoKey nameIdInfoKey, SPFedSession fedSession, Map paramsMap)
            throws SAML2MetaException, SAML2Exception, SessionException {
        IDPSSODescriptorType idpsso = sm.getIDPSSODescriptor(realm, nameIdInfoKey.getRemoteEntityID());

        if (idpsso == null) {
            String[] data = {nameIdInfoKey.getRemoteEntityID()};
            LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        List<EndpointType> slosList = idpsso.getSingleLogoutService();
        String location = LogoutUtil.getSLOServiceLocation(slosList, SAML2Constants.SOAP);

        if (location == null) {
            if (debug.isWarnEnabled()) {
                debug.warn("SPSessionListener.initiateSPSingleLogout(): Unable to synchronize sessions with IdP \""
                        + nameIdInfoKey.getRemoteEntityID() + "\" since the IdP does not have SOAP SLO endpoint "
                        + "specified in its metadata, possibly this is a misconfiguration of the hosted SP");
            }
            return;
        }

        IDPSSOConfigElement idpConfig = sm.getIDPSSOConfig(realm, nameIdInfoKey.getRemoteEntityID());

        LogoutUtil.doLogout(metaAlias, nameIdInfoKey.getRemoteEntityID(), slosList, null, binding, null,
                fedSession.idpSessionIndex, fedSession.info.getNameID(), null, null, paramsMap, idpConfig);
    }
}
