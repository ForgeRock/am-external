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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PacketFactory}.
 */
public class PacketFactoryTest {

    /**
     * Test to ensure conformity with <a href="https://tools.ietf.org/html/rfc2865#section-7.1">IETF RFC 2865 section
     * 7.1</a>
     */
    @Test
    void testRfc2865Sec7dot1Example() throws UnknownHostException {
        final String hex = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb"
                + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d" + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8"
                + "01 10 05 06 00 00 00 03";

        final ByteBuffer bfr = Utils.toBuffer(hex);
        dumpBfr(bfr);
        final Packet pkt = PacketFactory.toPacket(bfr);
        assertThat(pkt).isNotNull();
        assertThat(pkt.getAuthenticator()).isNotNull().describedAs("authenticator should be defined");
        assertThat(pkt.getType()).isEqualTo(PacketType.ACCESS_REQUEST).describedAs("Incorrect type code");
        assertThat(pkt.getIdentifier()).isEqualTo((short) 0).describedAs("packet identifier should have been 0");
        assertThat(pkt.getAttributeSet().size()).isEqualTo(4).describedAs("packet attributes contained");

        assertThat(pkt.getAttributeAt(0)).isInstanceOf(UserNameAttribute.class).describedAs("0 attribute");
        assertThat(((UserNameAttribute) pkt.getAttributeAt(0)).getName()).isEqualTo("nemo").describedAs("user name");

        assertThat(pkt.getAttributeAt(1)).isInstanceOf(UserPasswordAttribute.class).describedAs("1 attribute");

        assertThat(pkt.getAttributeAt(2)).isInstanceOf(NASIPAddressAttribute.class).describedAs("2 attribute");
        assertThat(((NASIPAddressAttribute) pkt.getAttributeAt(2)).getIpAddress())
                .isEqualTo(InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 1, 16 }))
                .describedAs("NAS IP address");

        assertThat(pkt.getAttributeAt(3)).isInstanceOf(NASPortAttribute.class).describedAs("3 attribute");
        assertThat(((NASPortAttribute) pkt.getAttributeAt(3)).getPort()).isEqualTo(3).describedAs("NAS port");

    }

    /**
     * Test username attribute reading
     */
    @Test
    void testUserNameAtt() {
        final String hex = "01 06 6e 65 6d 6f";
        final ByteBuffer bfr = Utils.toBuffer(hex);
        final Attribute att = PacketFactory.nextAttribute(bfr);
        assertThat(att).isNotNull();
        assertThat(att).isInstanceOf(UserNameAttribute.class).describedAs("wrong attribute class instantiated");
        final UserNameAttribute una = (UserNameAttribute) att;
        assertThat(una.getName()).isEqualTo("nemo");
    }

    /**
     * dumps to std out in sets of 16 hex bytes separated by spaces and prefixed with '0' for bytes having value less
     * than 0x10. The buffer is returned as was meaning ready to read from the same point as when it was passed to this
     * method.
     */
    private void dumpBfr(ByteBuffer bfr) {
        System.out.println("Packet contents: ");

        bfr.mark();
        int i = 0;

        while (bfr.hasRemaining()) {
            if (i == 16) {
                System.out.println();
                i = 0;
            }
            i++;
            final byte b = bfr.get();
            final int j = (b) & 0xFF; // trim off sign-extending bits
            String bt = Integer.toHexString(j);
            if (bt.length() == 1) { // prefix single chars with '0'
                bt = "0" + bt;
            }

            System.out.print(bt + " ");

        }
        bfr.reset();
        System.out.println();
    }
}
