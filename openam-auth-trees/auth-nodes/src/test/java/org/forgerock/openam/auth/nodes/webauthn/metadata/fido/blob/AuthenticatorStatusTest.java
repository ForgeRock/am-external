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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class AuthenticatorStatusTest {

    //@Checkstyle:off LineLength
    /**
     * Test that the enum values are in the correct format as provided in the
     * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#authenticatorstatus-enum">
     *     FIDO Metadata Service V3 Specification</a>.
     * <p>
     * This test has been added to ensure that the enum values are always kept in the same format specified in the spec,
     * e.g. FIDO_CERTIFIED_L1plus, even if this does not meet our usual naming conventions.
     */
    //@Checkstyle:on LineLength
    @Test
    void testCorrectFormat() {
        assertThat(AuthenticatorStatus.values()).anyMatch(status -> status.name().contains("plus"));
    }

}
