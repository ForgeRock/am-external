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
 * Portions Copyrighted 2011-2025 Ping Identity Corporation
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */

package org.forgerock.openam.radius.common.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_IP_NETMASK;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedIPNetmaskAttribute}.
 */
public class FramedIPNetmaskAttributeTest {

    @Test
    void testFromOctets() {
        byte[] o = new byte[6];
        o[0] = (byte) FRAMED_IP_NETMASK.getTypeCode();
        o[1] = 6;
        o[2] = 5;
        o[3] = 4;
        o[4] = 3;
        o[5] = 2;

        FramedIPNetmaskAttribute a = new FramedIPNetmaskAttribute(o);

        assertThat(a.getMask()[0]).isEqualTo((byte) 5);
        assertThat(a.getMask()[1]).isEqualTo((byte) 4);
        assertThat(a.getMask()[2]).isEqualTo((byte) 3);
        assertThat(a.getMask()[3]).isEqualTo((byte) 2);
    }

    @Test
    void testFromAddress() {
        FramedIPNetmaskAttribute a = new FramedIPNetmaskAttribute(5, 4, 3, 2);

        assertThat(a.getMask()[0]).isEqualTo((byte) 5);
        assertThat(a.getMask()[1]).isEqualTo((byte) 4);
        assertThat(a.getMask()[2]).isEqualTo((byte) 3);
        assertThat(a.getMask()[3]).isEqualTo((byte) 2);
        assertThat(a.getOctets()[0]).isEqualTo((byte) FRAMED_IP_NETMASK.getTypeCode());
        assertThat(a.getOctets()[1]).isEqualTo((byte) 6);
        assertThat(a.getOctets()[2]).isEqualTo((byte) 5);
        assertThat(a.getOctets()[3]).isEqualTo((byte) 4);
        assertThat(a.getOctets()[4]).isEqualTo((byte) 3);
        assertThat(a.getOctets()[5]).isEqualTo((byte) 2);
    }
}
