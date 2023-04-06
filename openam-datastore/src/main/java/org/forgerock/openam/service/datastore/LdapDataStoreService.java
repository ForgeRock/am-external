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
 * Copyright 2018-2022 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.sm.SMSUtils.serviceExists;
import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.APPLICATION_DATASTORE_ATTR_NAME;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.POLICY_DATASTORE_ATTR_NAME;
import static org.forgerock.openam.services.datastore.DataStoreId.DEFAULT_ID;
import static org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier.Type;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.ldap.ConnectionConfig;
import org.forgerock.openam.services.datastore.DataStoreException;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreService;
import org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.RealmUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * Ldap based data store service implementation.
 *
 * @since 6.0.0
 */
@Singleton
final class LdapDataStoreService implements DataStoreService, ServiceListener, DataStoreServiceRegister {

    private static final Logger logger = LoggerFactory.getLogger(LdapDataStoreService.class);

    private static final String SERVICE_NAME = "amDataStoreService";
    private static final String SERVICE_VERSION = "1.0";
    private static final String SUB_CONFIG_NAME = "dataStoreContainer";

    private static final String BIND_DN = "bindDN";
    private static final String BIND_PASSWORD = "bindPassword";
    private static final String SERVER_PORT = "serverPort";
    private static final String MINIMUM_CONNECTION_POOL = "minimumConnectionPool";
    private static final String MAXIMUM_CONNECTION_POOL = "maximumConnectionPool";
    private static final String SERVER_HOSTNAME = "serverHostname";
    private static final String SERVER_URLS = "serverUrls";
    private static final String USE_SSL = "useSsl";
    private static final String USE_START_TLS = "useStartTls";
    private static final String AFFINITY_ENABLED = "affinityEnabled";


    private final Provider<ConnectionFactory> defaultConnectionFactoryProvider;
    private final LdapConnectionFactoryProvider connectionFactoryProvider;
    private final ServiceConfigManagerFactory configManagerFactory;

    private final ConcurrentMap<DataStoreId, ConnectionFactory> factoryCache;
    private final Set<DataStoreServiceChangeNotifier> changeNotifiers;
    private final RealmLookup realmLookup;
    private final VolatileActionConsistencyController consistencyController;
    private final Runnable refreshDataLayer;
    private final PrivilegedAction<SSOToken> adminTokenAction;
    private boolean shuttingDown;

    @Inject
    LdapDataStoreService(Provider<ConnectionFactory> defaultConnectionFactoryProvider,
            LdapConnectionFactoryProvider connectionFactoryProvider,
            ServiceConfigManagerFactory configManagerFactory, ShutdownManager shutdownManager,
            Set<DataStoreServiceChangeNotifier> changeNotifiers,
            RealmLookup realmLookup, VolatileActionConsistencyController consistencyController,
            @Named("refresh-data-layer") Runnable refreshDataLayer, PrivilegedAction<SSOToken> adminTokenAction) {
        this.defaultConnectionFactoryProvider = defaultConnectionFactoryProvider;
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.configManagerFactory = configManagerFactory;
        this.factoryCache = new ConcurrentHashMap<>();
        this.changeNotifiers = changeNotifiers;
        this.realmLookup = realmLookup;
        this.consistencyController = consistencyController;
        this.refreshDataLayer = refreshDataLayer;
        this.adminTokenAction = adminTokenAction;

        shutdownManager.addShutdownListener(this::shutDown);
    }

    @Override
    public ConnectionFactory getConnectionFactory(DataStoreId dataStoreId) {
        Reject.ifNull(dataStoreId);
        ConnectionFactory connectionFactory = factoryCache.get(dataStoreId);

        if (connectionFactory == null) {
            connectionFactory = createConnectionFactory(dataStoreId);
        }

        return new ManagedConnectionFactory(connectionFactory);
    }

    /**
     * Acts on data in the form '/datastorecontainer/dataStoreName' where we wish to retrieve 'dataStoreName'.
     *
     * @param serviceComponent Full serviceComponent
     * @return DataStoreId of the provided dataStoreName within the serviceComponent
     */
    private DataStoreId getDataStoreIdFromConfigServiceComponent(String serviceComponent) {
        return DataStoreId.of(serviceComponent.substring(serviceComponent.lastIndexOf("/") + 1));
    }

