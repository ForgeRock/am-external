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
 * Portions Copyrighted 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.radius.common.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_MTU;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedMTUAttribute}.
 */
public class FramedMTUAttributeTest {

    @Test
    void testHighest() {
        final FramedMTUAttribute a = new FramedMTUAttribute(65535);
        assertThat(a.getMtu()).isEqualTo(65535).describedAs("mtu should be 65535");
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_MTU.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);

        final FramedMTUAttribute b = new FramedMTUAttribute(bytes);
        assertThat(b.getMtu()).isEqualTo(65535).describedAs("mtu created from octets should be 65535");
    }

    @Test
    void testHighestFromOctets() {
        final byte[] octets = new byte[] {
            (byte) FRAMED_MTU.getTypeCode(), 6, 0, 0, (byte) 255, (byte) 255
        };
        final FramedMTUAttribute a = new FramedMTUAttribute(octets);
        assertThat(a.getMtu()).isEqualTo(65535).describedAs("mtu should be 65535");
    }

    @Test
    void testLowest() {
        final FramedMTUAttribute a = new FramedMTUAttribute(64);
        assertThat(a.getMtu()).isEqualTo(64).describedAs("mtu should be 64");
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_MTU.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);

        final FramedMTUAttribute b = new FramedMTUAttribute(bytes);
        assertThat(b.getMtu()).isEqualTo(64).describedAs("mtu created from octets should be 64");
    }

    @Test
    void testLowestFromOctets() {
        final byte[] octets = new byte[] {(byte) FRAMED_MTU.getTypeCode(), 6, 0, 0, 0, 64};
        final FramedMTUAttribute a = new FramedMTUAttribute(octets);
        assertThat(a.getMtu()).isEqualTo(64).describedAs("mtu should be 64");
    }

}
