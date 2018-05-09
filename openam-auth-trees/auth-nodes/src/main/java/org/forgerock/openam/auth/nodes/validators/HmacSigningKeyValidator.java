/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import org.forgerock.openam.sm.validation.Base64EncodedBinaryValidator;

/**
 * Validatates an attribute is set to a single 256-bit base64 encoded value suitable for a HS256 signing key.
 */
public class HmacSigningKeyValidator extends Base64EncodedBinaryValidator {

    private static final int BYTES_IN_256_BITS = 256 / 8;

    /**
     * Determines the minimum size of the base64-decoded binary value, in bytes. Defaults to 32 bytes (256 bits).
     *
     * @return the minimum required size of the signing key in bytes.
     */
    @Override
    protected int getMinimumSize()  {
        return BYTES_IN_256_BITS;
    }
}
