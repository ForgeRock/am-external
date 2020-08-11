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
package org.forgerock.openam.auth.nodes.webauthn.data;

import java.util.BitSet;

/**
 * https://www.w3.org/TR/webauthn/#sec-authenticator-data.
 */
public class AttestationFlags {

    private BitSet flags;

    /**
     * Attestation Flag constructor.
     * @param flags the flags as bits.
     */
    public AttestationFlags(BitSet flags) {
        this.flags = flags;
    }

    /**
     * https://www.w3.org/TR/webauthn/#concept-user-present.
     * @return true if user is present.
     */
    public boolean isUserPresent() {
        return flags.get(0);
    }

    /**
     * https://www.w3.org/TR/webauthn/#concept-user-verified.
     * @return true if user is present.
     */
    public boolean isUserVerified() {
        return flags.get(2);
    }

    /**
     * Is attestation data included.
     * @return true if included.
     */
    public boolean isAttestedDataIncluded() {
        return flags.get(6);
    }

    /**
     * Is extension data included.
     * @return true if included.
     */
    public boolean isExtensionDataIncluded() {
        return flags.get(7);
    }
}
