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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.services.push;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.utils.Time;

/**
 * This factory object is responsible for constructing {@link MessageId} objects either for new push messages, or when
 * the ID needs to be reconstructed from its string representation.
 *
 * @see MessageId
 */
public class MessageIdFactory {

    private final PushNotificationService pushNotificationService;

    /**
     * Guice injected constructor.
     *
     * @param pushNotificationService The push notification service to help determine the available message types in the
     *                                requested realm.
     */
    @Inject
    public MessageIdFactory(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Creates a new {@link MessageId} instance based on its string representation.
     *
     * @param messageKey The string representation of the {@link MessageId}.
     * @param realm The realm to which message key corresponds to.
     * @return The reconstructed {@link MessageId} instance.
     * @throws PushNotificationException If there was an error while trying to determine the message type for the
     *                                   message key.
     */
    public MessageId create(String messageKey, String realm) throws PushNotificationException {
        Map<MessageType, ClusterMessageHandler> messageHandlers = pushNotificationService.getMessageHandlers(realm);
        int idx = messageKey.lastIndexOf(':');
        if (idx == -1) {
            throw new IllegalArgumentException("Unable to parse message ID " + messageKey);
        }
        String typeId = messageKey.substring(0, idx);
        String messageId = messageKey.substring(idx + 1);

        MessageType messageType = messageHandlers.keySet().stream()
                .filter(type -> typeId.equals(type.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown message type provided: " + typeId));
        return new MessageId(messageType, messageId);
    }

    /**
     * Generates a new {@link MessageId} based on the provided {@link MessageType}.
     *
     * @param messageType The push message's {@link MessageType}.
     * @return A new unique {@link MessageId} instance.
     */
    public MessageId create(MessageType messageType) {
        return new MessageId(messageType, UUID.randomUUID().toString() + Time.currentTimeMillis());
    }
}
