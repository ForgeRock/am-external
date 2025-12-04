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

package org.forgerock.openam.radius.common.packet;

import org.forgerock.openam.radius.common.Attribute;

/**
 * Represents the RADIUS Message-Authenticator attribute.
 */
public class MessageAuthenticatorAttribute extends Attribute {

    /**
     * Message-Authenticator attribute type position.
     */
    public static final int TYPE_POSITION = 20;

    /**
     * Message-Authenticator attribute value start position.
     */
    public static final int VALUE_START_POSITION = TYPE_POSITION + 2;

    /**
     * Message-Authenticator attribute length position.
     */
    public static final int LENGTH_POSITION = TYPE_POSITION + 1;

    /**
     * Message-Authenticator attribute length.
     */
    public static final int ATTRIBUTE_LENGTH = 18;

    /**
     * Message-Authenticator attribute value length.
     */
    public static final int VALUE_LENGTH = ATTRIBUTE_LENGTH - 2;

    private final byte[] value;

    /**
     * Constructor using the on-the-wire octets for the attribute.
     *
     * @param octets the on-the-wire octets
     */
    public MessageAuthenticatorAttribute(byte[] octets) {
        super(octets);
        this.value = new byte[octets.length - 2];
        System.arraycopy(octets, 2, value, 0, octets.length - 2);
    }

    /**
     * Get the Message-Authenticator value.
     *
     * @return the Message-Authenticator value
     */
    public byte[] getValue() {
        return value;
    }
}

