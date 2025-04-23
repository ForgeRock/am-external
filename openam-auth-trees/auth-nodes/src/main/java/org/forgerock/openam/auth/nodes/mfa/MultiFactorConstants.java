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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
    /** The key for authenticator policies of the QR code. */
    public static final String POLICIES_QR_CODE_KEY = "policies";
    /** The key for the userId query component of the QR code. */
    public static final String USER_ID_QR_CODE_KEY = "uid";

    /**
     * RECOVERY CODE KEYS.
     */

    /** The key for the recovery codes. */
    public static final String RECOVERY_CODE_KEY = "recoveryCodes";
    /** The key for the device name. */
    public static final String RECOVERY_CODE_DEVICE_NAME = "recoveryCodeDeviceName";
    /** The number of recovery codes to generate for a newly minted device. */
    public static final int NUM_RECOVEY_CODES = 10;


    /** Scan the QR Code message key.*/
    public static final String SCAN_QR_CODE_MSG_KEY = "default.scanQRCodeMessage";

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
