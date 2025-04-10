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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

import java.security.InvalidKeyException;

import javax.xml.bind.DatatypeConverter;

import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;

/**
 * This class verifies the OTP code using the OATH HOTP algorithm.
 */
public class OathHotpVerifier extends AbstractOathVerifier {

    /**
     * The constructor.
     *
     * @param config the node configuration.
     * @param settings the oath device settings.
     */
    public OathHotpVerifier(OathTokenVerifierNode.Config config, OathDeviceSettings settings) {
        super(config, settings);
    }

    @Override
    void verify(String otp) throws OathVerificationException {
        int counter = settings.getCounter();
        byte[] sharedSecretBytes = DatatypeConverter.parseHexBinary(getSharedSecret());
        String algorithm = "Hmac" + HashAlgorithm.HMAC_SHA1.toString();

        for (int i = 0; i <= config.hotpWindowSize(); i++) {
            String otpGen;
            try {
                otpGen = generateOTP(sharedSecretBytes, counter + i, otp.length(),
                        settings.getChecksumDigit(), settings.getTruncationOffset(), algorithm);
            } catch (Exception e) {
                throw new OathVerificationException(e.getMessage(), e);
            }
            if (isEqual(otpGen, otp)) {
                // If OTP is correct set the counter value to counter+i (+1 for having been successful)
                settings.setCounter(counter + i + 1);
                return;
            }
        }

        throw new OathVerificationException();
    }

    private static int calcChecksum(long num, int digits) {
        boolean doubleDigit = true;
        int total = 0;
        while (0 < digits--) {
            int digit = (int) (num % 10);
            num /= 10;
            if (doubleDigit) {
                digit = DOUBLE_DIGITS[digit];
            }
            total += digit;
            doubleDigit = !doubleDigit;
        }
        int result = total % 10;
        if (result > 0) {
            result = 10 - result;
        }
        return result;
    }

    private String generateOTP(byte[] secret, long movingFactor, int codeDigits, boolean addChecksum,
            int truncationOffset, String crypto) throws InvalidKeyException {

        int digits = addChecksum ? (codeDigits + 1) : codeDigits;
        byte[] textBytes = toTextByteArray(movingFactor);
        byte[] hashBytes = hmacSha(crypto, secret, textBytes);
        int binary = getBinary(truncationOffset, hashBytes, true);

        int otp = binary % DIGITS_POWER[codeDigits];
        if (addChecksum) {
            otp = (otp * 10) + calcChecksum(otp, codeDigits);
        }

        StringBuilder result = new StringBuilder(Integer.toString(otp));
        while (result.length() < digits) {
            result.insert(0, "0");
        }

        return result.toString();
    }

    private byte[] toTextByteArray(long movingFactor) {
        byte[] textBytes = new byte[8];
        for (int i = textBytes.length - 1; i >= 0; i--) {
            textBytes[i] = (byte) (movingFactor & 0xff);
            movingFactor >>= 8;
        }
        return textBytes;
    }

}
