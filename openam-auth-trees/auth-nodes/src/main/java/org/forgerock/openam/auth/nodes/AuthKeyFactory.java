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
 * Copyright 2017-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.Key;

import org.forgerock.openam.utils.AMKeyProvider;
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
        return amKeyProvider.getPrivateKey(keyAlias);
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
        return amKeyProvider.getPublicKey(keyAlias);
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
