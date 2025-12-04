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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.service.datastore;

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static java.lang.Integer.parseInt;
import static java.util.Collections.singleton;
import static org.forgerock.openam.shared.secrets.Labels.EXTERNAL_DATASTORE_MTLS_CERT;
import static org.forgerock.openam.utils.SchemaAttributeUtils.stripAttributeNameFromValue;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.services.datastore.DataStoreId;

import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * Factory responsible for creating instances of {@link DataStoreConfig}s.
 */
public class DataStoreConfigFactory {

    /**
     * Creates a {@link DataStoreConfig} based on a {@link Map} of attributes and a {@link DataStoreId}.
     *
     * @param dataStoreId the {@link DataStoreId} used to build the {@link DataStoreConfig}
     * @param attributes the {@link Map} of attributes used to build the {@link DataStoreConfig}
     * @return a {@link DataStoreConfig}
     */
    public DataStoreConfig create(DataStoreId dataStoreId, Map<String, Set<String>> attributes) {
        String bindDn = getMapAttr(attributes, "bindDN");
        String bindPassword = getMapAttr(attributes, "bindPassword");
        int minimumConnectionPool = parseInt(getMapAttr(attributes, "minimumConnectionPool", "1"));
        int maximumConnectionPool = parseInt(getMapAttr(attributes, "maximumConnectionPool", "10"));
        boolean useStartTLS = getBooleanMapAttr(attributes, "useStartTLS", false);
        boolean dataStoreEnabled = getBooleanMapAttr(attributes, "dataStoreEnabled", true);
        boolean mtlsEnabled = getBooleanMapAttr(attributes, "mtlsEnabled", false);
        String secretLabelAsSecretId = generateSecretId(getMapAttr(attributes, "mtlsSecretLabel"));
        boolean affinityEnabled = getBooleanMapAttr(attributes, "affinityEnabled", false);
        boolean identityStore = getBooleanMapAttr(attributes, "identityStore", false);

        boolean useSsl = CollectionHelper.getBooleanMapAttr(attributes, "useSsl", false);
        Set<String> serverUrls = stripAttributeNameFromValue(attributes.get("serverUrls"));
        Set<LDAPURL> ldapUrls = LDAPUtils.getLdapUrls(
                LDAPUtils.convertToLDAPURLs(new LinkedHashSet<>(serverUrls)), useSsl);

        DataStoreConfig.Builder dataStoreConfigBuilder = getBuilder(dataStoreId, ldapUrls, bindDn,
                bindPassword != null ? bindPassword.toCharArray() : null, minimumConnectionPool, maximumConnectionPool,
                useSsl, useStartTLS, dataStoreEnabled, mtlsEnabled, secretLabelAsSecretId, affinityEnabled,
                identityStore);

        return dataStoreConfigBuilder.build();
    }

    /**
     * Creates a {@link DataStoreConfig} based on an {@link LDAPURL}.
     *
     * @param dataStoreConfig the current {@link DataStoreConfig} to update the {@link LDAPURL}s for
     * @param dataStoreId the {@link DataStoreId} used to build the {@link DataStoreConfig}
     * @param ldapUrl the {@link LDAPURL} based on which a {@link DataStoreConfig} gets created
     * @return a {@link DataStoreConfig} with an updated set of {@link LDAPURL}s which contains only
     * the {@link LDAPURL} provided
     */
    public DataStoreConfig create(DataStoreConfig dataStoreConfig, DataStoreId dataStoreId, LDAPURL ldapUrl) {
        DataStoreConfig.Builder dataStoreConfigBuilder = getBuilder(dataStoreId, singleton(ldapUrl),
                dataStoreConfig.getBindDN(), dataStoreConfig.getBindPassword(), dataStoreConfig.getMinConnections(),
                dataStoreConfig.getMaxConnections(), dataStoreConfig.isUseSsl(), dataStoreConfig.isStartTLSEnabled(),
                dataStoreConfig.isDataStoreEnabled(), dataStoreConfig.isMtlsEnabled(),
                dataStoreConfig.getMtlsSecretId(), dataStoreConfig.isAffinityEnabled(),
                dataStoreConfig.isIdentityStore());

        return dataStoreConfigBuilder.build();
    }

    private static DataStoreConfig.Builder getBuilder(DataStoreId dataStoreId, Set<LDAPURL> ldapUrls, String bindDn,
            char[] bindPassword, int minimumConnectionPool, int maximumConnectionPool, boolean useSsl,
            boolean useStartTLS, boolean dataStoreEnabled, boolean mtlsEnabled, String secretLabelAsSecretId,
            boolean affinityEnabled, boolean identityStore) {

        return DataStoreConfig.builder(dataStoreId)
                .withLDAPURLs(ldapUrls)
                .withBindDN(bindDn)
                .withBindPassword(bindPassword)
                .withMinimumConnectionPool(minimumConnectionPool)
                .withMaximumConnectionPool(maximumConnectionPool)
                .withUseSsl(useSsl)
                .withUseStartTLS(useStartTLS)
                .withDataStoreEnabled(dataStoreEnabled)
                .withMtlsEnabled(mtlsEnabled)
                .withMtlsSecretId(secretLabelAsSecretId)
                .withAffinityEnabled(affinityEnabled)
                .withIdentityStore(identityStore);
    }

    private String generateSecretId(String secretLabel) {
        return String.format(EXTERNAL_DATASTORE_MTLS_CERT, secretLabel);
    }

}
