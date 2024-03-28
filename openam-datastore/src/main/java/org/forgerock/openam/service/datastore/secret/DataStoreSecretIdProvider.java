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
 * Copyright 2023-2024 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore.secret;

import static org.forgerock.openam.shared.secrets.Labels.EXTERNAL_DATASTORE_MTLS_CERT;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.openam.secrets.SecretIdProvider;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;

/**
 * This provider exposes the secret IDs used by the DataStore Service.
 */
@AutoService(SecretIdProvider.class)
public class DataStoreSecretIdProvider implements SecretIdProvider {

    private static final String SERVICE_NAME = "amDataStoreService";
    private static final String SERVICE_VERSION = "1.0";
    private static final String SUB_CONFIG_NAME = "dataStoreContainer";
    private static final String MTLS_SECRET_LABEL = "mtlsSecretLabel";

    private static final String DATASTORE_KEY = "externalDataStore";
    private static final Logger logger = LoggerFactory.getLogger(DataStoreSecretIdProvider.class);

    private final ServiceConfigManagerFactory serviceConfigManagerFactory;

    /**
     * Generates the secret provider. We require the service config manager factory to be able to
     * easily look up the configurations for all external data stores.
     *
     * @param serviceConfigManagerFactory the service config manager factory
     */
    @Inject
    public DataStoreSecretIdProvider(ServiceConfigManagerFactory serviceConfigManagerFactory) {
        this.serviceConfigManagerFactory = serviceConfigManagerFactory;
    }

    @Override
    public Multimap<String, String> getGlobalMultiInstanceSecretIds(SSOToken authorizationToken) {
        return ImmutableMultimap.<String, String>builder()
                .putAll(DATASTORE_KEY, createSecretIds())
                .build();
    }

    private Set<String> createSecretIds() {
        try {
            ServiceConfig globalConfig = serviceConfigManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)
                    .getGlobalConfig("default");
            ServiceConfig containerConfig = globalConfig.getSubConfig(SUB_CONFIG_NAME);

            return containerConfig.getSubConfigNames().stream()
                    .map(subConfigName -> getSubConfig(containerConfig, subConfigName))
                    .filter(Objects::nonNull)
                    .map(ServiceConfig::getAttributes)
                    .map(attributes -> CollectionHelper.getMapAttr(attributes, MTLS_SECRET_LABEL))
                    .filter(StringUtils::isNotEmpty)
                    .map(secretLabel -> String.format(EXTERNAL_DATASTORE_MTLS_CERT, secretLabel))
                    .collect(Collectors.toSet());
        } catch (SMSException | SSOException e) {
            logger.warn("Unable to read external data store configurations; no secret labels will be available", e);
            return Collections.emptySet();
        }
    }

    private ServiceConfig getSubConfig(ServiceConfig containerConfig, String configName) {
        try {
            return containerConfig.getSubConfig(configName);
        } catch (SMSException | SSOException e) {
            logger.error(String.format("Unable to retrieve external data store configuration for [%s]", configName), e);
            return null;
        }
    }

}
