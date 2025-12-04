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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oauth.secrets;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.forgerock.openam.shared.secrets.Labels.OIDC_RELIANT_PARTY_CONFIG_TRUST_STORE;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.setup.SetupListener;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * This class is responsible for cleaning up the orphan trust store secret mappings when
 * a social provider OIDC Client configuration is deleted.
 */
public class OidcRpTrustStoreSecretMappingCleaner implements ServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(OidcRpTrustStoreSecretMappingCleaner.class);
    private static final String SERVICE_NAME = "SocialIdentityProviders";
    private static final String SERVICE_VERSION = "1.0";
    private final ServiceConfigManagerFactory configManagerFactory;
    private final RealmLookup realmLookup;
    private final Helper helper;

    /**
     * Creates an instance of the OIDCReliantPartyTrustStoreSecretMappingManager.
     *
     * @param configManagerFactory The config manager factory.
     * @param realmLookup A class that provides realm lookup utility functions.
     * @param helper A utility class that wraps the methods mainly to help with unit testing.
     */
    @Inject
    public OidcRpTrustStoreSecretMappingCleaner(ServiceConfigManagerFactory configManagerFactory,
           RealmLookup realmLookup, Helper helper) {
        this.configManagerFactory = configManagerFactory;
        this.realmLookup = realmLookup;
        this.helper = helper;
    }

    /**
     * Register for SocialIdentityProvider config updates.
     */
    void registerServiceListener() {
        getServiceConfigManager().addListener(this);
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        //no-op
    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName,
            String serviceComponent, int type) {
        //no-op
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName,
            String groupName, String serviceComponent, int type) {

        String realm = realmLookup.convertRealmDnToRealmPath(orgName);
        logger.debug("organizationConfigChanged called for realm: {}, orgName: {}, serviceName: {}, version: {}",
                realm, orgName, serviceName, version);
        if (type != REMOVED) {
            return;
        }

        Set<SecretStoreWithMappings> stores = helper.getRealmStores(realm);
        if (isEmpty(stores)) {
            return;
        }
        String configName = getConfigName(serviceComponent);
        for (SecretStoreWithMappings store : stores) {
            Multiple<PurposeMapping> mappings = store.mappings();
            if (mappings == null) {
                return;
            }
            Optional<String> secretId  = helper.getSecretId(realm, configName, store, mappings);
            secretId.ifPresent(id -> helper.deleteMapping(mappings, id, store.id(), realm));
        }
    }

    private String getConfigName(String serviceComponent) {
        int componentSlash = serviceComponent.lastIndexOf('/');
        if (componentSlash == -1) {
            // this case is unlikely given how the configuration is organised.
            // but leaving this in place to make sure nothing fall through the hoop
            // if things change in the future
            return serviceComponent;
        } else {
            return serviceComponent.substring(componentSlash + 1);
        }
    }


    private ServiceConfigManager getServiceConfigManager() {
        try {
            return configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION);
        } catch (SMSException | SSOException e) {
            throw new IllegalStateException("Unable to create SocialIdentityProviders service", e);
        }
    }


    /**
     * Helper class that wrap the functions to help with unit testing.
     */
    static class Helper {

        private final AnnotatedServiceRegistry serviceRegistry;

        /**
         * Creates an instance of the Helper.
         *
         * @param serviceRegistry A registry for all annotated service configurations.
         */
        @Inject
        Helper(AnnotatedServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        Set<SecretStoreWithMappings> getRealmStores(String realm) {
            try {
                return serviceRegistry.getRealmInstances(SecretStoreWithMappings.class, Realms.of(realm));
            } catch (SSOException | SMSException | RealmLookupException e) {
                logger.warn("Failed to get the realm secret stores for the realm {}", realm, e);
                return Collections.emptySet();
            }
        }

        Optional<String> getSecretId(String realm, String configName,
                                     SecretStoreWithMappings store, Multiple<PurposeMapping> mappings) {
            try {
                return mappings.idSet()
                        .stream()
                        .filter(id -> id.equalsIgnoreCase(String.format(OIDC_RELIANT_PARTY_CONFIG_TRUST_STORE,
                                configName)))
                        .findFirst();
            } catch (SMSException | SSOException e) {
                logger.warn("Failed to read mapping ids for the store {} in realm {}", store.id(), realm, e);
                return Optional.empty();
            }
        }

        void deleteMapping(Multiple<PurposeMapping> mappings, String secretId, String storeId, String realm) {
            try {
                mappings.delete(secretId);
            } catch (SMSException | SSOException e) {
                logger.warn("Failed to delete unused secretId {} from the store {} in realm {}",
                        secretId, storeId, realm, e);
            }
        }
    }

    /**
     * A listener that will be notified when the setup is complete that will register a social provider service listener
     * which is responsible for removing the orphan trust store secret mappings when a social provider OIDC Client
     * configuration is deleted.
     */
    public static class OidcRpTrustStoreSecretMappingCleanerRegister implements SetupListener {

        @Override
        public void setupComplete() {
            InjectorHolder.getInstance(OidcRpTrustStoreSecretMappingCleaner.class).registerServiceListener();
        }
    }
}
