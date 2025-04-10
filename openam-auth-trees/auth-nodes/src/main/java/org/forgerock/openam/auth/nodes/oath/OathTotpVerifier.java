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

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class verifies the OTP code using the OATH TOTP algorithm.
 */
public class OathTotpVerifier extends AbstractOathVerifier {

    private final Logger logger = LoggerFactory.getLogger(OathTotpVerifier.class);

    private final long timeInMilliseconds;

    /**
     * The constructor.
     *
     * @param config the node configuration.
     * @param settings the oath device settings.
     */
    public OathTotpVerifier(OathTokenVerifierNode.Config config, OathDeviceSettings settings) {
        this(config, settings, currentTimeMillis() / 1000L);
    }

    @VisibleForTesting
    OathTotpVerifier(OathTokenVerifierNode.Config config, OathDeviceSettings settings, long time) {
        super(config, settings);
        this.timeInMilliseconds = time;
    }

    @Override
    public void verify(String otp) throws OathVerificationException {
        long lastLoginTimeStep = settings.getLastLogin() / config.totpTimeInterval();

        // Check TOTP values for validity
        if (lastLoginTimeStep < 0) {
            throw new OathVerificationException("Invalid login time value");
        }

        // Get Time Step
        long localTime = (timeInMilliseconds / config.totpTimeInterval())
                + (settings.getClockDriftSeconds() / config.totpTimeInterval());

        if (lastLoginTimeStep == localTime) {
            throw new OathVerificationException("Login failed attempting to use the same OTP in same Time Step: "
                    + localTime);
        }

        boolean sameWindow = false;

        // Check if we are in the time window to prevent 2 logins within the window using the same OTP
        if (lastLoginTimeStep >= (localTime - config.totpTimeSteps())
                && lastLoginTimeStep <= (localTime + config.totpTimeSteps())) {
            logger.debug("Logging in in the same TOTP window");
            sameWindow = true;
        }

        String passLenStr = String.valueOf(otp.length());
        String algorithm = "Hmac" + config.totpHashAlgorithm().toString();
        String otpGen = generateTOTP(getSharedSecret(), Long.toHexString(localTime), passLenStr, algorithm);

        if (isEqual(otpGen, otp)) {
            checkDrift(localTime);
            updateDeviceSettings(localTime, settings);
            return;
        }

        for (int i = 1; i <= config.totpTimeSteps(); i++) {
            long time1 = localTime + i;
            long time2 = localTime - i;

            //check time step after current time
            otpGen = generateTOTP(getSharedSecret(), Long.toHexString(time1), passLenStr, algorithm);

            if (isEqual(otpGen, otp)) {
                checkDrift(time1);
                updateDeviceSettings(time1, settings);
                return;
            }

            //check time step before current time
            otpGen = generateTOTP(getSharedSecret(), Long.toHexString(time2), passLenStr, algorithm);

            if (isEqual(otpGen, otp) && sameWindow) {
                logger.error("Logging in in the same window with a OTP that is "
                        + "older than the current times OTP");
                throw new OathVerificationException();
            } else if (isEqual(otpGen, otp) && !sameWindow) {
                checkDrift(time2);
                updateDeviceSettings(time2, settings);
                return;
            }
        }

        throw new OathVerificationException();
    }

    private void updateDeviceSettings(long localTime, OathDeviceSettings settings) {
        settings.setLastLogin(localTime * config.totpTimeInterval(), TimeUnit.SECONDS);
        settings.setClockDriftSeconds((int) getDrift(localTime) * config.totpTimeInterval());
    }

    private static String generateTOTP(String key, String time, String returnDigits, String crypto) {
        int codeDigits = Integer.decode(returnDigits);

        StringBuilder timeBuilder = new StringBuilder(time);
        while (timeBuilder.length() < 16) {
            timeBuilder.insert(0, "0");
        }
        time = timeBuilder.toString();

        byte[] msgBytes = hexStringToBytes(time);
        byte[] keyBytes = hexStringToBytes(key);
        byte[] hashBytes = hmacSha(crypto, keyBytes, msgBytes);

        int binary = getBinary(0, hashBytes, false);
        int otp = binary % DIGITS_POWER[codeDigits];

        StringBuilder result = new StringBuilder(Integer.toString(otp));
        while (result.length() < codeDigits) {
            result.insert(0, "0");
        }

        return result.toString();
    }

    private long getDrift(long localTime) {
        return localTime - (timeInMilliseconds / config.totpTimeInterval());
    }

    private void checkDrift(long localTime)  throws OathVerificationException  {
        if (Math.abs(getDrift(localTime)) > config.maximumAllowedClockDrift()) {
            throw new OathVerificationException("OTP is out of sync.");
        }
    }

    private static byte[] hexStringToBytes(String hex) {
        byte[] byteArray = new BigInteger("10" + hex, 16).toByteArray();
        byte[] result = new byte[byteArray.length - 1];
        if (result.length > 0) {
            System.arraycopy(byteArray, 1, result, 0, result.length);
        }
        return result;
    }
}
