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

package org.forgerock.openam.radius.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.AttributeType.REPLY_MESSAGE;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReplyMessageAttribute}.
 */
public class ReplyMessageAttributeTest {

    public static final String TWO_FIVE_THREE_CHAR_STRING = "-".repeat(253);

    @Test
    void test() throws IOException {
        final ReplyMessageAttribute r = new ReplyMessageAttribute("hello");
        assertThat(r.getType()).isEqualTo(REPLY_MESSAGE).describedAs("should be a reply message");
        assertThat(r.getMessage()).isEqualTo("hello").describedAs("message should be 'hello'");
        final byte[] data = r.getOctets();
        final String hex = Utils.toSpacedHex(ByteBuffer.wrap(data));
        System.out.println("data: " + hex);
        assertThat(hex).isEqualTo("12 07 68 65 6c 6c 6f").describedAs("should have proper wire format");
    }

    @Test
    void testMaxSize() {
        final String maxS = TWO_FIVE_THREE_CHAR_STRING;
        final ReplyMessageAttribute r = new ReplyMessageAttribute(maxS);
        assertThat(r.getType()).isEqualTo(REPLY_MESSAGE).describedAs("should be a reply message");
        assertThat(r.getMessage()).isEqualTo(maxS).describedAs("message value should be unchanged");
    }
}
