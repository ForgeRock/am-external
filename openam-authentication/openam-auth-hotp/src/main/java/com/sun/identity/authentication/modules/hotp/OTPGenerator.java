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
 * Copyright 2021 ForgeRock AS.
 */
package com.sun.identity.authentication.modules.hotp;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Defines A Class that can generate a One Time Password.
 */
public interface OTPGenerator {

    /**
     * Generates an One Time Password using the given parameters.
     *
     * @param secret the shared secret.
     * @param movingFactor the counter, time, or other value that changes on a per use basis.
     * @param codeDigits the number of digits in the OTP, not including the checksum, if any.
     * @param addChecksum a flag that indicates if a checksum digit should be appended to the OTP.
     * @param truncationOffset the offset into the MAC result to begin truncation.  If this value is out of the range
     *                         of 0 ... 15, then dynamic truncation  will be used. Dynamic truncation is when the last
     *                         4 bits of the last byte of the MAC are used to determine the start offset.
     * @throws NoSuchAlgorithmException if no provider makes either HmacSHA1 or HMAC-SHA-1 digest algorithms available.
     * @throws InvalidKeyException The secret provided was not a valid HMAC-SHA-1 key.
     * @return A numeric String in base 10 that includes
     */
    String generateOTP(byte[] secret, long movingFactor, int codeDigits, boolean addChecksum,
                              int truncationOffset) throws NoSuchAlgorithmException, InvalidKeyException;
}
