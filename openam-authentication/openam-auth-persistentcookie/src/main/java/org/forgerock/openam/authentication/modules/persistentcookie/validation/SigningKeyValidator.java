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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.persistentcookie.validation;

import org.forgerock.openam.sm.validation.Base64EncodedBinaryValidator;

/**
 * Validatates an attribute is set to a single 256-bit base64 encoded value suitable for a HS256 signing key
 */
public class SigningKeyValidator extends Base64EncodedBinaryValidator {

    private static final int _256_BITS_IN_BYTES = 256 / 8;

    /**
     * Determines the minimum size of the base64-decoded binary value, in bytes. Defaults to 32 bytes (256 bits).
     *
     * @return the minimum required size of the signing key in bytes.
     */
    @Override
    protected int getMinimumSize()  {
        return _256_BITS_IN_BYTES;
    }
}
