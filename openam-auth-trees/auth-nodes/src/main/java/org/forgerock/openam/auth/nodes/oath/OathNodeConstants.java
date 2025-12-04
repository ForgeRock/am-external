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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

/**
 * Shared constants use by the OATH authentication nodes.
 */
public final class OathNodeConstants {

    private OathNodeConstants() {
        // do nothing
    }

    /**
     * DEFAULT VALUES.
     */

    /** Default minimum shared secret length. */
    public static final int DEFAULT_MIN_SHARED_SECRET_LENGTH = 32;
    /** Default totp time interval. */
    public static final int DEFAULT_TOTP_INTERVAL = 30;
    /** Default Truncation Offset value. */
    public static final int DEFAULT_TRUNCATION_OFFSET = -1;
    /** Default HOTP Window Size. */
    public static final int DEFAULT_HOTP_WINDOW_SIZE = 100;
    /** Default TOTP Time Steps value. */
    public static final int DEFAULT_TOTP_TIME_STEPS = 2;
    /** Default Maximum Allowed Clock Drift value. */
    public static final int DEFAULT_MAXIMUM_ALLOWED_CLOCK_DRIFT = 5;
    /** Default Add Checksum value. */
    public static final boolean DEFAULT_CHECKSUM = false;
    /** Default Allow Recovery Codes value. */
    public static final boolean DEFAULT_ALLOW_RECOVERY_CODES = false;

    /**
     * QR CODE KEYS.
     */

    /** The key for the counter query component of the QR code. */
    static final String COUNTER_QR_CODE_KEY = "counter";
    /** The key for the period query component of the QR code. */
    static final String PERIOD_QR_CODE_KEY = "period";
    /** The key for the secret query component of the QR code. */
    static final String SECRET_QR_CODE_KEY = "secret";
    /** The key for the digits query component of the QR code. */
    static final String DIGITS_QR_CODE_KEY = "digits";
    /** The key for the algorithm query component of the QR code. */
    static final String ALGORITHM_QR_CODE_KEY = "algorithm";
    /** The key for the OATH resource id query component of the QR code. */
    static final String OATH_RESOURCE_ID_KEY = "oid";

    /** The key for URI schema. */
    static final String OATH_URI_SCHEME_QR_CODE_KEY = "otpauth";
    /** The key for host. */
    public static final String HOTP_URI_HOST_QR_CODE_KEY = "hotp";
    /** The key for host. */
    public static final String TOTP_URI_HOST_QR_CODE_KEY = "totp";

    /**
     * SHARED STATE KEYS.
     */

    /** The key for the device profile. */
    public static final String OATH_DEVICE_PROFILE_KEY = "oathDeviceProfile";
    /** The key for the recovery code flag. */
    public static final String OATH_ENABLE_RECOVERY_CODE_KEY = "oathEnableRecoveryCode";
}
