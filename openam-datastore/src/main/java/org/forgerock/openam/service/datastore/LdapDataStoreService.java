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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static java.lang.Integer.parseInt;
import static org.forgerock.openam.ldap.LDAPUtils.LDAP_SECURE_PROTOCOLS;
import static org.forgerock.opendj.ldap.LdapConnectionFactory.AUTHN_BIND_REQUEST;
import static org.forgerock.opendj.ldap.LdapConnectionFactory.SSL_USE_STARTTLS;

import java.io.Closeable;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.PersistentSearchChangeType;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.security.SslContextBuilder;
import org.forgerock.opendj.security.SslOptions;
import org.forgerock.util.Option;
import org.forgerock.util.Options;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
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
final class LdapDataStoreService implements DataStoreService, Closeable {

    private static final String SERVICE_NAME = "amGlobalDataStoreService";
    private static final String SERVICE_VERSION = "1.0";

    private static final String DATA_STORE_NAME = "dataStoreName";
    private static final String BIND_DN = "bindDN";
    private static final String BIND_PASSWORD = "bindPassword";
    private static final String SERVER_PORT = "serverPort";
    private static final String SERVER_HOSTNAME = "serverHostname";
    private static final String USE_SSL = "useSsl";
    private static final String USE_START_TLS = "useStartTls";

