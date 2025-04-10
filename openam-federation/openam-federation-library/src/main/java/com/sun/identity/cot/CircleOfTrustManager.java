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
 * $Id: CircleOfTrustManager.java,v 1.13 2009/10/28 23:58:56 exu Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */
package com.sun.identity.cot;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;

import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.saml2.meta.SAML2COTUtils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.wsfederation.meta.WSFederationCOTUtils;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;

/**
 * This class has methods to manage the circle of trust.
 */
public class CircleOfTrustManager {
    
    private static final String SUBCONFIG_ID = "cot";
    private static final int SUBCONFIG_PRIORITY = 0;
    
    private static ConfigurationInstance configInstStatic;
    private static Logger debug = LoggerFactory.getLogger(CircleOfTrustManager.class);
    
    private ConfigurationInstance configInst;
    private Object callerSession = null;

    static {
        try {
            configInstStatic = ConfigurationManager.getConfigurationInstance(
                    COTConstants.COT_CONFIG_NAME);
        } catch (ConfigurationException ce) {
            debug.error(
                    "COTManager.static: Unable to get COT service config",ce);
            configInstStatic = null;
        }
        if (configInstStatic != null) {
            try {
                configInstStatic.addListener(new COTServiceListener());
            } catch (ConfigurationException ce) {
                debug.error("COTManager.static: Unable to add " +
                        "ConfigurationListener for COT service.",ce);
            }
        }
    }
    
    /**
     * Constructor for <code>COTManager</code>.
     *
     * @throws COTException if unable to construct <code>COTManager</code>.
     */
    public CircleOfTrustManager() throws COTException {
        configInst = configInstStatic;
        if (configInst == null) {
            throw new COTException("nullConfig", null);
        }
    }
    
    /**
     * Constructor for <code>COTManager</code>.
     *
     * @param callerToken session token of the caller
     * @throws COTException if unable to construct <code>COTManager</code>.
     */
    public CircleOfTrustManager(Object callerToken) throws COTException {
        try {
            configInst = ConfigurationManager.getConfigurationInstance(
                COTConstants.COT_CONFIG_NAME, callerToken);
            callerSession = callerToken;
        } catch (ConfigurationException ce) {
            throw new COTException("nullConfig", null);
        }
    }
    
    /**
     * Creates a circle of trust.
     *
     * @param realm the realm under which the circle of trust will be created.
     * @param cotDescriptor the circle of trust descriptor object to be created.
     * @throws COTException if unable to create the circle of trust.
     */
    public void createCircleOfTrust(String realm,
            CircleOfTrustDescriptor cotDescriptor)
            throws COTException {
        String classMethod = "COTManager.createCircleOfTrust: ";
        if (cotDescriptor == null) {
            throw new COTException("nullCot", null);
        }
        String entityId = null;
        
        if (realm == null) {
            realm = "/";
        }
        
        String name = cotDescriptor.getCircleOfTrustName();
        if ((name == null) || (name.trim().length() == 0)) {
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                    LogUtil.NO_COT_NAME_CREATE_COT_DESCRIPTOR,data);
            throw new COTException("invalidCOTName", null);
        }
        
        if (getAllCirclesOfTrust(realm).contains(name)) {
            debug.error(classMethod + "Circle of trust already exists" + name);
            String[] data = { name, realm };
            LogUtil.error(Level.INFO,
                    LogUtil.COT_EXISTS_CREATE_COT_DESCRIPTOR,data);
            
            throw new COTException("cotExists",data);
        }
        
        Map attrs = cotDescriptor.getAttributes();
        // Filter out the entityid which does not exist in the system
        Map tpMap = checkAndSetTrustedProviders(realm, cotDescriptor);
        
        // update the extended entity config
        if (tpMap != null) {
            updateEntityConfig(realm, name, COTConstants.SAML2, 
                (Set) tpMap.get(COTConstants.SAML2));
            updateEntityConfig(realm, name, COTConstants.WS_FED, 
                (Set) tpMap.get(COTConstants.WS_FED));
        }
        
