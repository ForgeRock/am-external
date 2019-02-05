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
 * Represents https://www.w3.org/TR/webauthn/#attestation-convey.
 */
public enum AttestationPreference {
    /** none attestation. */
    NONE("none"),
    /** indirect attestation. */
    INDIRECT("indirect"),
    /** direct attestation. */
    DIRECT("direct");

    private String value;

    /**
     * THe constructor.
     * @param value the value as a string.
     */
    AttestationPreference(String value) {
        this.value = value;
    }

    /**
     * Gets the attestation preference value.
     * @return the value.
     */
    public String getValue() {
        return value;
    }
}