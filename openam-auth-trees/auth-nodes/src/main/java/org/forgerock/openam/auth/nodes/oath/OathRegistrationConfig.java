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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_CHECKSUM;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MIN_SHARED_SECRET_LENGTH;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TRUNCATION_OFFSET;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorCommonConfig;

/**
 * The OATH Registration node configuration.
 */
public interface OathRegistrationConfig extends MultiFactorCommonConfig {

    /**
     * Specifies the One Time Password length.
     *
     * @return the length of the generated OTP in digits.
     */
    @Attribute(order = 70)
    default NumberOfDigits passwordLength() {
        return NumberOfDigits.SIX_DIGITS;
    }

    /**
     * Specifies the Minimum Secret Key Length.
     *
     * @return the minimum number of hexadecimal characters allowed for the Secret Key.
     */
    @Attribute(order = 80)
    default int minSharedSecretLength() {
        return DEFAULT_MIN_SHARED_SECRET_LENGTH;
    }

    /**
     * Specifies the OATH Algorithm to Use.
     *
     * @return the algorithm the device uses to generate the OTP.
     */
    @Attribute(order = 90)
    default OathAlgorithm algorithm() {
        return OathAlgorithm.TOTP;
    }

    /**
     * Specifies the TOTP time step interval.
     *
     * @return the TOTP time step in seconds that the OTP device uses to generate the OTP.
     */
    @Attribute(order = 100)
    default int totpTimeInterval() {
        return DEFAULT_TOTP_INTERVAL;
    }

    /**
     * Specifies the TOTP hash algorithm.
     *
     * @return the TOTP hash algorithm to be used to generate the OTP.
     */
    @Attribute(order = 110)
    default HashAlgorithm totpHashAlgorithm() {
        return HashAlgorithm.HMAC_SHA1;
    }

    /**
     * Specifies if a Checksum Digit should be added.
     *
     * @return true if a checksum digit is to be added to the OTP.
     */
    @Attribute(order = 120)
    default boolean addChecksum() {
        return DEFAULT_CHECKSUM;
    }

    /**
     * Specifies the Truncation Offset.
     *
     * @return the value of an offset to the generation of the OTP.
     */
    @Attribute(order = 130)
    default int truncationOffset() {
        return DEFAULT_TRUNCATION_OFFSET;
    }

    /**
     * Specifies whether if the node completes successfully to save the device profile directly or
     * to postpone the device profile storage and place the information into the transient state for
     * storage by later nodes.
     *
     * @return true if the device profile is to be postponed.
     */
    @Attribute(order = 140)
    default boolean postponeDeviceProfileStorage() {
        return false;
    }

}
