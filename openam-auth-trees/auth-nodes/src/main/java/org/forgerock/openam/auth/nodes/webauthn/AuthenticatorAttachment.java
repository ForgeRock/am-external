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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

/**
 * An implementation of https://www.w3.org/TR/webauthn/#enumdef-authenticatorattachment.
 */
public enum AuthenticatorAttachment {
    /**
     * Unspecified preference.
     */
    UNSPECIFIED(""),
    /**
     * Authenticator must by part of the platform, such as an in built fingerprint reader.
     */
    PLATFORM("platform"),
    /**
     * Authenticators can be external to the platform, such as a USB attached touch ID.
     */
    CROSS_PLATFORM("cross-platform");

    private String value;

    /**
     * The constructor.
     * @param value the preference value.
     */
    AuthenticatorAttachment(String value) {
        this.value = value;
    }

    /**
     * Returns the preference value.
     * @return the value.
     */
    public String getValue() {
        return value;
    }
}
