/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigFedMonitoring.java,v 1.2 2009/10/29 00:03:51 exu Exp $
 *
 * Portions Copyrighted 2011-2020 ForgeRock AS.
 */

package com.sun.identity.configuration;

import static org.forgerock.openam.utils.Time.newDate;

import java.security.AccessController;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.monitoring.FederationMonitoringSetupUtils;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SSOServerRealmFedInfo;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationType;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.UriNamedClaimTypesOfferedElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;

/**
 * This class gathers the configuration information for the
 * monitoring service, which is initially started in WebtopNaming.java
 * Configuration information can be gathered after Session services
 * have started up.
 */

public class ConfigFedMonitoring {

    private final Logger debug = LoggerFactory.getLogger(ConfigFedMonitoring.class);
    SSOToken ssoToken;
    private ArrayList realmList;

    public static final String IDENTITY_PROVIDER = "IDP";
    public static final String SERVICE_PROVIDER = "SP";
    public static final String POLICY_DECISION_POINT_DESCRIPTOR = "PDP";
    public static final String POLICY_ENFORCEMENT_POINT_DESCRIPTOR = "PEP";
    public static final String SAML_ATTRAUTHORITY = "AttrAuthority";
    public static final String SAML_AUTHNAUTHORITY = "AuthnAuthority";
    public static final String SAML_ATTRQUERY = "AttrQuery";
    public static final String AFFILIATE = "Affiliate";

    public ConfigFedMonitoring() {
    }

    /*
     *  this method is called by AMSetupServlet, when it's done
     *  configuring the OpenAM server after deployment.  it's also
     *  called by the MonitoringConfiguration load-on-startup servlet
     *  when the OpenAM server is restarted any time after being
     *  configured.  it completes the configuring of the monitoring
     *  agent with the config information that requires an SSOToken
     *  to retrieve.  there is another part of the configuration supplied
     *  to the agent by WebtopNaming.
     */
    public void configureFedMonitoring() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = newDate();
        String startDate = sdf.format(date1);
        String classMethod = "ConfigFedMonitoring.configureMonitoring: ";

        if (!MonitoringUtil.isRunning()) {
            if (debug.isWarnEnabled()) {
                debug.warn(classMethod + "monitoring is disabled");
            }
            return;
        }
        try {
            ssoToken = getSSOToken();
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Could not get proper SSOToken", ssoe);
            return;
        }

