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
 * $Id: AccountUtils.java,v 1.2 2008/06/25 05:47:45 qcheng Exp $
 *
 * Portions Copyrighted 2016-2020 ForgeRock AS.
 */
package com.sun.identity.saml2.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * This class <code>AccountUtils</code> is a utility class for
 * setting and retrieving the <code>SAML2<code> account federation information.
 */
public class AccountUtils {

    private static final Logger logger = LoggerFactory.getLogger(AccountUtils.class);
    private static final String DELIM = "|";
    private static final String NAMEID_INFO_ATTRIBUTE = "com.sun.identity.saml2.nameidinfo.attribute";
    private static final String NAMEID_INFO_KEY_ATTRIBUTE = "com.sun.identity.saml2.nameidinfokey.attribute";
    static SAML2MetaManager metaManager = null;
    
    static {
        try {
            metaManager= new SAML2MetaManager();
        } catch (SAML2MetaException se) {
            logger.error("Unable to obtain Meta Manager.", se);
        }
    }

    /**
     * Returns the account federation information of a user for the given 
     * identity provider and a service provider. 
     * @param userID user id for which account federation needs to be returned.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @return the account federation info object.
     *         null if the account federation does not exist.
     * @exception SAML2Exception if account federation retrieval is failed.
     */ 
    public static NameIDInfo getAccountFederation(
           String userID,
           String hostEntityID,
           String remoteEntityID) throws SAML2Exception {

        logger.debug("AccountUtils.getAccountFederation:");

        if(userID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullUserID"));
        }

        if(hostEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullHostEntityID"));
        }

        if(remoteEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullRemoteEntityID"));
        }

        try {
            Set<String> set = SAML2Utils.getDataStoreProvider().getAttribute(userID, getNameIDInfoAttribute());

            if(set == null || set.isEmpty()) {
               if(logger.isDebugEnabled()) {
                  logger.debug("AccountUtils.getAccount" +
                  "Federation : user does not have any account federations.");
               }
               return null;
            }
          
            String filter = hostEntityID + DELIM + remoteEntityID + DELIM;
            if(logger.isDebugEnabled()) {
               logger.debug("AccountUtils.getAccountFederation: "+
               " filter = " + filter + " userID = " + userID);
            }
            String info = null;

            for (String value : set) {
                if (value.startsWith(filter)) {
                    info = value;
                    break;
                }
            }
 
            if(info == null) { 
               if(logger.isDebugEnabled()) {
                  logger.debug("AccountUtils.getAccount" +
                  "Federation : user does not have account federation " +
                  " corresponding to =" + filter);
               }
               return null;
            }

            return NameIDInfo.parse(info);

        } catch (DataStoreProviderException dse) {

           logger.error("AccountUtils.readAccountFederation" +
           "Info: DataStoreProviderException", dse);
           throw new SAML2Exception(dse.getMessage());
        }
        
    }

    /**
     * Sets the account federation information to the datastore for a user.
     * @param info <code>NameIDInfo</code> object to be set.
     * @param userID user identifier for which the account federation to be set.
     * @exception SAML2Exception if any failure.
     */
    public static void setAccountFederation(NameIDInfo info, String userID) throws SAML2Exception {
        if (info == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullNameIDInfo"));
        }
        if (userID == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullUserID"));
        }

        NameIDInfoKey infoKey = info.getNameIDInfoKey();


        logger.debug("NameID info to be set: {}, infoKey to be set: {}", info.toValueString(), infoKey.toValueString());

        String filter = info.getHostEntityID() + DELIM + info.getRemoteEntityID() + DELIM;
        try {
            String nameIDInfoAttr = getNameIDInfoAttribute();
            String nameIDInfoKeyAttr = getNameIDInfoKeyAttribute();

            Map<String, Set<String>> newMappings = new HashMap<>();
            Map<String, Set<String>> existingMappings = SAML2Utils.getDataStoreProvider().getAttributes(userID,
                    ImmutableSet.of(nameIDInfoAttr, nameIDInfoKeyAttr));

            if (CollectionUtils.isEmpty(existingMappings)) {
                newMappings.putAll(convertToAttributes(info, infoKey));
            } else {
                Set<String> nameIdInfo = existingMappings.getOrDefault(nameIDInfoAttr, new HashSet<>());
                nameIdInfo.removeIf(value -> value.startsWith(filter));
                nameIdInfo.add(info.toValueString());
                newMappings.put(nameIDInfoAttr, nameIdInfo);

                Set<String> nameIdInfoKey = existingMappings.getOrDefault(nameIDInfoKeyAttr, new HashSet<>());
                nameIdInfoKey.removeIf(value -> value.startsWith(filter));
                nameIdInfoKey.add(infoKey.toValueString());
                newMappings.put(nameIDInfoKeyAttr, nameIdInfoKey);
            }

            logger.debug("Set federation mappings {} for user '{}'", newMappings, userID);

            SAML2Utils.getDataStoreProvider().setAttributes(userID, newMappings);
        } catch (DataStoreProviderException ex) {
            logger.error("An error occurred while updating NameID mapping", ex);
            throw new SAML2Exception(ex.getMessage());
        }
    }

