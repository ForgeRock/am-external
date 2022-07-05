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
 * $Id: ConfigurationInstanceImpl.java,v 1.12 2009/10/29 00:03:50 exu Exp $
 *
 * Portions Copyrighted 2015-2021 ForgeRock AS.
 */
package com.sun.identity.plugin.configuration.impl;

import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.core.sms.DefaultConfigServiceHolder;
import org.forgerock.openam.plugin.configuration.ConfigurationAuthorizationException;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.forgerock.openam.sm.exceptions.SmsAuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * <code>ConfigurationInstanceImpl</code> is the implementation that provides
 * the operations on service configuration. 
 */
public class ConfigurationInstanceImpl implements ConfigurationInstance {

    private static Map<String, String> serviceNameMap;
    private ServiceSchemaManager ssm;
    private ServiceConfigManager scm;
    private String componentName = null;
    private String subConfigId = null;
    private boolean hasOrgSchema = false;
    private static final int SUBCONFIG_PRIORITY = 0;
    private static final String RESOURCE_BUNDLE = "fmConfigurationService";
    private static Logger debug = LoggerFactory.getLogger(ConfigurationInstanceImpl.class);
    private static final DataStoreLookup dataStoreLookup = InjectorHolder.getInstance(DataStoreLookup.class);

    private SSOToken ssoToken;

    static {
        serviceNameMap = new HashMap<>();
        serviceNameMap.put("SAML2", "sunFMSAML2MetadataService");
        serviceNameMap.put("WS-FEDERATION", "sunFMWSFederationMetadataService");
        serviceNameMap.put("LIBCOT","sunFMCOTConfigService");
        serviceNameMap.put("PLATFORM", "iPlanetAMPlatformService");
        serviceNameMap.put("NAMING", "iPlanetAMNamingService");
        serviceNameMap.put("AUTHN", "iPlanetAMAuthService");
        serviceNameMap.put("SAML2_SOAP_BINDING","sunfmSAML2SOAPBindingService");
        serviceNameMap.put("MULTI_PROTOCOL","sunMultiFederationProtocol");
        serviceNameMap.put("STS_CONFIG","sunFAMSTSService");
        serviceNameMap.put("SAML2_CONFIG", "sunFAMSAML2Configuration");
    }

    private SSOToken getSSOToken() {
        return (ssoToken != null) ? ssoToken : AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    @Override
    public void init(String componentName, Object session) 
        throws ConfigurationException {

        String serviceName = serviceNameMap.get(componentName);
        if (serviceName == null) {
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "componentNameUnsupported", null);
        }
        if (session instanceof SSOToken) {
            ssoToken = (SSOToken) session;
        }

        try {
            SSOToken adminToken = getSSOToken();
            ssm = new ServiceSchemaManager(serviceName, adminToken);
            ServiceSchema oss = ssm.getOrganizationSchema();
            if (oss != null) {
                hasOrgSchema = true;
                Set subSchemaNames = oss.getSubSchemaNames();
                if ((subSchemaNames != null) && (subSchemaNames.size() == 1)) {
                    subConfigId = (String)subSchemaNames.iterator().next();
                }
            }
            scm = new ServiceConfigManager(serviceName, adminToken);
        } catch (SMSException | SSOException smsex) {
            debug.error("ConfigurationInstanceImpl.init:", smsex);
            throw new ConfigurationException(smsex);
        }

        this.componentName = componentName;       
    }

    /**
     * Returns Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp".
     *     The configName could be null or empty string, which means the default
     *     configuration for this components. 
     * @return Map of key/value pairs, key is the attribute name, value is
     *     a Set of attribute values or null if service configuration doesn't
     *     doesn't exist. If the configName parameter is null or empty, and OrganizationalConfig state is present,
     *     this state will be merged with the GlobalConfig attributes, with the OrganizationConfig attributes
     *     over-writing the GlobalConfig attributes, in case GlobalConfig and OrganizationConfig attributes share the
     *     same key.
     * @exception ConfigurationException if an error occurred while getting
     *     service configuration.
     */
    public Map getConfiguration(String realm, String configName)
        throws ConfigurationException {

        if (debug.isDebugEnabled()) {
            debug.debug("ConfigurationInstanceImpl.getConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName);
        }

        try {
            if (hasOrgSchema) {
                DataStoreId dataStoreId = getDataStoreId(realm);
                ServiceConfig organizationConfig = scm.getOrganizationConfig(realm, null, dataStoreId);
                if (organizationConfig == null) {
                    return null;
                }

                if (isEmpty(configName)) {
                    Map<String, Set<String>> organizationAttributes = organizationConfig.getAttributes();
                    ServiceConfig globalConfig = scm.getGlobalConfig(configName, dataStoreId);
                    if (globalConfig != null) {
                        Map<String, Set<String>> mergedAttributes = globalConfig.getAttributes();
                        mergedAttributes.putAll(organizationAttributes);
                        return mergedAttributes;
                    }
                    return organizationAttributes;
                } else {
                    if (subConfigId == null) {
                        if (debug.isDebugEnabled()) {
                            debug.debug("ConfigurationInstanceImpl." +
                                "getConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }
                    organizationConfig = organizationConfig.getSubConfig(configName);
                    if (organizationConfig == null) {
                        return null;
                    }

                    return organizationConfig.getAttributes();
                }
            } else {
                if ((realm != null) && (!realm.equals("/"))) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("ConfigurationInstanceImpl." +
                            "getConfiguration: organization configuraton not "+
                            "supported.");
                    }
                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noOrgConfig", data);
                }
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss == null) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("ConfigurationInstanceImpl." +
                            "getConfiguration: configuraton not " +
                            "supported.");
                    }

                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noConfig", data);
                }

                Map<String, Set<String>> retMap = DefaultConfigServiceHolder.get().getDefaultsForService(ss);
                if (componentName.equals("PLATFORM")) {
                    SSOToken token = getSSOToken();
                    retMap.put(Constants.PLATFORM_LIST, 
                        ServerConfiguration.getServerInfo(token));
                    retMap.put(Constants.SITE_LIST, 
                        SiteConfiguration.getSiteInfo(token));
                 }
                 return retMap;
            }
        } catch (SMSException | SSOException | RealmLookupException smsex) {
            debug.error("ConfigurationInstanceImpl.getConfiguration:", smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedGetConfig", data);
        }
    }

