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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.radius.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute;
import org.forgerock.openam.radius.common.packet.UnknownAttribute;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AttributeFactory}.
 */
class AttributeFactoryTest {

    @Test
    void shouldCreateAttributeGivenMessageAuthenticatorType() {
        // Given
        byte attributeType = 80;
        byte[] data = new byte[]{attributeType, 18, -9, -17, -36, 74, 45, 76, -22, 30, 57, 102, -58, 65, 106, 100,
                85, 87};

        // When
        Attribute attribute = AttributeFactory.createAttribute(data);

        // Then
        assertThat(attribute).isInstanceOf(MessageAuthenticatorAttribute.class);
    }

    @Test
    void shouldCreateAttributeGivenUnknownType() {
        // Given
        byte unknownAttributeType = 0;
        byte[] data = new byte[]{unknownAttributeType, 30, 57, 102, -58};

        // When
        Attribute attribute = AttributeFactory.createAttribute(data);

        // Then
        assertThat(attribute).isInstanceOf(UnknownAttribute.class);
    }

    @Test
    void shouldCreateAttributeGivenNonExistingType() {
        // Given
        byte unknownAttributeType = 90;
        byte[] data = new byte[]{unknownAttributeType, 30, 57, 102, -58};

        // When
        Attribute attribute = AttributeFactory.createAttribute(data);

        // Then
        assertThat(attribute).isInstanceOf(UnknownAttribute.class);
    }

}
