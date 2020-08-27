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
 * $Id: WSFederationCOTUtils.java,v 1.5 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2018-2019 ForgeRock AS.
 */
package com.sun.identity.wsfederation.meta;

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.jaxb.entityconfig.AttributeType;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;

/**
 * <code>WSFederationCOTUtils</code> provides utility methods to update
 * the WS-Federation Entity Configuration <code>cotlist</code> attributes
 * in the Service and Identity Provider configurations.
 */
public class WSFederationCOTUtils {
    
    private static Logger debug = LoggerFactory.getLogger(WSFederationCOTUtils.class);
    private Object callerSession = null;
    
    /*
     * Constructor.
     * @param callerToken session token of the caller.
     */
    public WSFederationCOTUtils(Object callerToken)  {
       callerSession = callerToken; 
    }
    
    /**
     * Updates the entity config to add the circle of trust name to the
     * <code>cotlist</code> attribute. The Service Provider and Identity
     * Provider Configuration are updated.
     *
     * @param realm the realm name where the entity configuration is.
     * @param name the circle of trust name.
     * @param entityId the name of the Entity identifier.
     * @throws WSFederationMetaException if there is a configuration error when
     *         updating the configuration.
     * @throws JAXBException is there is an error updating the entity
     *          configuration.
     */
    public void updateEntityConfig(String realm, String name, 
        String entityId)
    throws WSFederationMetaException, JAXBException {
        String classMethod = "WSFederationCOTUtils.updateEntityConfig: ";
        WSFederationMetaManager metaManager = null;
        if (callerSession != null) {
            metaManager = new WSFederationMetaManager(callerSession);
        } else {
            metaManager = new WSFederationMetaManager();
        }
        ObjectFactory objFactory = new ObjectFactory();
        // Check whether the entity id existed in the DS
        FederationElement edes = metaManager.getEntityDescriptor(
                realm, entityId);
        if (edes == null) {
            debug.error(classMethod +"No such entity: " + entityId);
            String[] data = {realm, entityId};
            throw new WSFederationMetaException("entityid_invalid", data);
        }
        FederationConfigElement eConfig = metaManager.getEntityConfig(realm, entityId);
        if (eConfig == null) {
            AttributeType atype = objFactory.createAttributeType();
            atype.setName(SAML2Constants.COT_LIST);
            atype.getValue().add(name);
            // add to eConfig
            FederationConfigType configType = objFactory.createFederationConfigType();
            configType.setFederationID(entityId);
            configType.setHosted(false);
            List<JAXBElement<BaseConfigType>> federationConfigs = configType.getIDPSSOConfigOrSPSSOConfig();
            // Decide which role EntityDescriptorElement includes
            // Right now, it is either an SP or an IdP
            // IdP will have UriNamedClaimTypesOffered
            if (metaManager.getUriNamedClaimTypesOffered(edes) != null) {
                JAXBElement<BaseConfigType> configElement = objFactory.createIDPSSOConfigElement(
                        objFactory.createBaseConfigType());
                configElement.getValue().getAttribute().add(atype);
                federationConfigs.add(configElement);
            } else {
                JAXBElement<BaseConfigType> configElement = objFactory.createSPSSOConfigElement(
                        objFactory.createBaseConfigType());
                configElement.getValue().getAttribute().add(atype);
                federationConfigs.add(configElement);
            }
            metaManager.setEntityConfig(realm, objFactory.createFederationConfigElement(configType));
        } else {
            List<JAXBElement<BaseConfigType>> elist = eConfig.getValue().getIDPSSOConfigOrSPSSOConfig();
            for (JAXBElement<BaseConfigType> anElist : elist) {
                BaseConfigType bConfig = anElist.getValue();
                List<AttributeType> list = bConfig.getAttribute();
                boolean foundCOT = false;
                for (Object aList : list) {
                    AttributeType avp = (AttributeType) aList;
                    if (avp.getName().trim().equalsIgnoreCase(
                            SAML2Constants.COT_LIST)) {
                        foundCOT = true;
                        List<String> avpl = avp.getValue();
                        if (avpl.isEmpty() || !containsValue(avpl, name)) {
                            avpl.add(name);
                            metaManager.setEntityConfig(realm, eConfig);
                            break;
                        }
                    }
                }
                // no cot_list in the original entity config
                if (!foundCOT) {
                    AttributeType atype = objFactory.createAttributeType();
                    atype.setName(SAML2Constants.COT_LIST);
                    atype.getValue().add(name);
                    list.add(atype);
                    metaManager.setEntityConfig(realm, eConfig);
                }
            }
        }
    }
    
    private static boolean containsValue(List list, String name) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            if (((String) iter.next()).trim().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes the circle of trust name passed from the <code>cotlist</code>
     * attribute in the Entity Config. The Service Provider and Identity
     * Provider Entity Configuration are updated.
     *
     * @param realm the realm of the provider
     * @param name the circle of trust name to be removed.
     * @param entityId the entity identifier of the provider.
     * @throws WSFederationMetaException if there is an error updating the 
     * entity config.
     */
    public void removeFromEntityConfig(String realm, String name, String entityId) throws WSFederationMetaException {
        String classMethod = "WSFederationCOTUtils.removeFromEntityConfig: ";
        WSFederationMetaManager metaManager = null;
        if (callerSession != null) {
            metaManager = new WSFederationMetaManager(callerSession);
        } else {
            metaManager = new WSFederationMetaManager();
        }
        // Check whether the entity id existed in the DS
        FederationElement edes = metaManager.getEntityDescriptor(
                realm, entityId);
        if (edes == null) {
            debug.error(classMethod +"No such entity: " + entityId);
            String[] data = {realm, entityId};
            throw new WSFederationMetaException("entityid_invalid", data);
        }
        FederationConfigElement eConfig = metaManager.getEntityConfig(realm, entityId);
        if (eConfig != null) {
            List<JAXBElement<BaseConfigType>> elist = eConfig.getValue(). getIDPSSOConfigOrSPSSOConfig();
            for (JAXBElement<BaseConfigType> anElist : elist) {
                BaseConfigType bConfig = anElist.getValue();
                List<AttributeType> list = bConfig.getAttribute();
                for (AttributeType avp : list) {
                    if (avp.getName().trim().equalsIgnoreCase(
                            SAML2Constants.COT_LIST)) {
                        List avpl = avp.getValue();
                        if (avpl != null && !avpl.isEmpty() &&
                                containsValue(avpl, name)) {
                            avpl.remove(name);
                            metaManager.setEntityConfig(realm, eConfig);
                            break;
                        }
                    }
                }
            }
        }
    }
}