    /**
     * Sets Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not set service configuration
     *     or service configuration doesn't exist.
     */
    public void setConfiguration(String realm,
        String configName, Map avPairs)
        throws ConfigurationException {

        if (debug.isDebugEnabled()) {
            debug.debug("ConfigurationInstanceImpl.setConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", avPairs = " + avPairs);
        }

        try {
            if (hasOrgSchema) {
                ServiceConfig sc = scm.getOrganizationConfig(realm, null, getDataStoreId(realm));

                if (sc == null) {
                    String[] data = { componentName, realm };
                    throw new ConfigurationException(RESOURCE_BUNDLE, 
                        "configNotExist", data);
                }

                if ((configName == null) || (configName.length() == 0)) {
                    sc.setAttributes(avPairs);
                } else {
                    if (subConfigId == null) {
                        if (debug.isDebugEnabled()) {
                            debug.debug("ConfigurationInstanceImpl." +
                                "setConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }
                    sc = sc.getSubConfig(configName);
                    if (sc == null) {
                        String[] data = { componentName, realm };
                        throw new ConfigurationException(RESOURCE_BUNDLE, 
                            "configNotExist", data);
                    }

                    sc.setAttributes(avPairs);
                }
            } else {
                if ((realm != null) && (!realm.equals("/"))) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("ConfigurationInstanceImpl." +
                            "setConfiguration: organization configuraton not "+
                            "supported.");
                    }
                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noOrgConfig", data);
                }
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss == null) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("ConfigurationInstanceImpl." +
                            "setConfiguration: configuraton not " +
                            "supported.");
                    }
                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noConfig", data);
                }

                DefaultConfigServiceHolder.get().setDefaultsForService(ss.getServiceName(),
                        ConfigurationAttributesFactory.create(avPairs), ss.getServiceType(), ss);
            }
        } catch (SmsAuthorizationException ex) {
            throw new ConfigurationAuthorizationException(ex);
        } catch (SMSException | SSOException | RealmLookupException smsex) {
            debug.error("ConfigurationInstanceImpl.setConfiguration:", smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE, 
                "failedSetConfig", data);
        }
    }

    /**
     * Creates Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the
     *     default configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not create service 
     *     configuration.
     */
    public void createConfiguration(String realm, String configName,
        Map avPairs)
        throws ConfigurationException {

        if (debug.isDebugEnabled()) {
            debug.debug("ConfigurationInstanceImpl.createConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", avPairs = " + avPairs);
        }

        try {
            if (hasOrgSchema) {
                DataStoreId dataStoreId = getDataStoreId(realm);
                ServiceConfig sc = scm.getOrganizationConfig(realm, null, dataStoreId);

                if (isEmpty(configName)) {
                    scm.createOrganizationConfig(realm, ConfigurationAttributesFactory.create(avPairs), dataStoreId);
                } else {
                    if (subConfigId == null) {
                        if (debug.isDebugEnabled()) {
                            debug.debug("ConfigurationInstanceImpl." +
                                "createConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }

                    if (sc == null) {
                        sc = scm.createOrganizationConfig(realm, null, dataStoreId);
                    } else if (sc.getSubConfigNames().contains(configName)) {
                        String[] data = { componentName, realm, configName };
                        throw new ConfigurationException(RESOURCE_BUNDLE, 
                            "configExist", data);
                    }

                    sc.addSubConfig(configName, subConfigId,
                        SUBCONFIG_PRIORITY, ConfigurationAttributesFactory.create(avPairs));
                }
            } else {
                if (debug.isDebugEnabled()) {
                    debug.debug("ConfigurationInstanceImpl." +
                        "createConfiguration: configuraton creation not " +
                        "supported.");
                }
                String[] data = { componentName };
                throw new ConfigurationException(RESOURCE_BUNDLE,
                    "noConfigCreation", data);
            }
        } catch (SMSException | SSOException | RealmLookupException smsex) {
            debug.error("ConfigurationInstanceImpl.createConfiguration:",
                smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedCreateConfig", data);
        }
    }

    /**
     * Deletes Configuration.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param attributes A set of attributes to be deleted from the Service
     *     configuration. If the value is null or empty, deletes all service 
     *     configuration.
     * @exception ConfigurationException if could not delete service 
     *     configuration.
     */
    public void deleteConfiguration(String realm, 
        String configName, Set attributes)
        throws ConfigurationException {

        if (debug.isDebugEnabled()) {
            debug.debug("ConfigurationInstanceImpl.deleteConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", attributes = " +
                attributes);
        }

        boolean removeConfig = (attributes == null) || (attributes.isEmpty());
        try {
            if (hasOrgSchema) {
                ServiceConfig sc;
                DataStoreId dataStoreId = getDataStoreId(realm);
                if (isEmpty(configName)) {
                    if (removeConfig) {
                        scm.removeOrganizationConfiguration(realm, null, dataStoreId);
                    } else {
                        sc = scm.getOrganizationConfig(realm, null, dataStoreId);
                        if (sc != null) {
                            sc.removeAttributes(attributes);
                        }
                    }
                } else {
                    if (subConfigId == null) {
                        if (debug.isDebugEnabled()) {
                            debug.debug("ConfigurationInstanceImpl." +
                                "deleteConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }

                    sc = scm.getOrganizationConfig(realm, null, dataStoreId);
                    if (sc != null) {
                        if (removeConfig) {
                            sc.removeSubConfig(configName);
                        } else {
                            sc = sc.getSubConfig(configName);
                            if (sc != null) {
                                sc.removeAttributes(attributes);
                            }
                        }
                    }
                }
            } else {
                if (debug.isDebugEnabled()) {
                    debug.debug("ConfigurationInstanceImpl." +
                        "deleteConfiguration: configuraton deletion not " +
                        "supported.");
                }
                String[] data = { componentName };
                throw new ConfigurationException(RESOURCE_BUNDLE,
                    "noConfigDeletion", data);
            }
        } catch (SmsAuthorizationException ex) {
            throw new ConfigurationAuthorizationException(ex);
        } catch (SMSException | SSOException | RealmLookupException smsex) {
            debug.error("ConfigurationInstanceImpl.deleteConfiguration:",
                smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE, 
                "failedDeleteConfig", data);
        }

    }

    /**
     * Returns all service config name for this components.
     * @param realm the name of organization at which the configuration resides.
     * @return Set of service configuration names. Return null if there 
     *     is no service configuration for this component, return empty set
     *     if there is only default configuration instance.
     * @exception ConfigurationException if could not get all service 
     *     configuration names.
     */
    public Set<String> getAllConfigurationNames(String realm)
        throws ConfigurationException {

        if (debug.isDebugEnabled()) {
            debug.debug("ConfigurationInstanceImpl.getAllConfigurationNames"+
                ": realm = " + realm + ", componentName = " + componentName);
        }
        try {
            if (hasOrgSchema) {
                ServiceConfig sc = scm.getOrganizationConfig(realm, null, getDataStoreId(realm));
                if (sc == null) {
                    return null;
                }
                Set<String> subConfigNames = sc.getSubConfigNames();
                if ((subConfigNames != null) && (subConfigNames.size() > 0)) {
                    return subConfigNames;
                } else {
                    return Collections.emptySet();
                }
            } else {
                if ((realm != null) && (!realm.equals("/"))) {
                    return null;
                }
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss == null) {
                    return null;
                } else {
                    return Collections.emptySet();
                }
            }
        } catch (SMSException | SSOException | RealmLookupException smsex) {
            debug.error("ConfigurationInstanceImpl.getAllConfigurationNames:",
                smsex);

            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedGetConfigNames", data);
        }
    }

    /**
     * Registers for changes to the component's configuration. The object will
     * be called when configuration for this component is changed.
     * @return the registered id for this listener instance.
     * @exception ConfigurationException if could not register the listener.
     */
    public String addListener(ConfigurationListener listener)
        throws ConfigurationException {

        if (hasOrgSchema) {
            return scm.addListener(new ServiceListenerImpl(listener,
                                                           componentName));
        } else {
            return ssm.addListener(new ServiceListenerImpl(listener,
                                                           componentName));
        }
    }

    /**
     * Unregisters the listener from the component for the given
     * listener ID. The ID was issued when the listener was registered.
     * @param listenerID the returned id when the listener was registered.
     * @exception ConfigurationException if could not register the listener.
     */
    public void removeListener(String listenerID)
        throws ConfigurationException {

        if (hasOrgSchema) {
            scm.removeListener(listenerID);
        } else {
            ssm.removeListener(listenerID);
        }
    }

    private DataStoreId getDataStoreId(String realmName) throws RealmLookupException {
        Realm realm = Realms.of(realmName);
        return dataStoreLookup.lookupRealmId(serviceNameMap.get(componentName), realm);
    }
}