    private void removeFromCacheAndClose(DataStoreId dataStoreId) {
        ConnectionFactory factory = factoryCache.remove(dataStoreId);
        if (factory != null) {
            IOUtils.closeIfNotNull(factory);
        } else {
            logger.error("Failed to identify and remove the connection " +
                    "factory from the cache, clearing the entire cache");
            factoryCache.entrySet().removeIf(e -> {
                if (!e.getKey().equals(DataStoreId.DEFAULT)) {
                    IOUtils.closeIfNotNull(e.getValue());
                    return true;
                }
                return false;
            });
        }
    }

    private synchronized ConnectionFactory createConnectionFactory(DataStoreId dataStoreId) {
        if (shuttingDown) {
            throw new DataStoreException("Service is shutting down");
        }

        ConnectionFactory connectionFactory = factoryCache.get(dataStoreId);

        if (connectionFactory != null) {
            return connectionFactory;
        }

        if (dataStoreId.equals(DataStoreId.DEFAULT)) {
            connectionFactory = defaultConnectionFactoryProvider.get();
        } else {
            DataStoreConfig config = (DataStoreConfig) getConfig(dataStoreId);
            connectionFactory = connectionFactoryProvider.createLdapConnectionFactory(config);
        }

        factoryCache.put(dataStoreId, connectionFactory);
        return connectionFactory;
    }

    private void resetDataStoreToDefault(DataStoreId dataStoreId) {
        SSOToken token = AccessController.doPrivileged(adminTokenAction);
        try {
            for (String realm : RealmUtils.getRealmNames(token)) {
                ServiceConfig config = new ServiceConfigManager(SERVICE_NAME, token).getOrganizationConfig(realm, null);
                if (serviceExists(config)) {
                    setDataStoreAttributeToDefault(config, dataStoreId, POLICY_DATASTORE_ATTR_NAME);
                    setDataStoreAttributeToDefault(config, dataStoreId, APPLICATION_DATASTORE_ATTR_NAME);
                }
            }
        } catch (SMSException | SSOException e) {
            logger.error("Failed to reset the data store to default", e);
        }
    }

