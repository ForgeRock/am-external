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
 * $Id: EncryptionConfig.java,v 1.2 2008/06/25 05:47:48 qcheng Exp $
 *
 * Portions Copyrighted 2019 ForgeRock AS.
 */
package com.sun.identity.saml2.key;

import java.security.Key;
import java.util.Optional;

/**
 * <code>EncryptionConfig</code> is a class for keeping encryption information such as the key-wrapping key, the data
 * encryption algorithm, data encryption key strength and key transport algorithm in one place.
 */ 
public class EncryptionConfig {
    
    private final Key wrappingKey;
    private final String dataEncAlgorithm;
    private final int dataEncStrength;
    private final String keyTransportAlgorithm;
    private final Optional<RsaOaepConfig> rsaOaepConfig;

    /**
     * Constructor for <code>EncryptionConfig</code>.
     * @param wrappingKey Key-wrapping key
     * @param dataEncAlgorithm Data encryption algorithm
     * @param dataEncStrength Data encryption key size
     * @param keyTransportAlgorithm The key transport algorithm.
     * @param rsaOaepConfig The optional RSA OAEP encryption related configuration.
     */
    public EncryptionConfig(Key wrappingKey, String dataEncAlgorithm, int dataEncStrength,
            String keyTransportAlgorithm, Optional<RsaOaepConfig> rsaOaepConfig) {
        this.wrappingKey = wrappingKey;
        this.dataEncAlgorithm = dataEncAlgorithm;
        this.dataEncStrength = dataEncStrength;
        this.keyTransportAlgorithm = keyTransportAlgorithm;
        this.rsaOaepConfig = rsaOaepConfig;
    }

    /**
     * Returns the key for encrypting the secret key.
     * @return <code>Key</code> for encrypting/wrapping the secretKey
     */
    public Key getWrappingKey() {
        return wrappingKey;
    }

    /**
     * Returns the algorithm for data encryption.
     * @return <code>String</code> for data encryption algorithm
     */
    public String getDataEncAlgorithm() {
        return dataEncAlgorithm;
    }
    
    /**
     * Returns the key strength for data encryption.
     * @return <code>int</code> for data encryption strength
     */
    public int getDataEncStrength() {
        return dataEncStrength;
    }

    /**
     * Returns the key transport algorithm.
     *
     * @return The key transport algorithm.
     */
    public String getKeyTransportAlgorithm() {
        return keyTransportAlgorithm;
    }

    /**
     * Returns the optional RSA OAEP configuration.
     *
     * @return If the key transport algorithm is not RSA OAEP based encryption, this will be an empty optional object.
     */
    public Optional<RsaOaepConfig> getRsaOaepConfig() {
        return rsaOaepConfig;
    }
}
