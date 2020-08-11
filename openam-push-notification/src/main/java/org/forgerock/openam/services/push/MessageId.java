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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.services.push;

import java.util.Objects;

/**
 * Represents a push message key that describes the push messages type and unique ID at the same time. The message key
 * in serialized form looks the following:
 * &lt;messageType&gt;:&lt;uniqueId&gt;
 * An example value for a message key looks like the following:
 * AUTHENTICATE:0835dc75-f37e-48f1-bfa1-027845ba81b01520838942913
 */
public class MessageId {

    private final MessageType messageType;
    private final String uniqueId;

    MessageId(MessageType messageType, String uniqueId) {
        this.messageType = messageType;
        this.uniqueId = uniqueId;
    }

    /**
     * Returns the MessageId's message type.
     *
     * @return This message key's message type.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageId that = (MessageId) o;
        return Objects.equals(messageType, that.messageType) && Objects.equals(uniqueId, that.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, uniqueId);
    }

    /**
     * Returns the string representation of this object. The format of this value is the following:
     * &lt;messageType&gt;:&lt;uniqueId&gt;
     * An example return value looks like the following:
     * AUTHENTICATE:0835dc75-f37e-48f1-bfa1-027845ba81b01520838942913
     *
     * @return The string representation of this MessageId object.
     */
    @Override
    public String toString() {
        return messageType.name() + ":" + uniqueId;
    }
}
