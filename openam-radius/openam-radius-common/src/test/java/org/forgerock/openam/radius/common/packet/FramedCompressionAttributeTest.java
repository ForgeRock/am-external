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
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_COMPRESSION;
import static org.forgerock.openam.radius.common.packet.FramedCompressionAttribute.IPX_HEADER;
import static org.forgerock.openam.radius.common.packet.FramedCompressionAttribute.NONE;
import static org.forgerock.openam.radius.common.packet.FramedCompressionAttribute.STAC_LZS;
import static org.forgerock.openam.radius.common.packet.FramedCompressionAttribute.VJ_TCP_IP_HEADER;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedCompressionAttribute}.
 */
public class FramedCompressionAttributeTest {

    @Test
    void testNoCompression() {
        final FramedCompressionAttribute a = new FramedCompressionAttribute(0);
        assertThat(a.getCompression()).isEqualTo(NONE);
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_COMPRESSION.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);
        assertThat(bytes[2]).isEqualTo((byte) 0);
        assertThat(bytes[3]).isEqualTo((byte) 0);
        assertThat(bytes[4]).isEqualTo((byte) 0);
        assertThat(bytes[5]).isEqualTo((byte) 0);
    }

    @Test
    void testVjTcpIpCompression() {
        final FramedCompressionAttribute a = new FramedCompressionAttribute(1);
        assertThat(a.getCompression()).isEqualTo(VJ_TCP_IP_HEADER);
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_COMPRESSION.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);
        assertThat(bytes[2]).isEqualTo((byte) 0);
        assertThat(bytes[3]).isEqualTo((byte) 0);
        assertThat(bytes[4]).isEqualTo((byte) 0);
        assertThat(bytes[5]).isEqualTo((byte) 1);
    }

    @Test
    void testIpxFromRawBytes() {
        final byte[] raw = new byte[] {13, 6, 0, 0, 0, 2};
        final FramedCompressionAttribute a = new FramedCompressionAttribute(raw);
        assertThat(a.getCompression()).isEqualTo(IPX_HEADER);
        assertThat(a.getType()).isEqualTo(FRAMED_COMPRESSION);
    }

    @Test
    void testSlzsFromRawBytes() {
        final byte[] raw = new byte[] {13, 6, 0, 0, 0, 3};
        final FramedCompressionAttribute a = new FramedCompressionAttribute(raw);
        assertThat(a.getCompression()).isEqualTo(STAC_LZS);
        assertThat(a.getType()).isEqualTo(FRAMED_COMPRESSION);
    }
}
