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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.sm.SmsWrapperObject.isFbcWithoutEmbeddedEnabled;
import static org.forgerock.openam.service.datastore.DataStoreGuiceModule.SERVICE_DATA_STORE_ID_ATTRIBUTE_NAMES;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.services.datastore.DataStoreException;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConfigRetrievalException;
import org.forgerock.openam.sm.config.ConfigSource;
import org.forgerock.openam.sm.config.ConfigTransformer;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Look up service to find configured data store Ids.
 *
 * @since 6.5.0
 */
final class SmsDataStoreLookup implements DataStoreLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsDataStoreLookup.class);
    static final String SERVICE_NAME = "amDataStoreService";
    static final String SERVICE_VERSION = "1.0";
    private static final String SUB_CONFIG_NAME = "dataStoreContainer";
    private static final DataStoreIds DEFAULT_IDS = new DataStoreIds(new DataStoreIdsBuilder());
    static final String POLICY_DATASTORE_ATTR_NAME = "policy-datastore-id";
    static final String APPLICATION_DATASTORE_ATTR_NAME = "application-datastore-id";

    private final Map<String, Function<DataStoreIds, DataStoreId>> realmServiceToDataStoreId;
    private final Map<String, String> serviceDataStoreIdAttributes;
    private final ConsoleConfigHandler configHandler;
    private final ServiceConfigManagerFactory configManagerFactory;

    @Inject
    SmsDataStoreLookup(ConsoleConfigHandler configHandler, ServiceConfigManagerFactory configManagerFactory,
            @Named(SERVICE_DATA_STORE_ID_ATTRIBUTE_NAMES) Map<String, String> serviceDataStoreIdAttributes) {
        this.configHandler = configHandler;
        this.configManagerFactory = configManagerFactory;
        realmServiceToDataStoreId = ImmutableMap
                .<String, Function<DataStoreIds, DataStoreId>>builder()
                .put(EntitlementUtils.SERVICE_NAME, DataStoreIds::getPolicyId)
                .put(EntitlementUtils.INDEXES_NAME, DataStoreIds::getPolicyId)
                .put(IdConstants.AGENT_SERVICE, DataStoreIds::getApplicationId)
                .put(Constants.SVC_NAME_SAML_METADATA, DataStoreIds::getApplicationId)
                .put(Constants.COT_SERVICE, DataStoreIds::getApplicationId)
                .put(Constants.SVC_NAME_WS_FED_METADATA, DataStoreIds::getApplicationId)
                .put(Constants.SAML_2_CONFIG, DataStoreIds::getApplicationId)
                .build();
        this.serviceDataStoreIdAttributes = serviceDataStoreIdAttributes;
    }

    @Override
    public DataStoreId lookupRealmId(String serviceName, Realm realm) {
        Reject.ifNull(serviceName, realm);

        if (serviceDataStoreIdAttributes.containsKey(serviceName)) {
            return getServiceDataStoreId(serviceDataStoreIdAttributes.get(serviceName));
        }

        if (!realmServiceToDataStoreId.containsKey(serviceName)) {
            return DataStoreId.DEFAULT;
        }

        return transformDataStoreIdsFor(realm,
                dataStoreIds -> realmServiceToDataStoreId.get(serviceName).apply(dataStoreIds));
    }

    @Override
    public DataStoreId lookupGlobalId(String serviceName) {
        Reject.ifNull(serviceName);

        if (!serviceDataStoreIdAttributes.containsKey(serviceName)) {
            return DataStoreId.DEFAULT;
        }

        return getServiceDataStoreId(serviceDataStoreIdAttributes.get(serviceName));
    }

    @Override
    public Set<DataStoreId> lookupRealmIds(Realm realm) {
        Reject.ifNull(realm);
        return transformDataStoreIdsFor(realm,
                dataStoreIds -> ImmutableSet.of(dataStoreIds.policyId, dataStoreIds.applicationId));
    }

    @Override
    public Set<DataStoreId> lookupGlobalIds() {
        return serviceDataStoreIdAttributes.values().stream()
                .map(this::getServiceDataStoreId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DataStoreId> getAllIds() {
        try {
            ServiceConfig globalConfig = getGlobalServiceConfig();
            ServiceConfig containerConfig = globalConfig.getSubConfig(SUB_CONFIG_NAME);

            Stream<DataStoreId> defaultDataStoreId = isFbcWithoutEmbeddedEnabled() ?
                    Stream.empty() : Stream.of(DataStoreId.DEFAULT);

            return Stream.concat(defaultDataStoreId,
                    containerConfig.getSubConfigNames()
                            .stream()
                            .map(DataStoreId::of))
                    .collect(Collectors.toSet());
        } catch (SMSException | SSOException e) {
            throw new DataStoreException("Unable to read SMS configuration for data store", e);
        }
    }

    private <T> T transformDataStoreIdsFor(Realm realm, Function<DataStoreIds, T> mapper) {
        try {
            DataStoreIds dataStoreIds = configHandler.getConfig(realm.asPath(), DataStoreIdsBuilder.class);

            if (dataStoreIds == null) {
                throw new DataStoreException("Data store config has come back as null for " + realm);
            }

            return mapper.apply(dataStoreIds);
        } catch (ConfigRetrievalException e) {
            LOGGER.warn("Failed to retrieve data store config for realm " + realm, e);
            // service wont be available when we upgrade from a version prior to 6.5.0
            // so return default external data store ids to defend against upgrade failure
            return mapper.apply(DEFAULT_IDS);
        }
    }

    private DataStoreId getServiceDataStoreId(String attributeName) {
        try {
            String dataStoreId = CollectionUtils.getFirstItem(getGlobalServiceConfig()
                    .getAttributeValue(attributeName));

            if (dataStoreId == null) {
                return DataStoreId.DEFAULT;
            }

            return DataStoreId.of(dataStoreId);
        } catch (SMSException | SSOException e) {
            LOGGER.warn("Failed to retrieve global data store config", e);
            // service wont be available when we upgrade from a version prior to 7.0.0
            // so return default external data store ids to defend against upgrade failure
            return DataStoreId.DEFAULT;
        }
    }

    private ServiceConfig getGlobalServiceConfig() throws SMSException, SSOException {
        ServiceConfigManager configManager = configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION);
        ServiceConfig globalConfig = configManager.getGlobalConfig("default");

        if (globalConfig == null) {
            throw new DataStoreException("Unable to retrieve the global config");
        }
        return globalConfig;
    }

    static final class DataStoreIds {

        private final DataStoreId policyId;
        private final DataStoreId applicationId;

        DataStoreIds(DataStoreIdsBuilder builder) {
            policyId = builder.policyId;
            applicationId = builder.applicationId;
        }

        DataStoreId getPolicyId() {
            return policyId;
        }

        DataStoreId getApplicationId() {
            return applicationId;
        }
    }

    /**
     * Config builder used to help retrieve data store Ids.
     */
    @ConfigSource(value = "amDataStoreService", includeAttributeDefaults = false)
    public static final class DataStoreIdsBuilder implements ConsoleConfigBuilder<DataStoreIds> {

        private DataStoreId policyId = DataStoreId.DEFAULT;
        private DataStoreId applicationId = DataStoreId.DEFAULT;

        @ConfigAttribute(value = POLICY_DATASTORE_ATTR_NAME, required = false,
                transformer = StringToDataStoreId.class, defaultValues = DataStoreId.DEFAULT_ID)
        public void withPolicyDataStoreId(DataStoreId policyId) {
            this.policyId = policyId;
        }

        @ConfigAttribute(value = APPLICATION_DATASTORE_ATTR_NAME, required = false,
                transformer = StringToDataStoreId.class, defaultValues = DataStoreId.DEFAULT_ID)
        public void withApplicationDataStoreId(DataStoreId applicationId) {
            this.applicationId = applicationId;
        }

        @Override
        public DataStoreIds build(Map<String, Set<String>> attributes) {
            Reject.ifNull(policyId, applicationId);
            return new DataStoreIds(this);
        }

    }

    /**
     * Transforms a given string into a data store Id instance.
     */
    public static final class StringToDataStoreId implements ConfigTransformer<DataStoreId> {

        @Override
        public DataStoreId transform(Set<String> values, Class<?> parameterType) {
            String value = values.iterator().next();
            return DataStoreId.of(value);
        }

    }
}
