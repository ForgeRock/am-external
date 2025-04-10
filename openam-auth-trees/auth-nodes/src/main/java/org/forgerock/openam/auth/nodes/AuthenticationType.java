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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

/**
 * The client type used to authenticate with the authenticator.
 */
public enum AuthenticationType {

    /**
     * Biometric Only.
     */
    BIOMETRIC_ONLY("Biometric Only"),
    /**
     * Biometric and allow device credential fallback.
     */
    BIOMETRIC_ALLOW_FALLBACK("Biometric and allow device credential fallback"),
    /**
     * Application Pin.
     */
    APPLICATION_PIN("Application Pin"),
    /**
     * Authentication is not required.
     */
    NONE("Authentication is not required");

    private final String value;

    /**
     * The constructor.
     *
     * @param value the value.
     */
    AuthenticationType(String value) {
        this.value = value;
    }

    /**
     * Returns the client type value.
     *
     * @return the value.
     */
    public String getValue() {
        return value;
    }
}