    private void setDataStoreAttributeToDefault(ServiceConfig config,
            DataStoreId dataStoreId, String attributeName) throws SSOException, SMSException {
        Set<String> ids = config.getAttributeValue(attributeName);
        if (!ids.isEmpty()) {
            String id = ids.iterator().next();
            if (dataStoreId.equals(DataStoreId.of(id))) {
                config.replaceAttributeValue(attributeName, id, DEFAULT_ID);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param dataStoreId Non-null ID representing the DataStore to read configuration for.
     * @return A non-null {@link ConnectionConfig}
     * @throws DataStoreException If there was an error whilst trying to read the configuration of if the
     * requested {@code dataStoreId} did not represent a valid configuration.
     */
    public ConnectionConfig getConfig(DataStoreId dataStoreId) {
        try {
            ServiceConfigManager configManager = getServiceConfigManager();
            ServiceConfig globalConfig = configManager.getGlobalConfig("default");

            if (globalConfig == null) {
                throw new DataStoreException("Unable to retrieve the global config");
            }

            ServiceConfig containerConfig = globalConfig.getSubConfig(SUB_CONFIG_NAME);
            ServiceConfig dataStoreSubConfig = containerConfig.getSubConfig(dataStoreId.getId());

            if (dataStoreSubConfig == null) {
                throw new DataStoreException("Unable to retrieve the sub config for the data store");
            }

            Map<String, Set<String>> attributes = dataStoreSubConfig.getAttributes();

            Set<String> serverUrls;
            if (attributes.containsKey(SERVER_URLS)) {
                serverUrls = attributes.get(SERVER_URLS);
            } else {
                // During upgrade from 6.5.0 AM this service is accessed,
                // since the configuration hasn't been upgraded yet
                // the old attributes are used.
                String hostname = getMapAttr(attributes, SERVER_HOSTNAME);
                String port = getMapAttr(attributes, SERVER_PORT);
                serverUrls = Collections.singleton("[0]=" + hostname + ":" + port);
            }

            String bindPassword = getMapAttr(attributes, BIND_PASSWORD);
            return DataStoreConfig.builder(dataStoreId)
                    .withServerUrls(serverUrls)
                    .withBindDN(getMapAttr(attributes, BIND_DN))
                    .withBindPassword(bindPassword != null ? bindPassword.toCharArray() : null)
                    .withMinimumConnectionPool(parseInt(getMapAttr(attributes, MINIMUM_CONNECTION_POOL)))
                    .withMaximumConnectionPool(parseInt(getMapAttr(attributes, MAXIMUM_CONNECTION_POOL)))
                    .withUseSsl(getBooleanMapAttr(attributes, USE_SSL, false))
                    .withUseStartTLS(getBooleanMapAttr(attributes, USE_START_TLS, false))
                    .withAffinityEnabled(getBooleanMapAttr(attributes, AFFINITY_ENABLED, false))
                    .build();

        } catch (SMSException | SSOException e) {
            throw new DataStoreException("Unable to read SMS configuration for data store", e);
        }
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type) {
        if (serviceName.equals(SERVICE_NAME)) {
            String realm = realmLookup.convertRealmDnToRealmPath(orgName);
            changeNotifiers.forEach(n -> n.notifyOrgChanges(realm));
        }
    }

    @Override
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {

        if (!serviceName.equals(SERVICE_NAME)) {
            return;
        }

        if (serviceComponent.isEmpty()) {
            //if there's no data store reference on the updated global config change, no need to do anything
            return;
        }

        consistencyController.safeExecuteVolatileAction(() -> notifyOfDataStoreChange(serviceComponent, type));
    }

    private void notifyOfDataStoreChange(String serviceComponent, int type) {
        DataStoreId dataStoreId = getDataStoreIdFromConfigServiceComponent(serviceComponent);

        switch (type) {
            case ADDED:
                changeNotifiers.forEach(n -> n.notifyGlobalChanges(dataStoreId, Type.DATA_STORE_ADDED));
                break;
            case REMOVED:
                changeNotifiers.forEach(n -> n.notifyGlobalChanges(dataStoreId, Type.DATA_STORE_REMOVED));
                removeFromCacheAndClose(dataStoreId);
                resetDataStoreToDefault(dataStoreId);
                break;
            case MODIFIED:
                changeNotifiers.forEach(n -> n.notifyGlobalChanges(dataStoreId, Type.DATA_STORE_REMOVED));
                removeFromCacheAndClose(dataStoreId);
                changeNotifiers.forEach(n -> n.notifyGlobalChanges(dataStoreId, Type.DATA_STORE_ADDED));
                break;
        }

        refreshDataLayer.run();
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // Do nothing
    }

    private ServiceConfigManager getServiceConfigManager() {
        try {
            return configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION);
        } catch (SMSException | SSOException e) {
            throw new DataStoreException("Unable to retrieve the data store service", e);
        }
    }

    @Override
    public void register() {
        getServiceConfigManager().addListener(this);
    }

    private synchronized void shutDown() {
        shuttingDown = true;
        factoryCache.forEach((key, factory) -> factory.close());
    }

    /**
     * Connection factories created by this service should be managed
     * solely by this service, specifically the closing of factories.
     */
    private static class ManagedConnectionFactory implements ConnectionFactory {

        private final ConnectionFactory wrappedFactory;

        private ManagedConnectionFactory(ConnectionFactory wrappedFactory) {
            this.wrappedFactory = requireNonNull(wrappedFactory, "Wrapped factory must not be null");
        }

        @Override
        public Promise<Connection, LdapException> getConnectionAsync() {
            return wrappedFactory.getConnectionAsync();
        }

        @Override
        public Connection getConnection() throws LdapException {
            return wrappedFactory.getConnection();
        }

        @Override
        public void close() {
            logger.warn("Ignoring attempted call to close a managed connection factory");
        }

        @Override
        public String toString() {
            if (wrappedFactory != null) {
                return wrappedFactory.toString();
            }
            return null;
        }

    }

}
