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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob;

import static org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatusType.CERTIFICATION;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatusType.INFO;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatusType.SECURITY;

/**
 * Models the {@literal AuthenticatorStatus} object defined in the FIDO MDS specification.
 * <p>
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#authenticatorstatus-enum">
 * 3.1.4 AuthenticatorStatus enum</a>
 * </p>
 * NOTE: The values contained in this enum are treated as ordinals and so the order of the constants is important and
 * should be maintained. Values at the bottom of this list are considered to have a higher priority status than those
 * at the top. These values will be used in comparisons when checking authenticator device status, and so their exact
 * names are important.
 */
public enum AuthenticatorStatus {
    //@Checkstyle:off JavadocVariable
    NOT_FIDO_CERTIFIED(CERTIFICATION),
    USER_VERIFICATION_BYPASS(SECURITY), // A status that signifies extra security information
    USER_KEY_REMOTE_COMPROMISE(SECURITY), // A status that signifies extra security information
    USER_KEY_PHYSICAL_COMPROMISE(SECURITY), // A status that signifies extra security information
    ATTESTATION_KEY_COMPROMISE(SECURITY), // A status that signifies extra security information
    UPDATE_AVAILABLE(INFO), // A status that signifies extra information
    SELF_ASSERTION_SUBMITTED(CERTIFICATION),
    FIDO_CERTIFIED(CERTIFICATION),
    FIDO_CERTIFIED_L1(CERTIFICATION),
    FIDO_CERTIFIED_L1plus(CERTIFICATION),
    FIDO_CERTIFIED_L2(CERTIFICATION),
    FIDO_CERTIFIED_L2plus(CERTIFICATION),
    FIDO_CERTIFIED_L3(CERTIFICATION),
    FIDO_CERTIFIED_L3plus(CERTIFICATION),
    REVOKED(CERTIFICATION); // At the end of the list as it supersedes all other statuses

    private final AuthenticatorStatusType type;

    AuthenticatorStatus(final AuthenticatorStatusType type) {
        this.type = type;
    }

    /**
     * Returns the {@link AuthenticatorStatusType} of the authenticator status.
     *
     * @return the type of the authenticator status
     */
    public AuthenticatorStatusType getType() {
        return type;
    }

    /**
     * Determines if the authenticator status meets (matches the level, or exceeds) the provided status.
     *
     * @param minimumStatus the minimum status the current status must meet
     * @return {@code true} if the current status meets the minimum status, {@code false} otherwise
     */
    public boolean isSatisfiedBy(AuthenticatorStatus minimumStatus) {
        return this.ordinal() >= minimumStatus.ordinal();
    }
}
