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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.junit.jupiter.api.Test;

/**
 * Test methods for the <code>RadiusRequest</code> class.
 *
 * @see org.forgerock.openam.radius.server.RadiusRequest
 */
public class RadiusRequestTest {

    private static final String REQUEST_ID = "requestId";

    /**
     * Test the <code>RadiusRequest#getAttribute</code> method.
     *
     * @see org.forgerock.openam.radius.server.RadiusRequest#getAttribute
     */
    @Test
    void getAttribute() {
        // Given
        UserNameAttribute una = new UserNameAttribute("testUser");
        AccessRequest packet = new AccessRequest();
        packet.addAttribute(una);
        RadiusRequest request = new RadiusRequest(packet, REQUEST_ID);
        // When
        UserNameAttribute attribute = (UserNameAttribute) request.getAttribute(UserNameAttribute.class);
        // then
        assertThat(attribute).isSameAs(una);
    }

    /**
     * Test the <code>RadiusRequest#getRequestId</code> method.
     *
     * @see org.forgerock.openam.radius.server.RadiusRequest#getRequestId
     */
    @Test
    void getRequestId() {
        // Given
        AccessRequest packet = new AccessRequest((short) 1, mock(Authenticator.class));
        RadiusRequest request = new RadiusRequest(packet, REQUEST_ID);
        // Then
        assertThat(request.getRequestId()).isEqualTo(REQUEST_ID);
    }

    /**
     * Test the <code>RadiusRequest#getRequestPacket</code> method.
     *
     * @see org.forgerock.openam.radius.server.RadiusRequest#getRequestPacket
     */
    @Test
    void getRequestPacket() {
        // Given
        AccessRequest packet = new AccessRequest((short) 1, mock(Authenticator.class));
        RadiusRequest request = new RadiusRequest(packet, REQUEST_ID);
        // When
        Packet returned = request.getRequestPacket();
        // Then
        assertThat(returned).isSameAs(packet);
    }

    /**
     * Test the <code>RadiusRequest#getUsername</code> method.
     *
     * @see org.forgerock.openam.radius.server.RadiusRequest#getUsername
     */
    @Test
    void getUsername() {
        // Given
        String userName = "testUser";
        AccessRequest packet = new AccessRequest((short) 1, mock(Authenticator.class));
        UserNameAttribute userNameAttribute = new UserNameAttribute(userName);
        packet.addAttribute(userNameAttribute);
        RadiusRequest request = new RadiusRequest(packet, REQUEST_ID);

        // when
        String returnedUserName = request.getUsername();

        // Then
        assertThat(returnedUserName).isEqualTo(userName);
    }

}
