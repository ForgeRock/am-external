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
import static org.forgerock.openam.radius.common.AttributeType.FILTER_ID;

import org.forgerock.openam.radius.common.Utils;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FilterIdAttribute}.
 */
public class FilterIdAttributeTest {

    public static final String TWO_FIVE_THREE_CHAR_STRING = "-".repeat(253);
    public static final String TWO_FIVE_NINE_CHAR_STRING = "-".repeat(259);

    @Test
    void testNormalUse() {
        FilterIdAttribute a = new FilterIdAttribute("filter");
        byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FILTER_ID.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) ("filter".length() + 2));
        assertThat(new String(bytes, 2, bytes.length - 2, UTF_8)).isEqualTo("filter");
    }

    @Test
    void testTruncationOfString() {
        String f253 = TWO_FIVE_THREE_CHAR_STRING;

        FilterIdAttribute a = new FilterIdAttribute(TWO_FIVE_NINE_CHAR_STRING);
        assertThat(a.getFilterId()).isEqualTo(f253)
                .describedAs("filter id string should have been truncated to 254 chars");

        byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FILTER_ID.getTypeCode());
        assertThat(bytes[1]).isEqualTo(((byte) 255)); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        assertThat(new String(bytes, 2, 253, UTF_8)).isEqualTo(f253);
    }

    @Test
    void test253StringFits() {
        String f253 = TWO_FIVE_THREE_CHAR_STRING;

        FilterIdAttribute a = new FilterIdAttribute(f253);
        assertThat(a.getFilterId()).isEqualTo(f253).describedAs("253 byte filter id string should have been allowed");

        byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FILTER_ID.getTypeCode());
        assertThat(bytes[1]).isEqualTo(((byte) 255)); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        assertThat(new String(bytes, 2, 253, UTF_8)).isEqualTo(f253);
    }

    @Test
    void testFromRawBytes() {
        byte[] raw = new byte[] {11, 8, 102, 105, 108, 116, 101, 114};
        FilterIdAttribute a = new FilterIdAttribute(raw);
        byte[] bytes = a.getOctets();
        System.out.println("hAp2=" + Utils.toHexAndPrintableChars(bytes));
        assertThat(bytes[0]).isEqualTo((byte) FILTER_ID.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 8);
        assertThat(new String(bytes, 2, 6, UTF_8)).isEqualTo("filter");
    }
}
