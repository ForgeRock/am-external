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
 * Portions Copyrighted 2011-2025 Ping Identity Corporation.
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */

package org.forgerock.openam.radius.common.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_IPX_NETWORK;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedIPXNetworkAttribute}.
 */
public class FramedIPXNetworkAttributeTest {

    @Test
    void testFromOctets() {
        final byte [] testPacket = new byte[6];
        testPacket[0] = (byte) FRAMED_IPX_NETWORK.getTypeCode();
        testPacket[1] = 6;
        testPacket[2] = 5;
        testPacket[3] = 4;
        testPacket[4] = 3;
        testPacket[5] = 2;

        final FramedIPXNetworkAttribute testIPXpacket = new FramedIPXNetworkAttribute(testPacket);

        assertThat(testIPXpacket.getIPXNetworkAddress()[0]).isEqualTo((byte) 5);
        assertThat(testIPXpacket.getIPXNetworkAddress()[1]).isEqualTo((byte) 4);
        assertThat(testIPXpacket.getIPXNetworkAddress()[2]).isEqualTo((byte) 3);
        assertThat(testIPXpacket.getIPXNetworkAddress()[3]).isEqualTo((byte) 2);

        assertThat(testIPXpacket.getOctets()[0]).isEqualTo((byte) FRAMED_IPX_NETWORK.getTypeCode());
        assertThat(testIPXpacket.getOctets()[1]).isEqualTo((byte) 6);
        assertThat(testIPXpacket.getOctets()[2]).isEqualTo((byte) 5);
        assertThat(testIPXpacket.getOctets()[3]).isEqualTo((byte) 4);
        assertThat(testIPXpacket.getOctets()[4]).isEqualTo((byte) 3);
        assertThat(testIPXpacket.getOctets()[5]).isEqualTo((byte) 2);
    }

    @Test
    void testFromAddress() {
        final FramedIPXNetworkAttribute testIPXAddress = new FramedIPXNetworkAttribute(5, 4, 3, 2);

        assertThat(testIPXAddress.getOctets()[0]).isEqualTo((byte) FRAMED_IPX_NETWORK.getTypeCode());
        assertThat(testIPXAddress.getOctets()[1]).isEqualTo((byte) 6);
        assertThat(testIPXAddress.getOctets()[2]).isEqualTo((byte) 5);
        assertThat(testIPXAddress.getOctets()[3]).isEqualTo((byte) 4);
        assertThat(testIPXAddress.getOctets()[4]).isEqualTo((byte) 3);
        assertThat(testIPXAddress.getOctets()[5]).isEqualTo((byte) 2);
    }

}
