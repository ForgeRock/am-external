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
 * Copyright 2017 ForgeRock AS.
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
            configMap.put(componentName, new HashMap<String, Map<String, Set<String>>>());
        }
    }

    @Override
    public Map getConfiguration(String realm, String configName) throws ConfigurationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<String, Map<String, Set<String>>>());
            return Collections.EMPTY_MAP;
        }
        return configMap.get(componentName).get(configName);
    }

    @Override
    public void setConfiguration(String realm, String configName, Map avPairs)
            throws ConfigurationException, UnsupportedOperationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<String, Map<String, Set<String>>>());
        }
        if (configMap.get(componentName).containsKey(configName)) {
            configMap.get(componentName).get(configName).putAll(avPairs);
        } else {
            configMap.get(componentName).put(configName, new HashMap(avPairs));
        }
    }

    @Override
    public void createConfiguration(String realm, String configName, Map avPairs)
            throws ConfigurationException, UnsupportedOperationException {
        if (!configMap.containsKey(componentName)) {
            configMap.put(componentName, new HashMap<String, Map<String, Set<String>>>());
        }
        if (configMap.get(componentName).containsKey(configName)) {
            configMap.get(componentName).get(configName).putAll(avPairs);
        } else {
            configMap.get(componentName).put(configName, new HashMap(avPairs));
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
            configMap.put(componentName, new HashMap<String, Map<String, Set<String>>>());
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
