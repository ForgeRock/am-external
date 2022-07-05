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
 * Copyright 2018-2021 ForgeRock AS.
 */
package org.forgerock.openam.services.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

@Listeners(MockitoTestNGListener.class)
public class MessageIdFactoryTest {

    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private ClusterMessageHandler messageHandler;
    private MessageIdFactory messageIdFactory;

    @BeforeMethod
    public void setup() {
        messageIdFactory = new MessageIdFactory(pushNotificationService);
    }

    @Test
    public void shouldRecreateMessageKeyFromString() throws Exception {
        given(pushNotificationService.getMessageHandlers(eq("/"))).willReturn(
                ImmutableMap.<MessageType, ClusterMessageHandler>builder()
                        .put(DefaultMessageTypes.AUTHENTICATE, messageHandler)
                        .put(DefaultMessageTypes.REGISTER, messageHandler)
                        .build());

        MessageId messageId = messageIdFactory.create("AUTHENTICATE:badger", "/");

        assertThat(messageId).isNotNull();
        assertThat(messageId.getMessageType()).isEqualTo(DefaultMessageTypes.AUTHENTICATE);
        assertThat(messageId.toString()).isNotNull().isEqualTo("AUTHENTICATE:badger");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfMessageTypeIsUnknown() throws Exception {
        messageIdFactory.create("AUTHENTICATE:badger", "/");
    }

    @Test
    public void shouldGenerateMessageId() {
        MessageId messageId = messageIdFactory.create(DefaultMessageTypes.AUTHENTICATE);

        assertThat(messageId.getMessageType()).isEqualTo(DefaultMessageTypes.AUTHENTICATE);
        assertThat(messageId.toString()).startsWith("AUTHENTICATE:");
        assertThat(messageId.toString().length()).isGreaterThan("AUTHENTICATE:".length());
    }
}
