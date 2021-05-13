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
 * Copyright 2018-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.mfa;

/**
 * Shared constants use by the second factor authentication nodes.
 */
public final class MultiFactorConstants {

    private MultiFactorConstants() {
        // do nothing
    }

    /**
     * MULTI-FACTOR AUTHENTICATION METHODS.
     */

    /** The key for MFA method on shared state. */
    public static final String MFA_METHOD = "mfaMethod";
    /** The value of MFA Push method on shared state. */
    public static final String PUSH_METHOD = "push";
    /** The value of MFA Oath method on shared state. */
    public static final String OATH_METHOD = "oath";
    /** The value of MFA WebAuthn method on shared state. */
    public static final String WEBAUTHN_METHOD = "webauthn";

    /**
     * COMMON QR CODE KEYS.
     */

    /** The key for the image query component of the QR code. */
    public static final String IMG_QR_CODE_KEY = "image";
    /** The key for the bgcolour query component of the QR code. */
    public static final String BGCOLOUR_QR_CODE_KEY = "b";
    /** The key for the issuer of the QR code. */
    public static final String ISSUER_QR_CODE_KEY = "issuer";
    /** The key for path. */
    public static final String URI_PATH_QR_CODE_KEY = "forgerock";
    /** The id of the HiddenCallback containing the URI. */
    public static final String HIDDEN_CALLCABK_ID = "mfaDeviceRegistration";

    /**
     * RECOVERY CODE KEYS.
     */

    /** The key for the recovery codes. */
    public static final String RECOVERY_CODE_KEY = "recoveryCodes";
    /** The key for the device name. */
    public static final String RECOVERY_CODE_DEVICE_NAME = "recoveryCodeDeviceName";
    /** The number of recovery codes to generate for a newly minted device. */
    public static final int NUM_RECOVEY_CODES = 10;


    /** Scan the QR Code message.*/
    public static final String SCAN_QR_CODE_MSG = "Scan the barcode image below with the ForgeRock Authenticator app "
            + "to register your device with your login.";

    /**
     * DEFAULT VALUES.
     */

    /** Default Issuer name. */
    public static final String DEFAULT_ISSUER = "ForgeRock";
    /** Default background color. */
    public static final String DEFAULT_BG_COLOR = "032b75";
    /** Default timeout value. */
    public static final int DEFAULT_TIMEOUT = 60;
    /** Default enable generate recovery code. */
    public static final boolean DEFAULT_GENERATE_RECOVERY_CODES = true;
}
