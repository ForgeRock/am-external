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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions Copyrighted 2024-2025 Ping Identity Corporation
 */

package org.forgerock.openam.radius.common.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.packet.FramedIPAddressAttribute.Type.NAS_ASSIGNED;
import static org.forgerock.openam.radius.common.packet.FramedIPAddressAttribute.Type.USER_NEGOTIATED;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedIPAddressAttribute}.
 */
public class FramedIPAddressAttributeTest {

    @Test
    void testUserNegotiated() {
        FramedIPAddressAttribute a;
        a = new FramedIPAddressAttribute(USER_NEGOTIATED, 1, 2, 3, 4);
        assertThat(a.isUserNegotiated()).isTrue();
        assertThat(a.isNasSelected()).isFalse();
        assertThat(a.isSpecified()).isFalse();
    }

    @Test
    void testIsNasSelected() {
        final FramedIPAddressAttribute a = new FramedIPAddressAttribute(NAS_ASSIGNED, 1, 2, 3, 4);
        assertThat(a.isUserNegotiated()).isFalse();
        assertThat(a.isNasSelected()).isTrue();
        assertThat(a.isSpecified()).isFalse();
    }

    @Test
    void testIsSpecified() {
        FramedIPAddressAttribute a;
        a = new FramedIPAddressAttribute(FramedIPAddressAttribute.Type.SPECIFIED, 192, 168, 1, 3);
        assertThat(a.isUserNegotiated()).isFalse();
        assertThat(a.isNasSelected()).isFalse();
        assertThat(a.isSpecified()).isTrue();
        assertThat(a.getAddress()[0]).isEqualTo((byte) 192);
        assertThat(a.getAddress()[1]).isEqualTo((byte) 168);
        assertThat(a.getAddress()[2]).isEqualTo((byte) 1);
        assertThat(a.getAddress()[3]).isEqualTo((byte) 3);
    }
}
