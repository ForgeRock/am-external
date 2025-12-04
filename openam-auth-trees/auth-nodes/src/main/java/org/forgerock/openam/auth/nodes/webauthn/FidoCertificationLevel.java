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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import org.forgerock.openam.annotations.sm.I18nKey;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatus;

/**
 * Defines the level of FIDO certification that the authenticator must have.
 * <p>
 * This is a subset of the authenticator status defined in the FIDO MDS specification -
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#authenticatorstatus-enum">
 * 3.1.4 AuthenticatorStatus enum</a>
 * </p>
 */
public enum FidoCertificationLevel {
    /**
     * Metadata service not enabled.
     */
    @I18nKey("off")
    OFF,
    //@Checkstyle:off JavadocVariable
    @I18nKey("self-assertion")
    SELF_ASSERTION_SUBMITTED(AuthenticatorStatus.SELF_ASSERTION_SUBMITTED),
    @I18nKey("fido-certified-l1")
    FIDO_CERTIFIED_L1(AuthenticatorStatus.FIDO_CERTIFIED_L1),
    @I18nKey("fido-certified-l1plus")
    FIDO_CERTIFIED_L1plus(AuthenticatorStatus.FIDO_CERTIFIED_L1plus),
    @I18nKey("fido-certified-l2")
    FIDO_CERTIFIED_L2(AuthenticatorStatus.FIDO_CERTIFIED_L2),
    @I18nKey("fido-certified-l2plus")
    FIDO_CERTIFIED_L2plus(AuthenticatorStatus.FIDO_CERTIFIED_L2plus),
    @I18nKey("fido-certified-l3")
    FIDO_CERTIFIED_L3(AuthenticatorStatus.FIDO_CERTIFIED_L3),
    @I18nKey("fido-certified-l3plus")
    FIDO_CERTIFIED_L3plus(AuthenticatorStatus.FIDO_CERTIFIED_L3plus);
    //@Checkstyle:on JavadocVariable

    private final AuthenticatorStatus status;

    /**
     * Returns the corresponding {@link AuthenticatorStatus} for the certification level.
     * @return the status.
     */
    public AuthenticatorStatus getStatus() {
        return status;
    }

    FidoCertificationLevel() {
        this.status = null;
    }

    FidoCertificationLevel(AuthenticatorStatus status) {
        this.status = status;
    }
}
