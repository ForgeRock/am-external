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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_APPLETALK_ZONE;

import org.forgerock.openam.radius.common.Utils;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedAppleTalkZoneAttribute}.
 */
public class FramedAppleTalkZoneAttributeTest {

    @Test
    void testNormalUse() {
        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute("filter");
        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_APPLETALK_ZONE.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) ("filter".length() + 2));
        assertThat(new String(bytes, 2, bytes.length - 2, UTF_8)).isEqualTo("filter");
    }

    @Test
    void testTruncationOfString() {
        // create a 256 byte string
        final String fifty = "---------1---------2---------3---------4---------5"; // 50 chars
        final String f253 = fifty + fifty + fifty + fifty + fifty + "123";

        final String f259 = f253 + "456789";

        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute(f259);
        assertThat(a.getZone()).isEqualTo(f253)
                .describedAs("filter id string should have been truncated to 254 chars");

        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_APPLETALK_ZONE.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 255); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        assertThat(new String(bytes, 2, 253, UTF_8)).isEqualTo(f253);
    }

    @Test
    void test253StringFits() {
        // create a 256 byte string
        final String fifty = "---------1---------2---------3---------4---------5"; // 50 chars
        final String f253 = fifty + fifty + fifty + fifty + fifty + "123";

        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute(f253);
        assertThat(a.getZone()).isEqualTo(f253).describedAs("253 byte filter id string should have been allowed");

        final byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_APPLETALK_ZONE.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 255); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        assertThat(new String(bytes, 2, 253, UTF_8)).isEqualTo(f253);
    }

    @Test
    void testFromRawBytes() {
        final byte type = (byte) FRAMED_APPLETALK_ZONE.getTypeCode();
        final byte[] octets = new byte[] {type, 8, 102, 105, 108, 116, 101, 114};
        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute(octets);
        final byte[] bytes = a.getOctets();
        System.out.println("hAp2=" + Utils.toHexAndPrintableChars(bytes));
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_APPLETALK_ZONE.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 8);
        assertThat(new String(bytes, 2, 6, UTF_8)).isEqualTo("filter");
    }
}
