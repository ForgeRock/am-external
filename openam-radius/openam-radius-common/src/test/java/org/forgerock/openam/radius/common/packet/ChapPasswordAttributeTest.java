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
import static org.forgerock.openam.radius.common.AttributeType.CHAP_PASSWORD;

import org.forgerock.openam.radius.common.Utils;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CHAPPasswordAttribute}.
 */
public class ChapPasswordAttributeTest {

    @Test
    void testPaddingShortHash() {
        CHAPPasswordAttribute a = new CHAPPasswordAttribute("challenge", 27);
        byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) CHAP_PASSWORD.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 19);
        assertThat(bytes[2]).isEqualTo((byte) 27);
        assertThat(new String(bytes, 3, 16, UTF_8)).startsWith("challenge");
        assertThat(new String(bytes, 3, 16, UTF_8)).isNotEqualTo("challenge");
    }

    @Test
    void testHashToLong() {
        CHAPPasswordAttribute a = new CHAPPasswordAttribute("1234567890123456---", 27);
        byte[] bytes = a.getOctets();
        System.out.println("hAp3=" + Utils.toHexAndPrintableChars(bytes));
        assertThat(bytes[0]).isEqualTo((byte) CHAP_PASSWORD.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 19);
        assertThat(bytes.length).isEqualTo(19);
        assertThat(bytes[2]).isEqualTo((byte) 27);
        assertThat(new String(bytes, 3, 16, UTF_8)).isEqualTo("1234567890123456");
    }

    @Test
    void testValidHashLength() {
        CHAPPasswordAttribute a = new CHAPPasswordAttribute("1234567890123456", 27);
        byte[] bytes = a.getOctets();
        System.out.println("hAp0=" + Utils.toHexAndPrintableChars(bytes));
        assertThat(bytes[0]).isEqualTo((byte) CHAP_PASSWORD.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 19);
        assertThat(bytes[2]).isEqualTo((byte) 27);
        assertThat(new String(bytes, 3, 16, UTF_8)).isEqualTo("1234567890123456");
    }


    @Test
    void testFromRawBytes() {
        byte[] raw = new byte[] {3, 19, 27, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54};
        System.out.println("hAp1=" + Utils.toHexAndPrintableChars(raw));
        CHAPPasswordAttribute a = new CHAPPasswordAttribute(raw);
        byte[] bytes = a.getOctets();
        System.out.println("hAp2=" + Utils.toHexAndPrintableChars(bytes));
        assertThat(bytes[0]).isEqualTo((byte) CHAP_PASSWORD.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 19);
        assertThat(bytes[2]).isEqualTo((byte) 27);
        assertThat(new String(bytes, 3, 16, UTF_8)).isEqualTo("1234567890123456");
    }
}
