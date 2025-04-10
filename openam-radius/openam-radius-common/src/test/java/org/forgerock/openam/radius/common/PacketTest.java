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
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions Copyrighted 2024-2025 Ping Identity Corporation
 */

package org.forgerock.openam.radius.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.radius.common.PacketType.ACCESS_REJECT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Packet}.
 */
public class PacketTest {

    /**
     * Test to ensure conformity with <a href="https://tools.ietf.org/html/rfc2865#section-7.1">IETF RFC 2865 section
     * 7.1</a>
     */
    @Test
    void testSerializingRfc2865Section7dot1Example() {
        // what we should end up with
        final String res = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
                + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d "
                + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 " + "01 10 05 06 00 00 00 03";

        final AccessRequest accessReq = new AccessRequest();
        accessReq.setIdentifier((short) 0);
        accessReq.addAttribute(new UserNameAttribute("nemo"));

        final String authenticatorBytes = "0f 40 3f 94 73 97 80 57 bd 83 d5 cb 98 f4 22 7a";
        final byte[] aBytes = Utils.toByteArray(authenticatorBytes);
        final RequestAuthenticator authenticator = new RequestAuthenticator(aBytes);
        accessReq.setAuthenticator(authenticator);

        accessReq.addAttribute(new UserPasswordAttribute(authenticator, Rfc2865Examples.secret,
                Rfc2865Examples.password));
        try {
            final InetAddress address = InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 1, 16 });
            accessReq.addAttribute(new NASIPAddressAttribute(address));
        } catch (final UnknownHostException e) {
            e.printStackTrace(); // ignore since it won't happen given valid address
        }
        accessReq.addAttribute(new NASPortAttribute(3));
        final byte[] bytes = accessReq.getOctets();
        final ByteBuffer pktBfr = ByteBuffer.wrap(bytes);
        final String spaceHex = Utils.toSpacedHex(pktBfr);
        assertThat(spaceHex).isEqualTo(res).describedAs("output sequence of AccessRequest should have matched");
    }

    /**
     * Test to ensure conformity with <a href="https://tools.ietf.org/html/rfc2865#section-7.3">IETF RFC 2865 section
     * 7.3</a>
     */
    @Test
    void testSerializingRfc2865Section7dot3RejectExample() {
        // what we should end up with
        final String res = "03 03 00 14 a4 2f 4f ca 45 91 6c 4e 09 c8 34 0f 9e 74 6a a0";

        final ByteBuffer bfr = Utils.toBuffer(res);
        final Packet p = PacketFactory.toPacket(bfr);
        assertThat(p).isNotNull();
        assertThat(p.getType()).isEqualTo(ACCESS_REJECT).describedAs("should be reject packet");
        assertThat(p.getIdentifier()).isEqualTo((short) 3).describedAs("identifier should be 3");
        assertThat(p.getAuthenticator()).isNotNull().describedAs("authenticator should be found");
        final byte[] authb = p.getAuthenticator().getOctets();
        final String authHex = Utils.toSpacedHex(ByteBuffer.wrap(authb));
        assertThat(authHex).isEqualTo("a4 2f 4f ca 45 91 6c 4e 09 c8 34 0f 9e 74 6a a0")
                .describedAs("auth bytes should match those from wire format");
    }

    /**
     * Test to ensure conformity with <a href="https://tools.ietf.org/html/rfc2865#section-7.3">IETF RFC 2865 section
     * 7.3</a>
     */
    @Test
    void testSerializingRfc2865Sec7dot3RejectExampleCreateServerPacketWithMsg() {
        // what we should end up with
        final String res = "03 03 00 1b a4 2f 4f ca 45 91 6c 4e 09 c8 34 0f 9e 74 6a a0 12 07 68 65 6c 6c 6f";

        final byte[] bytes = Utils.toByteArray(res);
        final Packet p = PacketFactory.toPacket(bytes);
        assertThat(p.getType()).isEqualTo(ACCESS_REJECT).describedAs("should be reject packet");
        assertThat(p.getIdentifier()).isEqualTo((short) 3).describedAs("identifier should be 3");
        assertThat(p.getAuthenticator()).isNotNull().describedAs("authenticator should be found");
        final byte[] authb = p.getAuthenticator().getOctets();
        final String authHex = Utils.toSpacedHex(ByteBuffer.wrap(authb));
        assertThat(authHex).isEqualTo("a4 2f 4f ca 45 91 6c 4e 09 c8 34 0f 9e 74 6a a0")
                .describedAs("auth bytes should match those from wire format");
        assertThat(p.getAttributeSet()).isNotNull().describedAs("should have attribute set");
        assertThat(p.getAttributeSet().size()).isEqualTo(1).describedAs("should be one attribute");
        final Attribute a = p.getAttributeAt(0);
        assertThat(a.getType()).isEqualTo(AttributeType.REPLY_MESSAGE).describedAs("should be a reply message");
        final ReplyMessageAttribute r = (ReplyMessageAttribute) a;
        assertThat(r.getMessage()).isEqualTo("hello").describedAs("message should be 'hello'");
    }

    /**
     * Test the serialization.
     */
    @Test
    void testSerializingOfReject() {
        // what we should end up with
        final String res = "03 03 00 1b a4 2f 4f ca 45 91 6c 4e 09 c8 34 0f 9e 74 6a a0 12 07 68 65 6c 6c 6f";

        final byte[] bytes = Utils.toByteArray(res);
        final Packet p = PacketFactory.toPacket(bytes);
        final byte[] data = p.getOctets();
        final String hex = Utils.toSpacedHex(ByteBuffer.wrap(data));

        assertThat(hex).isEqualTo(res).describedAs("serialized form should match original");
    }
}
