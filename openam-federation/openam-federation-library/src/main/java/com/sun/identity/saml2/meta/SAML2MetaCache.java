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
 * $Id: SAML2MetaCache.java,v 1.4 2008/07/08 01:08:43 exu Exp $
 *
 * Portions Copyrighted 2010-2019 ForgeRock AS.
 */
package com.sun.identity.saml2.meta;

import java.util.Hashtable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;

/**
 * The <code>SAML2MetaCache</code> provides metadata cache.
 */
class SAML2MetaCache
{
    private static Logger debug = LoggerFactory.getLogger(SAML2MetaCache.class);

    private static Hashtable<CacheKey, EntityDescriptorElement> descriptorCache = new Hashtable<>();
    private static Hashtable<CacheKey, EntityConfigElement> configCache = new Hashtable<>();

    private SAML2MetaCache() {
    }

    /**
     * Returns the standard metadata entity descriptor under the realm from
     * cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>EntityDescriptorElement</code> for the entity or null
     *         if not found. 
     */
    static EntityDescriptorElement getEntityDescriptor(String realm, String entityId) {
        CacheKey cacheKey = buildCacheKey(realm, entityId);
        EntityDescriptorElement descriptor = descriptorCache.get(cacheKey);
        if (debug.isDebugEnabled()) {
            debug.debug("SAML2MetaCache.getEntityDescriptor: cacheKey = " +
                          cacheKey + ", found = " + (descriptor != null));
        }
        return descriptor;
    }

    /**
     * Adds the standard metadata entity descriptor under the realm to cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @param descriptor <code>EntityDescriptorElement</code> for the entity. 
     */
    static void putEntityDescriptor(String realm, String entityId, EntityDescriptorElement descriptor) {
        CacheKey cacheKey = buildCacheKey(realm, entityId);
        if (descriptor != null) {
            if (debug.isDebugEnabled()) {
                debug.debug("SAML2MetaCache.putEntityDescriptor: cacheKey = " +
                    cacheKey);
            }
            descriptorCache.put(cacheKey, descriptor);
        } else {
            if (debug.isDebugEnabled()) {
                debug.debug(
                    "SAML2MetaCache.putEntityDescriptor: delete cacheEey = " +
                    cacheKey);
            }
            descriptorCache.remove(cacheKey);
            configCache.remove(cacheKey);
        }
    }

    /**
     * Returns extended entity configuration under the realm from cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>EntityConfigElement</code> object for the entity or null
     *         if not found.
     */
    static EntityConfigElement getEntityConfig(String realm, String entityId) {
        CacheKey cacheKey = buildCacheKey(realm, entityId);
        EntityConfigElement config = configCache.get(cacheKey);
        if (debug.isDebugEnabled()) {
            debug.debug("SAML2MetaCache.getEntityConfig: cacheKey = " +
			  cacheKey + ", found = " + (config != null));
        }
        return config;
    }

    /**
     * Adds extended entity configuration under the realm to cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param config <code>EntityConfigElement</code> object for the entity.
     */
    static void putEntityConfig(String realm, String entityId,
            EntityConfigElement config) {
        CacheKey cacheKey = buildCacheKey(realm, entityId);
        if (config != null) {
            if (debug.isDebugEnabled()) {
                debug.debug("SAML2MetaCache.putEntityConfig: cacheKey = " +
                    cacheKey);
            }
            configCache.put(cacheKey, config);
        } else {
            if (debug.isDebugEnabled()) {
                debug.debug(
                    "SAML2MetaCache.putEntityConfig: delete cacheKey = " +
                    cacheKey);
            }
            configCache.remove(cacheKey);
        }
    }

    /**
     * Clears cache completely.
     */
    static void clear() {
        if (debug.isDebugEnabled()) {
            debug.debug("SAML2MetaCache.clear() called");
        }
        descriptorCache.clear();
        configCache.clear();
    }

    /**
     * Build cache key for descriptorCache and configCache based on realm and
     * entity ID.
     * @param realm The realm under which the entity resides.
     * @param entityId The entity ID or the name of circle of trust.
     * @return The cache key.
     */
    private static CacheKey buildCacheKey(String realm, String entityId) {
        return new CacheKey(realm, entityId);
    }

    /**
     * CacheKey to maintain the cache locally
     */
    private static final class CacheKey {

        private final String realm;
        private final String entityId;

        private CacheKey(String realm, String entityId) {
            this.realm = realm;
            this.entityId = entityId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(realm, cacheKey.realm) &&
                    Objects.equals(entityId, cacheKey.entityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realm, entityId);
        }
    }
}
