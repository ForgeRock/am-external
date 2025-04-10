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
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_PROTOCOL;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedProtocolAttribute}.
 */
public class FramedProtocolAttributeTest {

    @Test
    void testLowest() {
        final FramedProtocolAttribute a = new FramedProtocolAttribute(0);
        assertThat(a.getFraming()).isEqualTo(0).describedAs("frame should be 0");
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_PROTOCOL.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);
        assertThat(bytes[2]).isEqualTo((byte) 0);
        assertThat(bytes[3]).isEqualTo((byte) 0);
        assertThat(bytes[4]).isEqualTo((byte) 0);
        assertThat(bytes[5]).isEqualTo((byte) 0);
    }

    @Test
    void testHighest() {
        final FramedProtocolAttribute a = new FramedProtocolAttribute(65535);
        assertThat(a.getFraming()).isEqualTo(65535).describedAs("should be 65535");
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_PROTOCOL.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);
        assertThat(bytes[2]).isEqualTo((byte) 0);
        assertThat(bytes[3]).isEqualTo((byte) 0);
        assertThat(bytes[4]).isEqualTo((byte) 255);
        assertThat(bytes[5]).isEqualTo((byte) 255);
    }

    @Test
    void testMaxFromRawBytes() {
        final byte[] raw = new byte[] {
            (byte) FRAMED_PROTOCOL.getTypeCode(), 6, 0, 0, (byte) 255, (byte) 255
        };
        final FramedProtocolAttribute a = new FramedProtocolAttribute(raw);
        assertThat(a.getFraming()).isEqualTo(65535);
        assertThat(a.getType()).isEqualTo(FRAMED_PROTOCOL);
    }

    @Test
    void testMinFromRawBytes() {
        final byte[] raw = new byte[] {(byte) FRAMED_PROTOCOL.getTypeCode(), 6, 0, 0, 0, 0};
        final FramedProtocolAttribute a = new FramedProtocolAttribute(raw);
        assertThat(a.getFraming()).isEqualTo(0);
        assertThat(a.getType()).isEqualTo(FRAMED_PROTOCOL);
    }

    @Test
    void testPPP() {
        final byte type = (byte) FRAMED_PROTOCOL.getTypeCode();
        final byte[] raw = new byte[] {type, 6, 0, 0, 0, FramedProtocolAttribute.PPP};
        final FramedProtocolAttribute a = new FramedProtocolAttribute(raw);
        assertThat(a.getFraming()).isEqualTo(FramedProtocolAttribute.PPP);
        assertThat(a.getType()).isEqualTo(FRAMED_PROTOCOL);
    }
}