    /**
     * Converts the provided NameIDInfo and NameIDInfoKey to a Map&lt;String, Set&lt;String&gt;&gt; structure.
     *
     * @param info The NameIDInfo.
     * @param infoKey The NameIDInfoKey.
     * @return An attribute map containing the serialized NameID data.
     * @throws SAML2Exception If there was a problem whilst creating NameIDInfoKey.
     */
    public static Map<String, Set<String>> convertToAttributes(NameIDInfo info, NameIDInfoKey infoKey)
            throws SAML2Exception {
        Map<String, Set<String>> ret = new HashMap<>();
        if (infoKey == null) {
            infoKey = info.getNameIDInfoKey();
        }
        ret.put(getNameIDInfoAttribute(), Collections.singleton(info.toValueString()));
        ret.put(getNameIDInfoKeyAttribute(), Collections.singleton(infoKey.toValueString()));

        return ret;

    }
    /**
     * Removes the account federation of a user.
     * @param info <code>NameIDInfo</code> object. 
     * @param userID user identifie for which the account federation needs to
     *               be removed.
     * @return true if the account federation is removed successfully.
     * @exception SAML2Exception if any failure.
     */
    public static boolean removeAccountFederation(
        NameIDInfo info, String userID) throws SAML2Exception {

         logger.debug("AccountUtils.removeAccountFederation:");
         if(info == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                  "nullNameIDInfo"));
         }

         if(userID == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                  "nullUserID"));
         }

         try {
             Set existingFed =  SAML2Utils.getDataStoreProvider().
                   getAttribute(userID, getNameIDInfoAttribute()); 
             Set existingInfoKey = SAML2Utils.getDataStoreProvider().
                   getAttribute(userID, getNameIDInfoKeyAttribute());

             if(existingFed == null || existingFed.isEmpty()) {
                if(logger.isDebugEnabled()) {
                   logger.debug("AccountUtils.removeAccount" +
                   "Federation: user does not have account federation infos.");
                }
                return false;
             }

 
             String infoValue = info.toValueString();
             String infoKeyValue = info.getNameIDInfoKey().toValueString();

             if(logger.isDebugEnabled()) {
                logger.debug("AccountUtils.removeAccount" +
                "Federation: info to be removed:"+ infoValue + "user="+ 
                 userID + "infoKeyValue = " + infoKeyValue);
             }
             
             if(existingFed.contains(infoValue)) {

                existingFed.remove(infoValue);
                if(existingInfoKey != null &&
                       existingInfoKey.contains(infoKeyValue)) {
                   existingInfoKey.remove(infoKeyValue);
                }

                Map map = new HashMap();
                map.put(getNameIDInfoAttribute(), existingFed);
                map.put(getNameIDInfoKeyAttribute(), existingInfoKey);
                SAML2Utils.getDataStoreProvider().setAttributes(userID, map);
                return true;
             }

             if(logger.isDebugEnabled()) {
                logger.debug("AccountUtils.removeAccount" +
                "Federation: account federation info not found.");
             }
             return false;

         } catch (DataStoreProviderException dse) {
             logger.error("SAML2Utils.removeAccountFederation: " +
             "DataStoreProviderException", dse);
             throw new SAML2Exception(dse.getMessage());
         }
    }
    /**
     * Returns the SAML2 Name Identifier Info attribute name.
     * @return the SAML2 Name Identifier Info attribute name.
     */
    public static String getNameIDInfoAttribute() {
        return SystemPropertiesManager.get(NAMEID_INFO_ATTRIBUTE, 
           SAML2Constants.NAMEID_INFO);
    }

    /**
     * Returns the SAML2 Name Identifier InfoKey attribute name.
     * @return the SAML2 Name Identifier InfoKey attribute name.
     */
    public static String getNameIDInfoKeyAttribute() {
        return SystemPropertiesManager.get(NAMEID_INFO_KEY_ATTRIBUTE,
           SAML2Constants.NAMEID_INFO_KEY);
    }
}

