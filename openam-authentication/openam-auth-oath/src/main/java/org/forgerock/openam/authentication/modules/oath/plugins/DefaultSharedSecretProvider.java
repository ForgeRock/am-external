/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oath.plugins;

import javax.xml.bind.DatatypeConverter;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;

/**
 * Default implementation of the {@link SharedSecretProvider } and this may also be used as an extension
 * point to allow implementation of additional logic.
 */
public class DefaultSharedSecretProvider implements SharedSecretProvider {

    protected String amAuthOATH = "amAuthOATH";
    private final Debug debug = Debug.getInstance(amAuthOATH);

    /**
     * Processes the shared secret value retrieved for the user.
     * The default implementation takes the HEX encoded shared secret and converts it into
     * a byte array.
     *
     * @param secretKeyValue the secret key attribute
     * @return the processed shared secret
     * @throws AuthLoginException if an error processing the secret key
     */
    @Override
    public byte[] getSharedSecret(String secretKeyValue) throws AuthLoginException {

        byte[] secretKeyBytes = DatatypeConverter.parseHexBinary(prepareSecretKey(secretKeyValue));
        if (debug.messageEnabled()){
            debug.message("DefaultSharedSecretProvider returning secretKey value");
        }
        return secretKeyBytes;
    }

    /**
     * Prepares the secret key string for conversion to an array of bytes.
     *
     * @param secretKey the secret key value
     * @return a string value compatible with the {@link DatatypeConverter}
     */
    private String prepareSecretKey(String secretKey) {
        // get rid of white space in string (messes with the data converter)
        secretKey = secretKey.replaceAll("\\s+", "");
        // make sure secretkey is even length
        if ((secretKey.length() % 2) != 0) {
            secretKey = "0" + secretKey;
        }
        return secretKey;
    }
}
