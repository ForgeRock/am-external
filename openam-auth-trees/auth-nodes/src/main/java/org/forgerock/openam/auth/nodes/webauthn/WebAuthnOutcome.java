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
package org.forgerock.openam.auth.nodes.webauthn;

import java.util.Optional;

/**
 * Represents the outcome of a WebAuthn operation.
 *
 * @param legacyData The original :: separated format used by the javascript returned by AM and the Ping SDK.
 * @param authenticatorAttachment The authenticator attachment type (e.g. "platform", "cross-platform").
 *                                Cannot be modelled as an enum as we must handle unknown values.
 * @param error An optional error that occurred during the operation.
 * @param isSupported Whether WebAuthn was supported by the browser.
 */
public record WebAuthnOutcome(String legacyData, Optional<String> authenticatorAttachment,
        Optional<WebAuthnDomException> error, boolean isSupported) {

    /**
     * Create a new unsupported outcome.
     * @return A new unsupported outcome.
     */
    public static WebAuthnOutcome unsupported() {
        return new WebAuthnOutcome(null, null, Optional.empty(), false);
    }

    /**
     * Create a new error outcome.
     * @param error The error that occurred.
     * @return A new error outcome.
     */
    public static WebAuthnOutcome error(WebAuthnDomException error) {
        return new WebAuthnOutcome(null, null, Optional.of(error), true);
    }

    /**
     * Create a new successful outcome.
     * @param legacyData The original :: separated format used by the javascript returned by AM and the Ping SDK.
     * @param authenticatorAttachment The authenticator attachment type (e.g. "platform", "cross-platform").
     */
    public WebAuthnOutcome(String legacyData, Optional<String> authenticatorAttachment) {
        this(legacyData, authenticatorAttachment, Optional.empty(), true);
    }

    /**
     * Returns whether an error occurred during the operation.
     * @return {@code true} if an error occurred, {@code false} otherwise.
     */
    public boolean isError() {
        return error.isPresent();
    }
}
