/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyStore;

import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;


/**
 * Provides methods which a Node can use to get keys from the underlying AM keystore.
 */
public class AuthKeyFactory {

    private static final String AUTH_KEY_ALIAS = "iplanet-am-auth-key-alias";
    private static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Gets the private Auth key from the AM keystore.
     *
     * @param amKeyProvider provides access to the am keystore.
     * @param orgName       the name of the org.
     * @return The auth key.
     * @throws FileNotFoundException if the keystore file is not found.
     * @throws SSOException          if the credentials are incorrect.
     * @throws SMSException          an exception from SMS, config related.
     */
    public Key getPrivateAuthKey(AMKeyProvider amKeyProvider, String orgName) throws FileNotFoundException,
            SSOException, SMSException {
        logger.debug("getPrivateAuthKey method started");
        String keyAlias = getKeyAlias(orgName);
        logger.debug("keyAlias {}", keyAlias);
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(amKeyProvider.getKeystoreFilePath())
                .withPassword(amKeyProvider.getKeystorePass())
                .withKeyStoreType(amKeyProvider.getKeystoreType())
                .build();
        logger.debug("Keystore built successfully");
        return new KeyStoreManager(keyStore).getPrivateKey(keyAlias, amKeyProvider.getPrivateKeyPass());
    }

    /**
     * Gets the public Auth key from the AM keystore.
     *
     * @param amKeyProvider provides access to the am keystore.
     * @param orgName       the name of the org.
     * @return The auth key.
     * @throws FileNotFoundException if the keystore file is not found.
     * @throws SSOException          if the credentials are incorrect.
     * @throws SMSException          an exception from SMS, config related.
     */
    public Key getPublicAuthKey(AMKeyProvider amKeyProvider, String orgName) throws FileNotFoundException, SSOException,
            SMSException {
        logger.debug("getPublicAuthKey started");
        String keyAlias = getKeyAlias(orgName);
        logger.debug("keyAlias {}", keyAlias);
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(amKeyProvider.getKeystoreFilePath())
                .withPassword(amKeyProvider.getKeystorePass())
                .withKeyStoreType(amKeyProvider.getKeystoreType())
                .build();
        logger.debug("KeyStore built successfully");
        return new KeyStoreManager(keyStore).getPublicKey(keyAlias);
    }

    private String getKeyAlias(String orgName) throws SMSException, SSOException {
        ServiceConfig orgConfig = getServiceConfigManager().getOrganizationConfig(orgName, null);
        return CollectionHelper.getMapAttr(orgConfig.getAttributes(), AUTH_KEY_ALIAS);
    }

    private ServiceConfigManager getServiceConfigManager() throws SSOException, SMSException {
        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        return new ServiceConfigManager(AUTH_SERVICE_NAME, token);
    }
}
