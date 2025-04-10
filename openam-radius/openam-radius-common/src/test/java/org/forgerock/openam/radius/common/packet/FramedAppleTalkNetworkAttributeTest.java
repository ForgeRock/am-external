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
import static org.forgerock.openam.radius.common.AttributeType.FRAMED_APPLETALK_NETWORK;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FramedAppleTalkNetworkAttribute}.
 */
public class FramedAppleTalkNetworkAttributeTest {

    @Test
    void testUnnumbered() {
        FramedAppleTalkNetworkAttribute a = new FramedAppleTalkNetworkAttribute(0);
        assertThat(a.isNasAssigned()).isTrue().describedAs("0 should result in NAS assigned network");
        byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_APPLETALK_NETWORK.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);
        assertThat(a.getNetworkNumber()).isEqualTo(0);
    }

    @Test
    void testMaxNumbered() {
        FramedAppleTalkNetworkAttribute a = new FramedAppleTalkNetworkAttribute(65535);
        assertThat(a.getNetworkNumber()).isEqualTo(65535);
        assertThat(a.isNasAssigned()).isFalse().describedAs(">0 should result in an unassigned port");
        byte[] bytes = a.getOctets();
        assertThat(bytes[0]).isEqualTo((byte) FRAMED_APPLETALK_NETWORK.getTypeCode());
        assertThat(bytes[1]).isEqualTo((byte) 6);
        assertThat(bytes[2]).isEqualTo((byte) 0);
        assertThat(bytes[3]).isEqualTo((byte) 0);
        assertThat(bytes[4]).isEqualTo((byte) 255);
        assertThat(bytes[5]).isEqualTo((byte) 255);
    }

    @Test
    void testMaxFromRawBytes() {
        byte[] raw = new byte[] {38, 6, 0, 0, (byte) 255, (byte) 255};
        FramedAppleTalkNetworkAttribute a = new FramedAppleTalkNetworkAttribute(raw);
        assertThat(a.getType()).isEqualTo(FRAMED_APPLETALK_NETWORK);
        assertThat(a.getNetworkNumber()).isEqualTo(65535);
        assertThat(a.isNasAssigned()).isFalse();
    }

    @Test
    void testMinFromRawBytes() {
        byte[] raw = new byte[] {38, 6, 0, 0, 0, 1};
        FramedAppleTalkNetworkAttribute a = new FramedAppleTalkNetworkAttribute(raw);
        assertThat(a.getType()).isEqualTo(FRAMED_APPLETALK_NETWORK);
        assertThat(a.getNetworkNumber()).isEqualTo(1);
        assertThat(a.isNasAssigned()).isFalse();
    }

    @Test
    void testUnnumberedFromRawBytes() {
        byte[] raw = new byte[] {38, 6, 0, 0, 0, 0};
        FramedAppleTalkNetworkAttribute a = new FramedAppleTalkNetworkAttribute(raw);
        assertThat(a.getType()).isEqualTo(FRAMED_APPLETALK_NETWORK);
        assertThat(a.getNetworkNumber()).isEqualTo(0);
        assertThat(a.isNasAssigned()).isTrue();
    }
}
