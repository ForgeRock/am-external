/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.federation.testutils;

import static com.sun.identity.wsfederation.meta.WSFederationMetaUtils.convertStringToJAXB;
import static org.forgerock.openam.utils.IOUtils.getFileContentFromClassPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.impl.EntityDescriptorElementImpl;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;

/**
 * A simplified configuration store implementation that does not care about global vs realm configurations.
 */
public class TestCaseConfigurationInstance implements ConfigurationInstance {

    // Used when the organisation config is requested rather than a specific configuration,
    // realm/configName parameters will be null.
    private final String ORG_CONFIG = "ORG_CONFIG";

    /** Map by componentName, configName, attribute, values */
    private static Map<String, Map<String, Map<String, Set<String>>>> configMap = new HashMap<>();

    private String componentName;

    /**
     * Clears out any configuration already created, good to call between test cases.
     */
    public static void resetConfiguration() {
        configMap.clear();
    }

    @Override
    public void init(String componentName, Object session) throws ConfigurationException {
        this.componentName = componentName;
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<>());
        }
    }

    @Override
    public Map getConfiguration(String realm, String configName) throws ConfigurationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<>());
            return Collections.EMPTY_MAP;
        }
        if (configName == null) {
            return configMap.get(componentName).get(ORG_CONFIG);
        } else {
            return configMap.get(componentName).get(configName);
        }
    }

    @Override
    public void setConfiguration(String realm, String configName, Map avPairs)
            throws ConfigurationException, UnsupportedOperationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<>());
        }
        if (configName == null) {
            if (configMap.get(componentName).containsKey(ORG_CONFIG)) {
                configMap.get(componentName).get(ORG_CONFIG).putAll(avPairs);
            } else {
                configMap.get(componentName).put(ORG_CONFIG, new HashMap(avPairs));
            }
        } else {
            if (configMap.get(componentName).containsKey(configName)) {
                configMap.get(componentName).get(configName).putAll(avPairs);
            } else {
                configMap.get(componentName).put(configName, new HashMap(avPairs));
            }
        }
    }

    @Override
    public void createConfiguration(String realm, String configName, Map avPairs)
            throws ConfigurationException, UnsupportedOperationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<>());
        }
        if (configName == null) {
            if (configMap.get(componentName).containsKey(ORG_CONFIG)) {
                configMap.get(componentName).get(ORG_CONFIG).putAll(avPairs);
            } else {
                configMap.get(componentName).put(ORG_CONFIG, new HashMap(avPairs));
            }
        } else {
            if (configMap.get(componentName).containsKey(configName)) {
                configMap.get(componentName).get(configName).putAll(avPairs);
            } else {
                configMap.get(componentName).put(configName, new HashMap(avPairs));
            }
        }
    }

    @Override
    public void deleteConfiguration(String realm, String configName, Set attributes)
            throws ConfigurationException, UnsupportedOperationException {

        // TODO
        // If the set of attributes is null/empty, remove the whole configName item otherwise
        // find the configName item and remove the attributes from it.
    }

    @Override
    public Set getAllConfigurationNames(String realm) throws ConfigurationException, UnsupportedOperationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<>());
            return Collections.EMPTY_SET;
        }
        return configMap.get(componentName).keySet();
    }

    @Override
    public String addListener(ConfigurationListener listener)
            throws ConfigurationException, UnsupportedOperationException {
        return "dummyListenerId";
    }

    @Override
    public void removeListener(String listenerID) throws ConfigurationException, UnsupportedOperationException {
    }

    public static void configureWsFed(String realm, String metadataPath, String extendedPath) throws Exception {
        String metadata = getFileContentFromClassPath(TestCaseConfigurationInstance.class, metadataPath);
        String extendedMetadata = getFileContentFromClassPath(TestCaseConfigurationInstance.class, extendedPath);
        WSFederationMetaManager metaManager = new WSFederationMetaManager();
        metaManager.createFederation(realm, (FederationElement) convertStringToJAXB(metadata));
        metaManager.createEntityConfig(realm, (FederationConfigElement) convertStringToJAXB(extendedMetadata));
    }

    public static void configureSaml2(String realm, String metadataPath, String extendedPath) throws Exception {
        String metadata = getFileContentFromClassPath(TestCaseConfigurationInstance.class, metadataPath);
        String extendedMetadata = getFileContentFromClassPath(TestCaseConfigurationInstance.class, extendedPath);
        SAML2MetaManager metaManager = new SAML2MetaManager();
        metaManager.createEntityDescriptor(realm,
                (EntityDescriptorElementImpl) SAML2MetaUtils.convertStringToJAXB(metadata));
        metaManager.createEntityConfig(realm,
                (EntityConfigElement) SAML2MetaUtils.convertStringToJAXB(extendedMetadata));
    }
}