        //  now all the realms' federation configs
        getAllRealms("/");
        date1 = newDate();
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod + "\n" +
                "    Start time " + startDate + "\n" +
                "    End time = " + sdf.format(date1));
        }
    }

    private SSOToken getSSOToken() throws SSOException {
        return (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    }

    /*
     *  get the list of realms, starting from "startRealm", usu. "/".
     *  return List with realms, with leading "/".
     */
    private List getRealmsList(String startRealm) {
        String classMethod = "ConfigFedMonitoring.getRealmsList: ";
        try {
            int rlmCnt = 1;  // for startRealm
            OrganizationConfigManager orgMgr =
                new OrganizationConfigManager(ssoToken, startRealm);
            Set orgs = orgMgr.getSubOrganizationNames("*", true);
            rlmCnt += orgs.size();
            realmList = new ArrayList(rlmCnt);
            realmList.add(startRealm);
            for (Iterator it = orgs.iterator(); it.hasNext(); ) {
                String ss = "/" + (String)it.next();
                realmList.add(ss);
            }
            return(realmList);
        } catch (SMSException e) {
            debug.error(classMethod +
                "SMSException getting OrgConfigMgr: " + e.getMessage());
        }
        return (new ArrayList());
    }

    private void getAllRealms(String startRealm) {
        String classMethod = "ConfigFedMonitoring.getAllRealms: ";
        boolean skipSAML2Entities = true;  // until IDPs/SPs per realm instrum
        StringBuffer sb = new StringBuffer(classMethod);
        sb.append("orgnames starting from ").append(startRealm).append(":\n");
        sb.append("  ").append(startRealm).append("\n");

        List rList = getRealmsList(startRealm);
        try {
            CircleOfTrustManager cotmgr = new CircleOfTrustManager();
            SAML2MetaManager saml2Mgr = new SAML2MetaManager();

            for (Iterator it = rList.iterator(); it.hasNext(); ) {
                String thisRealm = (String)it.next();
                Set cots = getCOTs(thisRealm, cotmgr);
                Map s2Ents = null;
                if (!skipSAML2Entities) {
                    s2Ents = getSAML2Entities(thisRealm, saml2Mgr);
                }
                Map wsEnts = getWSFedEntities(thisRealm);

                /*
                 *  getCOTMembers(thisRealm, cot, cotmgr, cotsb)
                 *  can get the members of the COT, but there isn't
                 *  a (MIB) entry that right now.
                 */
                
                Map membMap = getCOTMembers(thisRealm, cots, cotmgr);

                SSOServerRealmFedInfo srfi =
                    new SSOServerRealmFedInfo.
                        SSOServerRealmFedInfoBuilder(thisRealm).
                        cots(cots).
                        samlv2Entities(s2Ents).
                        wsEntities(wsEnts).
                        membEntities(membMap).build();
                FederationMonitoringSetupUtils.federationConfig(srfi);
            }
        } catch (SAML2MetaException e) {
            debug.error(classMethod + "SAML2 ex: " + e.getMessage());
        } catch (COTException e) {
            debug.error(classMethod + "COT ex: " + e.getMessage());
        }
    }

    public List getWSFedRoles(String entity, String realm) {
        List roles = new ArrayList(4);
        boolean isSP = true;
        int cnt = 0;
        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager();
            if (metaManager.getIDPSSOConfig(realm,entity) != null) {
                roles.add(IDENTITY_PROVIDER);
            }
            if (metaManager.getSPSSOConfig(realm, entity) != null) {
                roles.add(SERVICE_PROVIDER);
            }
            
            //to handle dual roles specifically for WSFED
            if (roles.isEmpty()) {
                FederationType fedElem =
                    metaManager.getEntityDescriptor(realm, entity).getValue();
                if (fedElem != null) {
                    for (Iterator iter = fedElem.getAny().iterator(); 
                        iter.hasNext(); ) 
                    {
                        Object o = iter.next();
                        if (o instanceof UriNamedClaimTypesOfferedElement) {
                            roles.add(IDENTITY_PROVIDER);
                            isSP = false; 
                        } else if (o instanceof TokenIssuerEndpointElement) {
                            cnt++;
                        }
                    }
                    if ((isSP) || (cnt >1)) {  
                        roles.add(SERVICE_PROVIDER);
                    } 
                }
            }
        } catch (WSFederationMetaException e) {
            debug.warn("ConfigFedMonitoring.getWSFedRoles", e); 
        }
        return (roles != null) ? roles : Collections.EMPTY_LIST;
    }

    /*
     * This is used to determine what 'roles' a particular entity is
     * acting as. It will producs a list of role names which can then
     * be used by the calling routine for whatever purpose it needs.
     */
    private List getSAMLv2Roles(String entity, String realm) {
        List roles = new ArrayList();
        
        try {
            SAML2MetaManager samlManager = new SAML2MetaManager();
            EntityDescriptorElement d =
                samlManager.getEntityDescriptor(realm, entity);
            
            if (d != null) {
                // find out what role this dude is playing
                if (SAML2MetaUtils.getSPSSODescriptor(d) != null) {
                    roles.add(SERVICE_PROVIDER);
                }
                if (SAML2MetaUtils.getIDPSSODescriptor(d) != null) {
                    roles.add(IDENTITY_PROVIDER);
                }
                if (SAML2MetaUtils.getPolicyDecisionPointDescriptor(d) != null)
                {
                    roles.add(POLICY_DECISION_POINT_DESCRIPTOR);
                }
                if (SAML2MetaUtils.getPolicyEnforcementPointDescriptor(d) !=
                    null)
                {
                    roles.add(POLICY_ENFORCEMENT_POINT_DESCRIPTOR);
                }
                if (SAML2MetaUtils.
                        getAttributeAuthorityDescriptor(d) != null) {
                    roles.add(SAML_ATTRAUTHORITY);
                }
                if (SAML2MetaUtils.getAuthnAuthorityDescriptor(d) != null) {
                    roles.add(SAML_AUTHNAUTHORITY);
                }
                if (SAML2MetaUtils.getAttributeQueryDescriptor(d) != null) {
                    roles.add(SAML_ATTRQUERY);
                }
                if (samlManager.getAffiliationDescriptor(realm, entity) !=
                    null)
                {
                    roles.add(AFFILIATE);
                }
            }
        } catch (SAML2MetaException s) {
            if (debug.isWarnEnabled()) {
                debug.warn("ConfigFedMonitoring.getSAMLv2Roles() - " +
                    "Couldn't get SAMLMetaManager");
            }
        }
        
        return (roles != null) ? roles : Collections.EMPTY_LIST;
    }

    /**
     * This is a convenience routine that can be used
     * to convert a List of String objects to a single String in the format of
     *     "one; two; three"
     */
    private String listToString(List roleNames) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = roleNames.iterator(); i.hasNext(); ) {
            String role = (String)i.next();
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(getLocalizedString(role));
        }
        return sb.toString();
    }
    
    private String getLocalizedString(String key) {
        return key;
    }

    private Set getCOTs(String realm, CircleOfTrustManager cotmgr) {
        String classMethod = "ConfigFedMonitoring.getCOTs: ";
        
        Set cotSet = null;
        try {
            cotSet = cotmgr.getAllCirclesOfTrust(realm);
        } catch (COTException e) {
            debug.error(classMethod + "COTMgr error: " + e.getMessage());
        }
        return cotSet;
    }

    private Map getCOTMembers(String realm, Set cotNames,
        CircleOfTrustManager cotmgr)
    {
        String classMethod = "ConfigFedMonitoring.getCOTMembers: ";
        StringBuffer sb = new StringBuffer(classMethod);
        
        Map cotMap = new HashMap();
        for (Iterator it1 = cotNames.iterator(); it1.hasNext(); ) {
            String cotName = (String)it1.next();
            if (debug.isDebugEnabled()) {
                sb.append(" cotName = ").append(cotName).append("\n");
            }
            Map memMap = new HashMap();
            try {
                Set cotSAML =
                    cotmgr.listCircleOfTrustMember(realm, cotName,
                        COTConstants.SAML2);
                Set cotWSFed =
                    cotmgr.listCircleOfTrustMember(realm, cotName,
                        COTConstants.WS_FED);
                memMap.put("SAML", cotSAML);
                memMap.put("WSFed", cotWSFed);

                cotMap.put(cotName, memMap);

                if (debug.isDebugEnabled()) {
                    sb.append("    SAMLv2 members: ");
                    if ((cotSAML != null) && (cotSAML.size() > 0)) {
                        for (Iterator it = cotSAML.iterator(); it.hasNext(); ) {
                            sb.append("      ").append((String)it.next()).
                                append("\n");
                        }
                    } else {
                        sb.append("none\n");
                    }

                    sb.append("    WSFed members: ");
                    if ((cotWSFed != null) && (cotWSFed.size() > 0)) {
                        for (Iterator it = cotWSFed.iterator(); it.hasNext(); ){
                            sb.append("      ").append((String)it.next()).
                                append("\n");
                        }
                    } else {
                        sb.append("none\n");
                    }
                }
            } catch (COTException cx) {
                debug.error(classMethod + "COTException: " + cx.getMessage());
            }
        }
        if (debug.isDebugEnabled()) {
            debug.error(sb.toString());
        }
        return cotMap;
    }

    private Map getSAML2Entities (String realm, SAML2MetaManager saml2Mgr) {
        String classMethod = "ConfigFedMonitoring.getSAML2Entities:";
        Set s2Ents = null;
        // s2entMap: entity name => Map of ("location", "roles") -> values
        Map s2entMap = new HashMap();  // for the SAML2 entities
        try {
            s2Ents = saml2Mgr.getAllEntities(realm);
            List hosted = saml2Mgr.getAllHostedEntities(realm);
            for (Iterator it = s2Ents.iterator(); it.hasNext(); ) {
                Map wse = new HashMap();
                String entId = (String)it.next();
                if ((hosted != null) && hosted.contains(entId)) {
                    wse.put("location", "hosted");
                } else {
                    wse.put("location", "remote");
                }
                wse.put("roles", listToString(getSAMLv2Roles(entId, realm)));
                s2entMap.put(entId, wse);
            }
        } catch (SAML2MetaException e) {
            debug.error(classMethod +
                "getting SAML2 entity providers for realm " + realm + ": " +
                e.getMessage());
        }
        return s2entMap;
    }

    private Map getWSFedEntities (String realm) {
        String classMethod = "ConfigFedMonitoring.getWSFedEntities:";
        Set wsEnts = null;
        // wsentMap: entity name => Map of ("location", "roles") -> values
        Map wsentMap = new HashMap();
        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager();
            wsEnts = metaManager.getAllEntities(realm);
            List hosted = metaManager.getAllHostedEntities(realm);
            for (Iterator it = wsEnts.iterator(); it.hasNext(); ) {
                Map wse = new HashMap();
                String entId = (String)it.next();
                if ((hosted != null) && hosted.contains(entId)) {
                    wse.put("location", "hosted");
                } else {
                    wse.put("location", "remote");
                }
                wse.put("roles", listToString(getWSFedRoles(entId, realm)));
                wsentMap.put(entId, wse);
            }
        } catch (WSFederationMetaException e) {
            debug.error(classMethod + "getting WSFed entities for realm " +
                realm + ": " + e.getMessage());
        }
        return wsentMap;
    }
}
