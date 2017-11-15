package org.forgerock.openam.authentication.modules.oath.plugins;

/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * Provided as an extension point to allow customised transformation of the OATH shared secret attribute.
 * @supported.all.api
 */
public interface SharedSecretProvider {

    /**
     * Takes the non-empty shared secret that is retrieved for a user and implements any processing needed for to
     * return the byte array of the string value.
     * the module will fail.
     *
     * @param secretKey  shared secret value
     * @return a byte array of the shared secret - this should not be null
     * @throws AuthLoginException if an error occurs transforming the value
     * and should cause the module to fail.
     */
    byte[] getSharedSecret(String secretKey) throws AuthLoginException;

}
