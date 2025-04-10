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
 * $Id: COTCache.java,v 1.2 2008/06/25 05:46:38 qcheng Exp $
 *
 * Portions Copyrighted 2021-2025 Ping Identity Corporation.
 */
package com.sun.identity.cot;


import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class caches circle of trust data.
 */
public class COTCache {
    
    private static final Logger logger = LoggerFactory.getLogger(COTCache.class);
    private static Hashtable<String, CircleOfTrustDescriptor> cotCache = new Hashtable<>();
    private static Logger debug = LoggerFactory.getLogger(COTCache.class);
    
    /**
     * Constructor.
     */
    private COTCache() {
    }
    
    
    /**
     * Returns the circle of trust descriptor under the realm from
     * cache.
     *
     * @param realm the realm under which the circle of trust resides.
     * @param name the circle of trust name.
     * @return <code>CircleOfTrustDescriptor</code> for the circle of trust
     *        or null if not found.
     */
    static CircleOfTrustDescriptor getCircleOfTrust(String realm, String name) {
        String classMethod = "CircleOfDescriptorCache:getCircleOfTrust:";
        String cacheKey = buildCacheKey(realm, name);
        CircleOfTrustDescriptor cotDesc =
                (CircleOfTrustDescriptor)cotCache.get(cacheKey);
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "cacheKey = " + cacheKey +
                    ", found = " + (cotDesc != null));
        }
        return cotDesc;
    }
    
    /**
     * Updates the Circle of Trust Cache.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param name Name of the circle of trust.
     * @param cotDescriptor <code>COTDescriptor</code> for the
     *                circle of trust.
     */
    static void putCircleOfTrust(String realm, String name,
            CircleOfTrustDescriptor cotDescriptor) {
        String classMethod = "CircleOfTrustCache:putCircleOfTrust";
        String cacheKey = buildCacheKey(realm, name);
        
        if (debug.isDebugEnabled()) {
            debug.debug(classMethod + ": cacheKey = " + cacheKey);
        }
        cotCache.put(cacheKey, cotDescriptor);
    }

    /**
     * Invalidate the Circle of Trust Cache for a realm + name.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param name Name of the circle of trust.
     * @return old <code>CircleOfTrustDescriptor</code> for the circle of trust
     *        or null if not found.
     */
    static CircleOfTrustDescriptor invalidate(String realm, String name) {
        String classMethod = "CircleOfTrustCache:invalidateCircleOfTrust";
        String cacheKey = buildCacheKey(realm, name);
        debug.debug("{}: cacheKey = ", classMethod, cacheKey);
        return cotCache.remove(cacheKey);
    }

    /**
     * Clears the circle of trust cache.
     */
    static void clear() {
        debug.debug("CircleOfTrustCache:clear");
        cotCache.clear();
    }
    
    /**
     * Builds cache key for circle of trust cache.
     *
     * @param realm the realm to which the circle of trust belongs.
     * @param cotName the name of the circle of trust.
     * @return the cache key.
     */
    private static String buildCacheKey(String realm, String cotName) {
        return realm + "//" + cotName;
    }
}
