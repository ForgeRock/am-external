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
 * $Id: IDPSessionListener.java,v 1.10 2009/09/23 22:28:31 bigfatrat Exp $
 *
 * Portions Copyrighted 2014-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * The class <code>IDPSessionListener</code> implements
 * SessionListener interface and is used for maintaining the 
 * IDP session cache.
 */

public class IDPSessionListener 
             implements SessionListener {

    private static final Logger logger = LoggerFactory.getLogger(IDPSessionListener.class);
    private static SAML2MetaManager sm = null;
    private static Logger debug = LoggerFactory.getLogger(IDPSessionListener.class);
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
    /**
     *  Constructor of <code>IDPSessionListener</code>.
     */

    public IDPSessionListener() {
    }

    /**
     *  Callback for SessionListener.
     *  It is used for cleaning up the IDP session cache.
     *  
     *  @param session The session object
     */
    public void sessionInvalidated(Object session)
    {
        String classMethod = "IDPSessionListener.sessionInvalidated: ";
        HashMap paramsMap = new HashMap();
        
        if (logger.isDebugEnabled()) {
            logger.debug(
                classMethod + "Entering ...");
        }
        if (session == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    classMethod + "Session is null.");
            }
            return;
        }
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();

            String sessionIndex = getSessionProperty(sessionProvider, session, SAML2Constants.IDP_SESSION_INDEX);
            Boolean doNotRemoveSaml2IDPSession = Boolean.parseBoolean(
                    getSessionProperty(sessionProvider, session, SAML2Constants.DO_NOT_REMOVE_SAML2_IDPSESSION));
            if (StringUtils.isBlank(sessionIndex)) {
                logger.debug("{} No sessionIndex stored in session.", classMethod);
                return;
            } else if (doNotRemoveSaml2IDPSession) {
                logger.debug("{} sessionIndex {}, is marked not to be removed. In use in upgraded session",
                        classMethod, sessionIndex);
                return;
            }
            logger.debug("{} Invalidating IDPSession with sessionIndex: {}", classMethod, sessionIndex);

            IDPSession idpSession = IDPSSOUtil.retrieveCachedIdPSession(sessionIndex);
            if (idpSession != null) {
                
                paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);

                String metaAlias = idpSession.getMetaAlias();

                String realm = SAML2Utils.
                    getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));

                String idpEntityID = sm.getEntityByMetaAlias(metaAlias);
               
                try {
                    List list = (List)idpSession.getNameIDandSPpairs();
                    for (Iterator iter = list.iterator(); iter.hasNext();) {
                        NameIDandSPpair pair = (NameIDandSPpair)iter.next();
                        String spEntityID = pair.getSPEntityID();
                        NameID nameID = pair.getNameID();

                        IDPSSOConfigElement idpConfig =
                               sm.getIDPSSOConfig(realm, idpEntityID);

                        if (idpConfig != null) {
                            List<String> idpSessionSyncList =
                               SAML2MetaUtils.getAttributes(idpConfig).
                                get(SAML2Constants.IDP_SESSION_SYNC_ENABLED);

                            if ((idpEntityID != null &&
                                spEntityID != null &&
                                idpSessionSyncList != null &&
                                idpSessionSyncList.size() != 0)) {

                                boolean idpSessionSyncEnabled =
                                     idpSessionSyncList.get(0).
                                      equals(SAML2Constants.TRUE)? true : false;
                                 // Initiate IDP SLO on IDP Idle/Max
                                 // session timeout only when the Session
                                 // Sync flag is enabled
                                if (idpSessionSyncEnabled) {
                                    if (logger.isDebugEnabled()) {
                                         logger.debug(
                                          classMethod +
                                          "IDP Session Synchronization flag " +
                                          "is enabled, initiating SLO to SP");
                                    }
                                    initiateIDPSingleLogout(sessionIndex,
                                                            metaAlias,
                                                            realm,
                                                            SAML2Constants.SOAP,
                                                            nameID,
                                                            spEntityID,
                                                            paramsMap);
                                }
                            }
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                             classMethod +
                                             "Unable to retrieve the IDP " +
                                             "config data, idpConfig is null");
                            }
                        }
                    }
                } catch (SAML2MetaException sme) {
                       logger.error(
                           "IDPSessionListener.sessionInvalidated:", sme);
                } catch (SAML2Exception se) {
                           logger.error(
                           "IDPSessionListener.sessionInvalidated:", se);
                } catch (SessionException s) {
                           logger.error(
                           "IDPSessionListener.sessionInvalidated:", s);
                }
               
                synchronized(IDPCache.idpSessionsByIndices) {
                    List list = (List)idpSession.getNameIDandSPpairs();
                    for(Iterator iter = list.iterator(); iter.hasNext();) {
                        NameIDandSPpair pair = (NameIDandSPpair)iter.next();
                        NameID nameID = pair.getNameID();
                        if (SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(
                            nameID.getFormat())) {
                            IDPCache.userIDByTransientNameIDValue.remove(
                                   nameID.getValue());
                        }
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        classMethod +
                        "IDP Session with session index " +
                        sessionIndex + " already removed.");
                }
            }

            String  sessID = sessionProvider.getSessionID(session);
            if (sessID != null) {
                if (IDPCache.idpSessionsBySessionID.get(sessID) != null) {
                    IDPCache.idpSessionsBySessionID.remove(sessID);
                    if ((agent != null) && agent.isRunning() && (saml2Svc != null)){
                        saml2Svc.setIdpSessionCount(
		            (long)IDPCache.idpSessionsBySessionID.size());
                    }
                }
           
                if (IDPCache.spSessionPartnerBySessionID.get(sessID) != null) {
                    IDPCache.spSessionPartnerBySessionID.remove(sessID);
                }
            }
            IDPSSOUtil.removeIdPSessionFromCachesAndFailoverStore(sessionIndex);

            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                   "cleaned up the IDP session cache for a session expiring or being destroyed: sessionIndex=" +
                   sessionIndex);
           }
        } catch (SessionException e) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                        classMethod + "invalid or expired session.", e);
            }
        } catch (SAML2MetaException samlme) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                        classMethod + "unable to retrieve idp entity id.",
                        samlme);
            }
        }
    }

    /**
     * Performs an IdP initiated SLO against the remote SP using SOAP binding.
     *
     * @param sessionIndex Session Index
     * @param metaAlias IDP meta alias
     * @param realm Realm
     * @param binding Binding used
     * @param nameID the NameID
     * @param spEntityID SP Entity ID
     * @param paramsMap parameters map
     * @throws SAML2MetaException If there was an error while retrieving the metadata.
     * @throws SAML2Exception If there was an error while initiating SLO.
     * @throws SessionException If there was a problem with the session.
     */
    private void initiateIDPSingleLogout(String sessionIndex, String metaAlias, String realm, String binding,
            NameID nameID, String spEntityID, Map paramsMap)
            throws SAML2MetaException, SAML2Exception, SessionException {
        SPSSODescriptorType spsso = sm.getSPSSODescriptor(realm, spEntityID);
        if (spsso == null) {
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        List<EndpointType> slosList = spsso.getSingleLogoutService();
        String location = LogoutUtil.getSLOServiceLocation(slosList, SAML2Constants.SOAP);

        if (location == null) {
            if (debug.isDebugEnabled()) {
                debug.debug("IDPSessionListener.initiateIDPSingleLogout(): Unable to synchronize sessions with SP \""
                        + spEntityID + "\" since the SP does not have SOAP SLO endpoint specified in its metadata");
            }
            return;
        }

        SPSSOConfigElement spConfig = sm.getSPSSOConfig(realm, spEntityID);

        LogoutUtil.doLogout(metaAlias, spEntityID, slosList, null, binding, null, sessionIndex, nameID, null, null,
                paramsMap, spConfig);
    }

    private String getSessionProperty(SessionProvider sessionProvider, Object session, String propertyName)
            throws SessionException {
        String[] values = sessionProvider.getProperty(session, propertyName);
        if (values == null || values.length == 0) {
            logger.debug("IDPSessionListener.getSessionProperty: No value stored in session for {}", propertyName);
            return null;
        }
        return values[0];
    }
}