        // create the cot node
        try {
            configInst.createConfiguration(realm, name, attrs);
            if (debug.isDebugEnabled()) {
                debug.debug( classMethod + "circle of trust is created.");
            }
            String[] data = {name, realm};
            LogUtil.access(Level.INFO,LogUtil.COT_DESCRIPTOR_CREATED,data);
        } catch (ConfigurationException e) {
            debug.error(classMethod, e);
            String[] data = { e.getMessage(), name, realm };
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_CREATE_COT_DESCRIPTOR,data);
            throw new COTException(e);
        }
    }
    
    /**
     * This method filters out invalid entities from the trusted provider list.
     */
    private Map checkAndSetTrustedProviders(String realm, 
            CircleOfTrustDescriptor cotDescriptor) throws COTException {
        Set tp = cotDescriptor.getTrustedProviders();
        Map map = null;
        if ((tp != null) && !tp.isEmpty()) { 
            map = COTUtils.trustedProviderSetToProtocolMap(tp, realm);
            retainValidEntityIDs(map, COTConstants.SAML2, realm);
            retainValidEntityIDs(map, COTConstants.WS_FED, realm);
        }
        return map;
    }

    /**
     * Retains only valid entity ID for a specific protocol
     */
    private void retainValidEntityIDs(Map map, String protocol, String realm) 
        throws COTException {
        Set pSet = (Set) map.get(protocol);
        if ((pSet != null) && !pSet.isEmpty()) {
            Set entityIds = getAllEntities(realm, protocol);     
            if ((entityIds == null) || entityIds.isEmpty())  {
                // no valid entity exists for this protocol, clear the map
                map.remove(protocol);
            } else {
                if (!entityIds.containsAll(pSet)) {
                    if (entityIds.retainAll(pSet)) {
                        if (debug.isDebugEnabled()) {
                            debug.debug("COTDescriptor.retainValidEntityIDs:" 
                                + " Following entity id: "
                                + entityIds + " are valid and will be added to "
                                + "the circle of trust");
                        }
                        map.put(protocol, entityIds);
                    }
                }
            }
        }
    }
    
    /**
     * Modifies the attributes of a circle of trust.
     *
     * @param realm the realm the circle of trust is in.
     * @param cotDescriptor circle of trust descriptor that contains
     *        the new set of attributes
     * @throws COTException if unable to modify the circle of trust.
     */
    public void modifyCircleOfTrust(String realm,
            CircleOfTrustDescriptor cotDescriptor)
            throws COTException {
        String classMethod = "COTManager.modifyCircleOfTrust :";
        if (cotDescriptor == null) {
            throw new COTException("nullCot", null);
        }
        
        if (realm == null) {
            realm = "/";
        }
        
        String name = cotDescriptor.getCircleOfTrustName();
        isValidCOTName(realm,name);
        
        try {
            Map attrs = cotDescriptor.getAttributes();
            configInst.setConfiguration(realm, name, attrs);
        } catch (ConfigurationException e) {
            debug.error(classMethod, e);
            String[] data = { e.getMessage(), name, realm };
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_MODIFY_COT_DESCRIPTOR,data);
            
            throw new COTException(e);
        }
    }
    
    /**
     * Returns a set of names of all circle of trusts.
     *
     * @param realm The realm under which the circle of trust resides.
     * @return Set of names of all circle of trusts.
     * @throws COTException if unable to read circle of trust.
     */
    public Set<String> getAllCirclesOfTrust(String realm) throws COTException {
        Set valueSet = null;
        Set<String> cotSet = new HashSet<>();
        if (realm == null) {
            realm = COTConstants.ROOT_REALM;
        }
        String classMethod = "COTManager.getAllCircleOfTrust: ";
        try {
            valueSet = configInst.getAllConfigurationNames(realm);
            if ((valueSet != null) && !valueSet.isEmpty()) {
                for (Iterator iter = valueSet.iterator(); iter.hasNext(); ) {
                    String name = (String) iter.next();
                    cotSet.add(name);
                }
            }
        } catch (ConfigurationException e) {
            debug.error(classMethod, e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_GET_ALL_COT_DESCRIPTOR,data);
            throw new COTException(e);
        }
        return cotSet;
    }
    
    
    /**
     * Checks if the federation protocol type is valid.
     *
     * @param protocolType the federation protocol to be checked.
     * @retrun true if the protocol type if valid.
     * @throws COTException if the circle of trust type is not valid.
     */
    boolean isValidProtocolType(String protocolType) throws COTException {
        String classMethod = "COTManager:isValidProtocolType";
        
        if (!COTUtils.isValidProtocolType(protocolType)) {
            debug.error(classMethod +"Invalid protocol Type " + protocolType);
            String[] data = { protocolType };
            throw new COTException("invalidProtocolType", data);
        }
        return true;
    }
    
    /**
     * Returns a set of entity identities based on the circle of
     * trust type SAML2, or WS_FED
     *
     * @param realm the realm name.
     * @param type the protocol type.
     * @throws COTException if the circle of trust type is invalid.
     */
    Set getAllEntities(String realm, String type) throws COTException {
        Set entityIds = Collections.EMPTY_SET;
        if (type != null) {
            if (type.equalsIgnoreCase(COTConstants.SAML2)) {
                entityIds = getSAML2Entities(realm);
            } else if (type.equalsIgnoreCase(COTConstants.WS_FED)) {
                entityIds = getWSFedEntities(realm);
            } else {
                String[] data = { type };
                throw new COTException("invalidProtocolType",data);
            }
        }
        return entityIds;
    }
    
    /**
     * Returns a set of all SAML2 identifiers
     */
    Set getSAML2Entities(String realm) throws COTException {
        try {
            SAML2MetaManager saml2MetaMgr = null;
            if (callerSession != null) {
                saml2MetaMgr = new SAML2MetaManager(callerSession);
            } else {
                saml2MetaMgr = new SAML2MetaManager();
            }
            return saml2MetaMgr.getAllEntities(realm);
        } catch (SAML2MetaException sme) {
            throw new COTException(sme);
        }
    }
    
    /**
     * Returns a set of all WSFED identifiers
     */
    Set getWSFedEntities(String realm) throws COTException {
        try {
            WSFederationMetaManager wsfedMetaMgr = null;
            if (callerSession != null) {
                wsfedMetaMgr = new WSFederationMetaManager(callerSession);
            } else {
                wsfedMetaMgr = new WSFederationMetaManager();
            }
            return wsfedMetaMgr.getAllEntities(realm);
        } catch (WSFederationMetaException sme) {
            throw new COTException(sme);
        }
    }

    /**
     * Updates the trusted providers list in the entity configuration.
     * The Circle of Trust type determines whether the entiry is a
     * SAML2, or WS_FED provider.
     *
     * @param realm the realm in which the entity configuration is in.
     * @param cotName the name of the circle of trust.
     * @param protocolType the federation protocol type, SAML2, or WS_FED.
     * @param trustedProviders a set of trusted provider identifiers to
     *        be updated in the entity configuration.
     * @throws COTException if there is an error updating the entity
     *         configuration.
     */
    void updateEntityConfig(String realm, String cotName, String protocolType,
            Set trustedProviders) throws COTException {
        if (protocolType.equalsIgnoreCase(COTConstants.SAML2)) {
            updateSAML2EntityConfig(realm,cotName,trustedProviders);
        } else if (protocolType.equalsIgnoreCase(COTConstants.WS_FED)) {
            updateWSFedEntityConfig(realm,cotName,trustedProviders);
        } else {
            String[] args = { protocolType };
            throw new COTException("invalidProtocolType",args);
        }
    }
    
    /**
     * Updates the entity configuration.
     * The Circle of Trust type determines whether the entity is a
     * SAML2, or WS_FED provider.
     *
     * @param realm the realm in which the entity configuration is in.
     * @param cotName the name of the circle of trust.
     * @param protocolType the federation protocol type, WS_FED or SAML2.
     * @param entityID the entity identifier.
     * @throws COTException if there is an error updating the entity
     *         configuration.
     */
    void updateEntityConfig(String realm, String cotName, String protocolType, 
        String entityID) throws COTException,JAXBException {
        if (protocolType.equalsIgnoreCase(COTConstants.SAML2)) {
            try {
                (new SAML2COTUtils(callerSession)).updateEntityConfig(
                    realm,cotName,entityID);
            } catch (SAML2MetaException idffe) {
                throw new COTException(idffe);
            }
        } else if (protocolType.equalsIgnoreCase(COTConstants.WS_FED)) {
            try {
                (new WSFederationCOTUtils(callerSession)).updateEntityConfig(
                    realm,cotName, entityID);
            } catch (WSFederationMetaException e) {
                throw new COTException(e);
            }
        } else {
            String[] args = { protocolType };
            throw new COTException("invalidProtocolType",args);
        }
    }
    
    /**
     * Remove circle of trust from the entity configuration.
     *
     * @param realm the realm name.
     * @param cotName the circle of trust name.
     * @param protocolType the federation protocol type.
     * @param entityID the entity identifier to be updated.
     * @throws COTException if there is  error updating entity configuration.
     * @throws JAXBException if there is error retrieving entity configuration.
     */
    void removeFromEntityConfig(
        String realm,
        String cotName,
        String protocolType,
        String entityID
    ) throws COTException, JAXBException {
        if (protocolType.equalsIgnoreCase(COTConstants.SAML2)) {
            try {
                (new SAML2COTUtils(callerSession)).removeFromEntityConfig(
                    realm,cotName, entityID);
            } catch (SAML2MetaException sme) {
                throw new COTException(sme);
            }
        } else if (protocolType.equalsIgnoreCase(COTConstants.WS_FED)) {
            try {
                (new WSFederationCOTUtils(callerSession)).removeFromEntityConfig
                    (realm, cotName, entityID);
            } catch (WSFederationMetaException e) {
                throw new COTException(e);
            }
        } else {
            String[] data = { protocolType };
            throw new COTException("invalidProtocolType",data);
        }
    }
    
    /**
     * Updates the SAML2 Entity Configuration.
     *
     * @param realm the realm name.
     * @param cotName the circle of trust name.
     * @param trustedProviders set of trusted provider names.
     * @throws COTException if there is an error updating the configuration.
     */
    void updateSAML2EntityConfig(String realm,String cotName,
            Set trustedProviders) throws COTException {
        String classMethod = "COTManager:updateSAML2EntityConfig";
        String entityId = null;
        SAML2COTUtils saml2CotUtils= new SAML2COTUtils(callerSession);
        if (trustedProviders != null && !trustedProviders.isEmpty()) {
            for (Iterator iter = trustedProviders.iterator();
            iter.hasNext();) {
                entityId = (String) iter.next();
                try {
                    saml2CotUtils.updateEntityConfig(realm,cotName,
                            entityId);
                } catch (SAML2MetaException sme) {
                    throw new COTException(sme);
                } catch (JAXBException e) {
                    debug.error(classMethod, e);
                    String[] data = {e.getMessage(),cotName,entityId,realm};
                    LogUtil.error(Level.INFO,
                            LogUtil.CONFIG_ERROR_CREATE_COT_DESCRIPTOR,
                            data);
                    throw new COTException(e);
                }
            }
        }
    }
    
    /**
     * Updates the WSFederation Entity Configuration.
     *
     * @param realm the realm name.
     * @param cotName the circle of trust name.
     * @param trustedProviders set of trusted provider names.
     * @throws COTException if there is an error updating the configuration.
     */
    void updateWSFedEntityConfig(String realm,String cotName,
            Set trustedProviders) throws COTException {
        String classMethod = "COTManager:updateWSFedEntityConfig";
        String entityId = null;
        WSFederationCOTUtils wsfedCotUtils= new WSFederationCOTUtils(
            callerSession);
        if (trustedProviders != null && !trustedProviders.isEmpty()) {
            for (Iterator iter = trustedProviders.iterator();
            iter.hasNext();) {
                entityId = (String) iter.next();
                try {
                    wsfedCotUtils.updateEntityConfig(realm,cotName,
                            entityId);
                } catch (WSFederationMetaException sme) {
                    throw new COTException(sme);
                } catch (JAXBException e) {
                    debug.error(classMethod, e);
                    String[] data = {e.getMessage(),cotName,entityId,realm};
                    LogUtil.error(Level.INFO,
                            LogUtil.CONFIG_ERROR_CREATE_COT_DESCRIPTOR,
                            data);
                    throw new COTException(e);
                }
            }
        }
    }
    
    /**
     * Adds entity identifier to a circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust will be
     *              modified.
     * @param cotName the name of the circle of trust.
     * @param protocolType the federation protcol type the entity supports.
     * @param entityId the entity identifier.
     * @throws COTException if unable to add member to the
     *         circle of trust.
     */
    public void addCircleOfTrustMember(String realm, String cotName,
            String protocolType, String entityId)
            throws COTException {
        addCircleOfTrustMember(realm, cotName, protocolType, entityId, true);
    }

    /**
     * Adds entity identifier to a circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust will be
     *              modified.
     * @param cotName the name of the circle of trust.
     * @param protocolType the federation protcol type the entity supports.
     * @param entityId the entity identifier.
     * @param addToEntityConfig if true, add the cotname to the entity config.
     * @throws COTException if unable to add member to the
     *         circle of trust.
     */
    public void addCircleOfTrustMember(String realm, String cotName,
            String protocolType, String entityId, boolean addToEntityConfig)
            throws COTException {
        String classMethod = "COTManager.addCircleOfTrustMember: ";
        if (realm == null) {
            realm = "/";
        }
        if ((cotName == null) || (cotName.trim().length() == 0)) {
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                    LogUtil.NULL_COT_NAME_ADD_COT_DESCRIPTOR,data);
            throw new COTException("invalidCOTName", null);
        }
        if ((entityId == null) || (entityId.trim().length() == 0)) {
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                    LogUtil.NULL_ENTITYID_ADD_COT_DESCRIPTOR,data);
            throw new COTException("invalidEntityID", null);
        }
        try {

            CircleOfTrustDescriptor cotDesc = getCircleOfTrust(realm, cotName);

            // add the cot to the entity config descriptor
            if (addToEntityConfig) {
                updateEntityConfig(realm, cotName, protocolType, entityId);
            }
            if (cotDesc.add(entityId, protocolType)) {
                modifyCircleOfTrust(realm, cotDesc);
                debug.debug("{}entityid '{}' circle of trust {} in Realm {} added", classMethod, entityId,
                        cotName, realm);
            } else {
                debug.debug("{}entityid '{}' circle of trust {} in Realm {} already found added", classMethod,
                        entityId, cotName, realm);
            }
        } catch (JAXBException jbe) {
            String[] data = { jbe.getMessage(),cotName,entityId, realm};
            debug.error("{}{}", classMethod, LogUtil.CONFIG_ERROR_ADD_COT_MEMBER, Arrays.toString(data), jbe);
            LogUtil.error(Level.INFO, LogUtil.CONFIG_ERROR_ADD_COT_MEMBER, data);
            throw new COTException(jbe);
        }
    }
    
    /**
     * Removes entity from circle of trust under the realm.
     *
     * @param realm the realm to which the circle of trust belongs.
     * @param cotName  the circle of trust name.
     * @param protocolType the federation protocol type.
     * @param entityId the entity identifier.
     * @throws COTException if there is an error removing entity from the
     *         circle of trust.
     */
    public void removeCircleOfTrustMember(String realm,String cotName,
            String protocolType, String entityId)
            throws COTException {
        removeCircleOfTrustMember(realm, cotName, protocolType, entityId, true);
    }

    /**
     * Removes entity from circle of trust under the realm.
     *
     * @param realm the realm to which the circle of trust belongs.
     * @param cotName  the circle of trust name.
     * @param protocolType the federation protocol type.
     * @param entityId the entity identifier.
     * @param rmEntityConfig flag indicate needs to remove cot from 
     *         entity config or not.
     * @throws COTException if there is an error removing entity from the
     *         circle of trust.
     */
    public void removeCircleOfTrustMember(String realm,String cotName,
            String protocolType, String entityId, boolean rmEntityConfig)
            throws COTException {
        String classMethod = "COTManager.removeCircleOfTrustMember: ";
        if (StringUtils.isBlank(cotName)) {
            String[] data = { cotName, realm };
            LogUtil.error(Level.INFO,
                    LogUtil.NULL_COT_NAME_REMOVE_COT_MEMBER,data);
            throw new COTException("invalidCOTName", null);
        }
        if (StringUtils.isBlank(entityId)) {
            String[] data = {cotName, entityId, realm };
            LogUtil.error(Level.INFO,
                    LogUtil.NULL_ENTITYID_REMOVE_COT_MEMBER,data);
            throw new COTException("invalidEntityID", null);
        }
        
        if (realm == null) {
            realm = COTConstants.ROOT_REALM;
        }
        
        try {
            // Remove the cot from the cotlist attribute in
            // the entity config.
            if (rmEntityConfig) {
                removeFromEntityConfig(realm, cotName, protocolType, entityId);
            }
            
            // Remove entity id from the cot
            CircleOfTrustDescriptor cotDesc;
            Map attrs = configInst.getConfiguration(realm, cotName);
            if (attrs == null) {
                cotDesc = new CircleOfTrustDescriptor(cotName, realm, COTConstants.ACTIVE);
            } else {
                cotDesc = new CircleOfTrustDescriptor(cotName, realm, attrs);
            }

            if (cotDesc.remove(entityId, protocolType)) {
                modifyCircleOfTrust(realm, cotDesc);
                COTCache.invalidate(realm, cotName);
                debug.debug("{}entityid '{}' in the circle of trust {} in Realm {} removed", classMethod, entityId,
                        cotName, realm);
            } else {
                debug.debug("{}entityid '{}' in the circle of trust {} in Realm {} not found for remove", classMethod,
                        entityId, cotName, realm);
            }
        } catch (ConfigurationException | JAXBException e) {
            String[] data = { e.getMessage(), cotName, entityId, realm };
            debug.error("{}{}", classMethod, LogUtil.CONFIG_ERROR_REMOVE_COT_MEMBER, Arrays.toString(data), e);
            LogUtil.error(Level.INFO, LogUtil.CONFIG_ERROR_REMOVE_COT_MEMBER,data);
            throw new COTException(e);
        }
    }
    
    /**
     * Lists trusted providers in a circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust will be
     *              modified.
     * @param cotName the name of the circle of trust
     * @param protocolType the federation protocol for the entities.
     * @return Set of trusted providers or null if no member in the
     *                circle of trust
     * @throws COTException if unable to list member in the
     *         circle of trust.
     */
    public Set listCircleOfTrustMember(String realm, String cotName,
            String protocolType) throws COTException {
        String classMethod = "COTManager.listCircleOfTrustMember: ";
        if ((cotName == null) || (cotName.trim().length() == 0)) {
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                    LogUtil.NULL_COT_NAME_LIST_COT,data);
            throw new COTException("invalidCOTName", null);
        }
        if (realm == null) {
            realm = "/";
        }
        Set trustedProviders = new HashSet();
        try {
            CircleOfTrustDescriptor cotDesc = null;
            Map attrs = configInst.getConfiguration(realm, cotName);
            if (attrs == null) {
                return null;
            } else {
                isValidProtocolType(protocolType);
                cotDesc = new CircleOfTrustDescriptor(cotName,  realm, attrs);
                trustedProviders = cotDesc.getTrustedProviders(protocolType);
            }
        } catch (ConfigurationException e) {
            debug.error(classMethod, e);
            String[] data = { e.getMessage(), cotName, realm};
            LogUtil.error(Level.INFO,LogUtil.CONFIG_ERROR_LIST_COT_MEMBER,data);
            throw new COTException(e);
        }
        return trustedProviders;
    }
    
    /**
     * Deletes the circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param cotName Name of the circle of trust.
     * @throws COTException if unable to delete the circle of trust.
     */
    public void deleteCircleOfTrust(String realm, String cotName)
    throws COTException {
        String classMethod = "COTManager.deleteCircleOfTrust:" ;
        if (realm == null) {
            realm = "/";
        }
        String[] data = { cotName, realm };
        
        isValidCOTName(realm,cotName);

        try {
            Set trustProviders = null;
            Map attrs = configInst.getConfiguration(realm, cotName);
            
            if (attrs != null) {
                CircleOfTrustDescriptor cotDesc=
                        new CircleOfTrustDescriptor(cotName, realm, attrs);
                
                trustProviders = cotDesc.getTrustedProviders();
            }
            if (attrs == null || trustProviders == null ||
                    trustProviders.isEmpty()) {
                configInst.deleteConfiguration(realm, cotName, null);
                LogUtil.access(Level.INFO,LogUtil.COT_DESCRIPTOR_DELETED,data);
            } else {
                debug.error(classMethod + "Delete circle of trust" +
                        " is not allowed since it contains members.");
                LogUtil.error(Level.INFO,
                        LogUtil.HAS_ENTITIES_DELETE_COT_DESCRIPTOR,data);
                String[] args = { cotName , realm };
                throw new COTException("deleteCOTFailedHasMembers", args);
            }
        } catch (ConfigurationException e) {
            debug.error(classMethod, e);
            String[] args = { e.getMessage(), cotName, realm };
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_DELETE_COT_DESCRIPTOR,args);
            throw new COTException(e);
        }
    }

    /**
     * Returns the circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param name Name of the circle of trust.
     * @return <code>SAML2CircleOfTrustDescriptor</code> containing the
     * attributes of the given CircleOfTrust.
     * @throws COTException if unable to retrieve the circle of trust.
     */
    public CircleOfTrustDescriptor getCircleOfTrust(String realm, String name) throws COTException {
        return getCircleOfTrust(realm, name, true);
    }

    /**
     * Returns the circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param name Name of the circle of trust.
     * @param useCache use cache first.
     * @return <code>SAML2CircleOfTrustDescriptor</code> containing the
     * attributes of the given CircleOfTrust.
     * @throws COTException if unable to retrieve the circle of trust.
     */
    public CircleOfTrustDescriptor getCircleOfTrust(String realm, String name, boolean useCache)
        throws COTException {
        String classMethod = "COTManager.getCircleOfTrust :";
        if (realm == null) {
            realm = "/";
        }
        isValidCOTName(realm, name);
        
        String[] data = { name, realm };
        
        CircleOfTrustDescriptor cotDesc = COTCache.getCircleOfTrust(realm,name);
        if (cotDesc != null && useCache) {
            LogUtil.access(Level.FINE, LogUtil.COT_FROM_CACHE, data);
        } else {
            try {
                Map attrs = configInst.getConfiguration(realm, name);
                if (attrs == null) {
                    cotDesc = new CircleOfTrustDescriptor(name, realm,
                            COTConstants.ACTIVE);
                } else {
                    cotDesc = new CircleOfTrustDescriptor(name, realm, attrs);
                }
                
                COTCache.putCircleOfTrust(realm, name, cotDesc);
                LogUtil.access(Level.INFO,LogUtil.COT_DESCRIPTOR_RETRIEVED,
                        data);
            } catch (ConfigurationException e) {
                debug.error(classMethod, e);
                data[0] = e.getMessage();
                data[1] = name;
                data[2] = realm;
                LogUtil.error(Level.INFO,
                        LogUtil.CONFIG_ERROR_GET_COT_DESCRIPTOR,data);
                throw new COTException(e);
            }
        }
        return cotDesc;
    }
    
    /**
     * Returns a set of names of all active circle of trusts.
     *
     * @param realm The realm under which the circle of trust resides.
     * @return Set of names of all active circle of trusts.
     * @throws COTException if the names of
     *         circle of trusts cannot be read.
     */
    public Set getAllActiveCirclesOfTrust(String realm)
    throws COTException {
        String classMethod = "COTManager.getAllActiveCirclesOfTrust: ";
        Set activeAuthDomains = new HashSet();
        
        try {
            Set valueSet = configInst.getAllConfigurationNames(realm);
            
            if ((valueSet != null) && !valueSet.isEmpty()) {
                for (Iterator iter = valueSet.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    Map attrMap = configInst.getConfiguration(realm, name);
                    
                    if (COTUtils.getFirstEntry(attrMap,
                        COTConstants.COT_STATUS).equalsIgnoreCase(
                            COTConstants.ACTIVE)) {
                        activeAuthDomains.add(name);
                    }
                }
            }
        } catch (ConfigurationException se) {
            debug.error(classMethod, se);
            String[] data = { se.getMessage(), realm };
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_GET_ALL_ACTIVE_COT,data);
            throw new COTException(se);
        }
        return activeAuthDomains;
    }
    
    
    /**
     * Determines if entity is in the circle of trust under the realm.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param name Name of the Circle of Trust.
     * @param protocolType the federation protocol type of the entity.
     * @param entityId the entity identifier.
     * @return true if the entity is in the specified circle of trust
     */
    public boolean isInCircleOfTrust(String realm, String name, 
            String protocolType, String entityId) {
        Set tProviders = new HashSet();
        try {
            CircleOfTrustDescriptor cotd = getCircleOfTrust(realm, name);
            Set pSet = cotd.getTrustedProviders(protocolType);
            return ((pSet != null) && !pSet.isEmpty() 
                && pSet.contains(entityId));
        } catch (Exception me) {
            debug.error("COTManager.isInCircleOfTrust", me);
            String[] data = {me.getMessage(), name, entityId , realm };
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_RETREIVE_COT,
                    data);
        }
        return false;
    }
    
    /**
     * Validates the circle of trust name.
     *
     * @param realm the realm the circle of trust is in.
     * @param name the circle of trust name.
     * @return true if circle of trust name is valid.
     * @throws <code>COTException</code> if the circle of trust name is invalid.
     */
    boolean isValidCOTName(String realm,String name) throws COTException {
        String classMethod = "COTManager.isValidCOTName: ";
        if ((name == null) || (name.trim().length() == 0) ||
                !getAllCirclesOfTrust(realm).contains(name)) {
            
            debug.error(classMethod + "invalid circle of trust name :" + name);
            String[] data = {realm , name};
            LogUtil.error(Level.INFO,LogUtil.INVALID_COT_NAME,data);
            throw new COTException("invalidCOTName", data);
        }
        return true;
    }
    
    
    /**
     * Checks if circle of trust status is active.
     */
    boolean isActiveCOT(String cotStatus) {
        return (cotStatus != null &&
                cotStatus.equalsIgnoreCase(COTConstants.ACTIVE));
    }

}