    private static final String SUB_CONFIG_NAME = "globaldatastorecontainer";
    private static final String CONFIG_ID_PREFIX = "/" + SUB_CONFIG_NAME + "/";
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDataStoreService.class);
    private static final Option<SslOptions> SSL_OPTIONS = Option.of(SslOptions.class, null);

    private final Provider<ConnectionFactory> defaultDataStoreProvider;
    private final ConcurrentMap<String, ConnectionFactory> factoryCache;
    private final ServiceConfigManagerFactory serviceConfigManagerFactory;
    private volatile boolean closing = false;

    @Inject
    LdapDataStoreService(Provider<ConnectionFactory> defaultDataStoreProvider,
                         ServiceConfigManagerFactory serviceConfigManagerFactory,
                         ShutdownManager shutdownManager) {
        this.defaultDataStoreProvider = defaultDataStoreProvider;
        this.factoryCache = new ConcurrentHashMap<>();
        this.serviceConfigManagerFactory = serviceConfigManagerFactory;

        try {
            addDataStoreListener(serviceConfigManagerFactory.create(SERVICE_NAME, SERVICE_VERSION));
        } catch (SMSException | SSOException e) {
            throw new DataStoreException("DataStoreService::Unable to construct DataStoreServiceListener", e);
        }

        shutdownManager.addShutdownListener(this::close);
    }

    private void addDataStoreListener(ServiceConfigManager scm) {
        if (scm.addListener(new LocalDataStoreServiceListener()) == null) {
            LOGGER.error("Could not add listener to ServiceConfigManager instance. Global Data Store service "
                    + "changes will not be dynamically updated");
        }
    }

    @Override
    public Connection getDefaultConnection() {
        if (closing) {
            throw new DataStoreException("Service is shutting down");
        }
        try {
            return defaultDataStoreProvider.get().getConnection();
        } catch (LdapException e) {
            throw new DataStoreException("Unable to create a new default connection", e);
        }
    }

    @Override
    public Connection getConnection(String dataStoreId) {
        if (closing) {
            throw new DataStoreException("Service is shutting down");
        }
        try {
            return getConnectionFactory(dataStoreId).getConnection();
        } catch (LdapException e) {
            throw new DataStoreException("Unable to create a new connection", e);
        }
    }

    private ConnectionFactory getConnectionFactory(String dataStoreId) {
        return factoryCache.computeIfAbsent(dataStoreId, this::createConnectionFactory);
    }

    private ConnectionFactory createConnectionFactory(String dataStoreId) {
        DataStoreConfig config = readConfig(dataStoreId);
        return new LdapConnectionFactory(config.getHostname(), config.getPort(), createOptions(config));
    }

    private Options createOptions(DataStoreConfig config) {
        Options ldapOptions = Options.defaultOptions();
        if (config.isUseSsl() || config.isUseStartTLS()) {
            addSSLOption(ldapOptions);
            ldapOptions.set(SSL_USE_STARTTLS, true);
        }
        addBindOption(ldapOptions, config);
        return ldapOptions;
    }

    private void addSSLOption(Options options) {
        SslContextBuilder sslContextBuilder = new SslContextBuilder();

        try {
            SSLContext sslContext = sslContextBuilder.build();
            SslOptions sslOptions = SslOptions
                    .newSslOptions(sslContext)
                    .enabledProtocols(LDAP_SECURE_PROTOCOLS);
            options.set(SSL_OPTIONS, sslOptions);
        } catch (GeneralSecurityException e) {
            LOGGER.error("LdapDataStoreService::createOptions::An error occurred while creating SSLContext", e);
        }
    }

    private void addBindOption(Options options, DataStoreConfig config) {
        if (!StringUtils.isBlank(config.getBindDN())) {
            options.set(AUTHN_BIND_REQUEST,
                    LDAPRequests.newSimpleBindRequest(config.getBindDN(), config.getBindPassword().toCharArray()));
        }
    }


    private DataStoreConfig readConfig(String dataStoreId) {
        Map<String, Set<String>> attributes = getDataStoreConfigAttributes(dataStoreId);
        return buildConfig(dataStoreId, attributes);
    }

    private Map<String, Set<String>> getDataStoreConfigAttributes(String id) {
        try {
            ServiceConfigManager scm = getServiceConfigManager();
            ServiceConfig globalConfig = scm.getGlobalConfig("default");
            if (globalConfig == null) {
                return Collections.emptyMap();
            }
            ServiceConfig containerConfig = globalConfig.getSubConfig(SUB_CONFIG_NAME);
            ServiceConfig subConfig = containerConfig.getSubConfig(id);
            if (subConfig == null) {
                return Collections.emptyMap();
            }
            return subConfig.getAttributes();
        } catch (SMSException | SSOException e) {
            LOGGER.error("Could not look up the Data Store Config for the ID : " + id, e);
        }
        return Collections.emptyMap();
    }

    private DataStoreConfig buildConfig(String id, Map<String, Set<String>> attributes) {
        return DataStoreConfig.builder()
            .withHostname(getMapAttr(attributes, SERVER_HOSTNAME))
            .withBindDN(getMapAttr(attributes, BIND_DN))
            .withBindPassword(getMapAttr(attributes, BIND_PASSWORD))
            .withPort(parseInt(getMapAttr(attributes, SERVER_PORT)))
            .withUseSsl(getBooleanMapAttr(attributes, USE_SSL, false))
            .withUseStartTLS(getBooleanMapAttr(attributes, USE_START_TLS, false))
            .withId(id).build();
    }

    private ServiceConfigManager getServiceConfigManager() throws SMSException, SSOException {
        return serviceConfigManagerFactory.create(SERVICE_NAME, SERVICE_VERSION);
    }

    @Override
    public void close() {
        closing = true;
        factoryCache.forEach((key, factory) -> factory.close());
        try {
            defaultDataStoreProvider.get().getConnection().close();
        } catch (LdapException e) {
        }
    }

    private class LocalDataStoreServiceListener implements ServiceListener {

        @Override
        public void schemaChanged(String serviceName, String version) {
            // No Operation
        }

        @Override
        public void globalConfigChanged(String serviceName, String version,
                String groupName, String serviceComponent, int type) {
            if (!SERVICE_NAME.equals(serviceName) || StringUtils.isBlank(serviceComponent)) {
                return;
            }
            String id = getConfigId(serviceComponent);
            switch (type) {
                case PersistentSearchChangeType.REMOVED:
                case PersistentSearchChangeType.ADDED:
                case PersistentSearchChangeType.MODIFIED:
                    factoryCache.remove(id);
                    break;
                default:
                    //do nothing
            }
        }

        private String getConfigId(String serviceComponent) {
            return serviceComponent.contains(CONFIG_ID_PREFIX) ?
                    serviceComponent.substring(CONFIG_ID_PREFIX.length()) : serviceComponent;
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName, String serviceComponent, int type) {
            // No Operation
        }
    }
}
