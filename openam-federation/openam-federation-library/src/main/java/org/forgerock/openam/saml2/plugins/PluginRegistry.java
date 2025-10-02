/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2022-2025 ForgeRock AS.
 */

package org.forgerock.openam.saml2.plugins;


import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ADAPTER_ENV;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.forgerock.openam.saml2.plugins.InitializablePlugin.HOSTED_ENTITY_ID;
import static org.forgerock.openam.saml2.plugins.InitializablePlugin.REALM;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;

/**
 * A registry that creates and caches all the SAML plugins.
 */
public class PluginRegistry {

    private static final Logger debug = LoggerFactory.getLogger(PluginRegistry.class);


    /**
     * SAML Plugin Cache
     */
    private static LoadingCache<PluginKey, SAMLPlugin> cache = newBuilder().build(new CacheLoader<>() {
        @Override
        public SAMLPlugin load(PluginKey key) throws Exception {
            return key.create();
        }
    });

    /**
     * Returns the SAML Plugin Instance
     *
     * @param key The plugin key
     * @return The SAML Plugin
     * @throws SAML2Exception when the plugin instance can't be created
     */
    public static SAMLPlugin get(PluginKey key) throws SAML2Exception {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            debug.error("Error while creating an instance of the SAML plugin {}", key.pluginClassName);
            throw new SAML2Exception(e);
        }
    }

    /**
     * Create the instance of new key.
     *
     * @param realm The realm of the entity provider
     * @param entityId The entity provider id
     * @param type The plugin type
     * @param pluginClassName The plugin class name
     * @return The new plugin key
     */
    public static PluginKey newKey(String realm, String entityId, Class type, String pluginClassName) {
        return new PluginKey(realm, entityId, type, pluginClassName,
                new HashMap<>(Map.of(HOSTED_ENTITY_ID, entityId, REALM, realm)));
    }


    /**
     * A plugin key that is capable of creating the plugin.
     *
     * @param <T> The plugin type
     */
    private static class PluginKey<T extends SAMLPlugin> {

        final String realm;
        final String entityId;
        final String pluginClassName;
        final Class type;
        final Map<String, String> initParams;

        /**
         * Creates an instance of the PluginKey.
         * @param realm The realm the SAML entity belongs to
         * @param entityId The SAML entity Id
         * @param type The plugin type
         * @param pluginClassName The fully qualified plugin class name
         * @param initParams The params used for initialising the plugin
         */
        private PluginKey(String realm, String entityId, Class type,
                String pluginClassName, Map<String, String> initParams) {
            this.realm = realm;
            this.entityId = entityId;
            this.type = type;
            this.pluginClassName = pluginClassName;
            this.initParams = initParams;
        }

        /**
         * Creates the Plugin.
         *
         * @return The plugin
         * @throws SAML2Exception When the plugin creation fails
         */
        T create() throws SAML2Exception {
            try {
                T adapter = (T) Class.forName(pluginClassName).newInstance();
                if (InitializablePlugin.class.isAssignableFrom(adapter.getClass())) {
                    if (SPAdapter.class.isAssignableFrom(adapter.getClass())) {
                        List<String> entityAttributes
                                = SAML2Utils.getAllAttributeValueFromSSOConfig(realm, entityId, SP_ROLE, SP_ADAPTER_ENV);
                        initParams.putAll(parseEntityAttributes(entityAttributes));
                    }
                    ((InitializablePlugin)adapter).initialize(initParams);
                    ((InitializablePlugin)adapter).initialize(entityId, realm);
                }
                if (debug.isDebugEnabled()) {
                    debug.debug("create new SAML plugin "
                            + pluginClassName + " for " + entityId + " under realm " + realm);
                }
                return adapter;
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                debug.error("Error while creating the plugin class instance.", e);
                throw new SAML2Exception(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PluginKey)) return false;
            PluginKey<?> pluginKey = (PluginKey<?>) o;
            return realm.equals(pluginKey.realm) && entityId.equals(pluginKey.entityId)
                    && pluginClassName.equals(pluginKey.pluginClassName) && type.equals(pluginKey.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realm, entityId, pluginClassName, type);
        }

        /**
         * Returns map based on A/V pair.
         */
        private static Map<String, String> parseEntityAttributes(List<String> list) {
            if (isEmpty(list)) {
                return emptyMap();
            }
            return list.stream() 
                    .filter(val -> isNotBlank(val))
                    .filter(val -> val.contains("="))
                    .map(val -> val.split("=", 2))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
        }
    }
}
