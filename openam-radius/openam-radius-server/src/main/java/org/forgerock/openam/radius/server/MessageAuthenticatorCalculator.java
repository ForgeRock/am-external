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

package org.forgerock.openam.radius.server;

import static org.forgerock.openam.radius.common.AttributeType.MESSAGE_AUTHENTICATOR;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.ATTRIBUTE_LENGTH;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.LENGTH_POSITION;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.TYPE_POSITION;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.VALUE_LENGTH;
import static org.forgerock.openam.radius.common.packet.MessageAuthenticatorAttribute.VALUE_START_POSITION;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.util.Reject;

/**
 * Class to calculate the Message-Authenticator attribute value for a RADIUS payload.
 */
public class MessageAuthenticatorCalculator {

    /**
     * Computes the Message-Authenticator attribute value for a Radius payload that meets the minimum requirements.
     *
     * @param payload the {@link ByteBuffer} for which to calculate the Message-Authenticator attribute value
     * @param secret used for the HMAC-MD5 calculation
     * @return the {@link Optional<ByteBuffer>} computed Message-Authenticator attribute value or
     * {@link Optional#empty()} if the Message-Authenticator attribute does not meet the minimum requirements
     */
    public Optional<ByteBuffer> computeFromPayloadIfPresent(ByteBuffer payload, String secret) {
        Reject.ifNull(payload, "payload must not be null");
        Reject.ifBlank(secret, "secret must not be blank");
        if (!isMessageAuthenticatorPresent(payload)) {
            return Optional.empty();
        }

        ByteBuffer payloadClone = trimToLength(payload);
        payloadClone.put(VALUE_START_POSITION, new byte[VALUE_LENGTH], 0, VALUE_LENGTH);
        return Optional.of(sign(payloadClone, secret));
    }

    private ByteBuffer trimToLength(ByteBuffer payload) {
        byte[] payloadArray = payload.array();
        // The payload length is stored in the 3rd and 4th bytes of the array (index 2 and 3), it is then read as a
        // 2-byte short value and converted to an unsigned integer.
        int packetLen = ByteBuffer.wrap(payloadArray, 2, 2).getShort() & 0xFFFF;
        ByteBuffer trimmedPayload = ByteBuffer.allocate(packetLen);
        trimmedPayload.put(payloadArray, 0, packetLen);
        return trimmedPayload;
    }

    private ByteBuffer sign(ByteBuffer payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacMD5");
            mac.init(keySpec);
            return ByteBuffer.wrap(mac.doFinal(payload.array()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to sign RADIUS payload", e);
        }
    }

    private boolean isMessageAuthenticatorPresent(ByteBuffer payload) {
        return payload.limit() >= VALUE_START_POSITION + VALUE_LENGTH
                && payload.get(TYPE_POSITION) == MESSAGE_AUTHENTICATOR.getTypeCode()
                && payload.get(LENGTH_POSITION) == ATTRIBUTE_LENGTH;
    }

}
