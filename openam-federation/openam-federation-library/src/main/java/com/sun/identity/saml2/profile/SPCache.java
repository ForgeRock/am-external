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
 * $Id: SPCache.java,v 1.17 2009/06/09 20:28:32 exu Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.profile;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;


/**
 * This class provides the memory store for SAML request and response information on Service Provider side.
 */

public class SPCache {

    private static final Logger logger = LoggerFactory.getLogger(SPCache.class);
    public static int interval = SAML2Constants.CACHE_CLEANUP_INTERVAL_DEFAULT;
    public static boolean isFedlet = false;

    private static final String FEDLET_CLASS = "com.sun.identity.plugin.configuration.impl.FedletConfigurationImpl";

    static {
        // if interval is manually configured and if it's less than minimum of 5 mins, then set it to minimum.
        // minimum is defined via service config 'CacheCleanupInterval' value. See serverAttributeMap.properties
        interval = SystemPropertiesManager.getAsInt(SAML2Constants.CACHE_CLEANUP_INTERVAL, SAML2Constants.CACHE_CLEANUP_INTERVAL_DEFAULT);
        if (interval < SAML2Constants.CACHE_CLEANUP_INTERVAL_MINIMUM) {
            logger.debug("SPCache.constructor: Cleanup interval shouldn't be less than {} seconds."
                            + " Setting to minimum value.", SAML2Constants.CACHE_CLEANUP_INTERVAL_MINIMUM);
            interval = SAML2Constants.CACHE_CLEANUP_INTERVAL_MINIMUM;
        } 
        // use the configuration implementation class to determine
        // if this is Fedlet, this could be done using a dedicate property
        // in the future 
        String configClass = SystemPropertiesManager.get("com.sun.identity.plugin.configuration.class");
        if (StringUtils.isNotEmpty(configClass) && configClass.trim().equals(FEDLET_CLASS)){
            isFedlet = true;
        } 
    }
    
    private SPCache() {
    }

    /**
     * Map saves data on whether the account was federated.
     * Key   :   A unique key String value
     * Value : String representing boolean val
     */
    final public static PeriodicCleanUpMap fedAccountHash = new PeriodicCleanUpMap(
            interval * 1000, interval * 1000);

    /**
     * Map saves response data so that it can be retrieved and used for Post Authentication Processing classes.
     * Key   :   A unique key String value
     * Value : Saml2ResponseData object
     */
    final public static PeriodicCleanUpMap samlResponseDataHash = new PeriodicCleanUpMap(
            interval * 1000, interval * 1000);

    /**
     * Map saves the request info.
     * Key   :   requestID String
     * Value : AuthnRequestInfo object
     */
    final public static PeriodicCleanUpMap requestHash = new PeriodicCleanUpMap(
        interval * 1000, interval * 1000); 

    /**
     * Map saves the MNI request info.
     * Key   :   requestID String
     * Value : ManageNameIDRequestInfo object
     */
    final protected static PeriodicCleanUpMap mniRequestHash = new PeriodicCleanUpMap(
        interval * 1000, interval * 1000);

    /**
     * Map to save the relayState URL.
     * Key  : a String the relayStateID 
     * Value: a String the RelayState Value 
     */
    final public static PeriodicCleanUpMap relayStateHash= new PeriodicCleanUpMap(
        interval * 1000, interval * 1000);

    /**
     * Hashtable stores information required for LogoutRequest consumption.
     * key : String NameIDInfoKey (NameIDInfoKey.toValueString())
     * value : List of SPFedSession's
     *       (SPFedSession - idp sessionIndex (String)
     *                     - sp token id (String)                     
     * one key --- multiple SPFedSession's
     */
    final public static Hashtable fedSessionListsByNameIDInfoKey = new Hashtable();

    /**
     * SP: used to map LogoutRequest ID and inResponseTo in LogoutResponse
     * element to the original LogoutRequest object
     * key : request ID (String)
     * value : original logout request object  (LogotRequest)
     */
    final public static PeriodicCleanUpMap logoutRequestIDHash =
        new PeriodicCleanUpMap(interval * 1000, interval * 1000);

    /**
     * Map saves response info for local auth.
     * Key: requestID String
     * Value: ResponseInfo object
     */
    final protected static PeriodicCleanUpMap responseHash = new PeriodicCleanUpMap(
        interval * 1000, interval * 1000);

    /**
     * Hashtable saves AuthnContext Mapper object.
     * Key: hostEntityID+realmName
     * Value: SPAuthnContextMapper
     */
    final public static Hashtable authCtxObjHash = new Hashtable();

    /**
     * Hashtable saves AuthnContext class name and the authLevel. 
     * Key: hostEntityID+realmName
     * Value: Map containing AuthContext Class Name as Key and value
     *              is authLevel.
     */
    final public static Hashtable authContextHash = new Hashtable();

    /**
     * Hashtable saves the Request Parameters before redirecting
     * to IDP Discovery Service to retreive the preferred IDP.
     * Key: requestID a String
     * Value : Request Parameters Map , a Map
     */
    final public static PeriodicCleanUpMap reqParamHash = new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000);


    /**
     * Cache saves the assertion id.
     * Key : assertion ID String
     * Value : Constant  
     */
    final public static PeriodicCleanUpMap assertionByIDCache =
        new PeriodicCleanUpMap(interval * 1000,
        interval * 1000);

    /**
     * Hashtable saves NameID format to user profile attribute mapping
     * key  : remoteEntityID + "|" + realm
     * value: Map containing NameID format as Key and user profile
     *     attribute name as Value.
     */
    public static final Map<String, Map<String, String>> formatAttributeHash = new ConcurrentHashMap<>();

    /**
     * Clears the auth context object hash table.
     *
     */
    public static void clear() {
        if ((authCtxObjHash != null) &&
                        (!authCtxObjHash.isEmpty())) {
            authCtxObjHash.clear();
        }
        if ((authContextHash != null) && 
                        (!authContextHash.isEmpty())) {
            authContextHash.clear();
        }
        formatAttributeHash.clear();
   }
}
