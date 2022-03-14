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
 * Copyright 2020-2022 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.secret.manager;

import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.secrets.config.KeyStoreSecretStore;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * A class responsible for managing the secret mappings when the saml2 entity is modified or deleted.
 */
public class Saml2EntitySecretMappingManager implements ServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(Saml2EntitySecretMappingManager.class);
    private static final String SERVICE_NAME = "sunFMSAML2MetadataService";
    private static final String SERVICE_VERSION = "1.0";

    private final ServiceConfigManagerFactory configManagerFactory;
    private final SecretMappingManagerHelper helper;
    private final RealmLookup realmLookup;

    /**
     * Creates an instance of the Saml2EntitySecretMappingManager.
     *
     * @param configManagerFactory The config manager factory.
     * @param helper The mapping manager helper instance.
     * @param realmLookup Helper class that provides realm lookup utility functions.
     */
    @Inject
    public Saml2EntitySecretMappingManager(ServiceConfigManagerFactory configManagerFactory,
            SecretMappingManagerHelper helper, RealmLookup realmLookup) {
        this.configManagerFactory = configManagerFactory;
        this.helper = helper;
        this.realmLookup = realmLookup;
    }

    /**
     * Register for SAML2 entity config updates.
     */
    void registerServiceListener() {
        getServiceConfigManager().addListener(this);
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        //no-op
    }

    @Override
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        //no-op
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName,
            String groupName, String serviceComponent, int type) {
        String realm = realmLookup.convertRealmDnToRealmPath(orgName);
        logger.debug("organizationConfigChanged called for realm: {}, orgName: {}, serviceName: {}, version: {}",
                realm, orgName, serviceName, version);
        if (type != MODIFIED && type != REMOVED) {
            return;
        }
        try {
            Set<String> secretIdIdentifiers = helper.getAllEntitySecretIdIdentifiers(realm);
            Set<KeyStoreSecretStore> stores = helper.getRealmStores(realm);
            for (KeyStoreSecretStore store : stores) {
                Multiple<PurposeMapping> mappings = store.mappings();
                deleteUnusedMappings(realm, secretIdIdentifiers, store, mappings);
            }
        } catch (SAML2MetaException e) {
            logger.error("Failed to get the entity configs for the realm {}", realm, e);
        }
    }

    private void deleteUnusedMappings(String realm, Set <String> secretIdIdentifiers,
            KeyStoreSecretStore store, Multiple <PurposeMapping> mappings) {
        if (mappings != null) {
            try {
                mappings.idSet().stream()
                        .filter(id -> helper.isUnusedSecretMapping(secretIdIdentifiers, id))
                        .forEach(id -> helper.deleteSecretMapping(mappings, id));
            } catch (SMSException | SSOException e) {
                logger.error("Failed to read mapping ids for the store {} in realm {}", store.id(), realm, e);
            }
        }
    }

    private ServiceConfigManager getServiceConfigManager() {
        try {
            return configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION);
        } catch (SMSException | SSOException e) {
            throw new IllegalStateException("Unable to create saml2 metadata service", e);
        }
    }
}
