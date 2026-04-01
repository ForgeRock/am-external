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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.securid;

import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PASSWORD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PUSH;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.VOICE_OR_SMS;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SecurID authentication methods.
 */
enum SecurIdMethod {

    /** Authenticate Tokencode. */
    TOKEN("Authenticate Tokencode", "Enter Tokencode", PASSWORD),
    /** SecurID. */
    SECURID("SecurID", "Enter PASSCODE", PASSWORD),
    /** SecurID New PIN. */
    SECURID_NEW_PIN("SecurId New PIN", "Enter New PIN", PASSWORD),
    /** Emergency Access Code. */
    EMERGENCY_TOKENCODE("Emergency Access Code", "Enter your Emergency Access Code", PASSWORD),
    /** Approve. */
    APPROVE("Approve", null, PUSH),
    /** Device Biometrics. */
    FINGERPRINT("Device Biometrics", null, PUSH),
    /** QR Code. */
    QRCODE("QR Code", null, PUSH),
    /** SMS Tokencode. */
    SMS("SMS Tokencode", "Enter the six-digit code sent to your phone", VOICE_OR_SMS),
    /** Voice Tokencode. */
    VOICE("Voice Tokencode", "Enter the six-digit code sent to your phone", VOICE_OR_SMS);

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurIdMethod.class);

    private final String displayName;
    private final String prompt;
    private final SecurIdMethodType type;

    SecurIdMethod(String displayName, String prompt, SecurIdMethodType type) {
        this.displayName = displayName;
        this.prompt = prompt;
        this.type = type;
    }

    /**
     * Returns the SecurIdMethod corresponding to the given method ID.
     *
     * @param methodId the display name
     * @return the corresponding SecurIdMethod, or null if not found
     */
    static SecurIdMethod fromMethodId(String methodId) {
        if ("SMS_REQUEST_CODE".equals(methodId)) {
            return SMS;
        }
        if ("VOICE_REQUEST_CODE".equals(methodId)) {
            return VOICE;
        }
        return Arrays.stream(SecurIdMethod.values())
                .filter(method -> method.name().equalsIgnoreCase(methodId))
                .findAny()
                .orElseGet(() -> {
                    LOGGER.warn("Unrecognized SecurID method ID: {}", methodId);
                    return null;
                });
    }

    /**
     * Returns the display name of this SecurID method.
     *
     * @return the display name
     */
    String displayName() {
        return displayName;
    }

    /**
     * Returns the prompt for this SecurID method.
     *
     * @return the prompt
     */
    String prompt() {
        return prompt;
    }

    /**
     * Returns the type of this SecurID method.
     *
     * @return the method type
     */
    SecurIdMethodType type() {
        return type;
    }

    /**
     * SecurID method types.
     */
    enum SecurIdMethodType {
        PASSWORD, PUSH, VOICE_OR_SMS
    }
}
