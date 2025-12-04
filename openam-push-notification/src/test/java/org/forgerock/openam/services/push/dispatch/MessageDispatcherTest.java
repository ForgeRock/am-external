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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.services.push.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.am.cts.api.tokens.TokenFactory;
import org.forgerock.am.cts.api.tokens.TokenType;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.am.cts.utils.JSONSerialisation;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class MessageDispatcherTest {

    private final Cache<MessageId, MessagePromise> cache = CacheBuilder.newBuilder().build();
    private final MessageId messageId = mock(MessageId.class);
    private final JSONSerialisation jsonSerialisation = mock(JSONSerialisation.class);
    private final CTSPersistentStore persistentStore = mock(CTSPersistentStore.class);
    private final ClusterMessageHandler mockMessageHandler = mock(ClusterMessageHandler.class);
    private final TokenFactory tokenFactory = mock(TokenFactory.class);
    private final Token token = mock(Token.class);

    private MessageDispatcher messageDispatcher;

    @BeforeEach
    void setUp() {
        when(tokenFactory.create(anyString(), any(TokenType.class))).thenReturn(token);
        messageDispatcher = new MessageDispatcher(cache, persistentStore, jsonSerialisation, 120, tokenFactory);
    }

    @Test
    void shouldReturnEmptyPromiseForExpectedMessageId() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        MessagePromise result = messageDispatcher.expectLocally(messageId, new HashSet<>());

        //then
        assertThat(result.getPromise().isDone()).isFalse();
    }

    @Test
    void shouldCompletePromiseForHandledMessageIdWhenExpected() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");
        given(messageId.toString()).willReturn("AUTHENTICATE:badger");
        MessagePromise result = messageDispatcher.expectLocally(messageId, new HashSet<>());

        //when
        messageDispatcher.handle(messageId, json(object()), mockMessageHandler);

        //then
        assertThat(result.getPromise().isDone()).isTrue();
    }

    @Test
    void shouldFallBackToCTSWhenMessageNotExpectedAndFailToFind() throws Exception {
        //given
        given(messageId.toString()).willReturn("messageId");
        given(persistentStore.read("messageId")).willReturn(null);

        //when - then
        assertThatThrownBy(() -> messageDispatcher.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Unable to find token with id messageId in CTS.");
    }

    @Test
    void shouldFallBackToCTSWhenMessageNotExpectedAndSucceed() throws Exception {
        //given
        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        ClusterMessageHandler mockHandler = mock(ClusterMessageHandler.class);
        given(token.getBlob()).willReturn(json(object()).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        //when
        messageDispatcher.handle(messageId, json(object()), mockHandler);

        //then
        verify(mockHandler, times(1)).update(any(Token.class), any(JsonValue.class));
    }

    @Test
    void shouldErrorForHandleMessageIdWhenNotExpected() {
        //given
        given(messageId.toString()).willReturn("notExpected");

        //when - then
        assertThatThrownBy(() -> messageDispatcher.handle(messageId, json(object()), null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Unable to find token with id notExpected in CTS.");
    }

    @Test
    void shouldErrorWhenPrimedForNullMessageKey() {
        //given

        //when - then
        assertThatThrownBy(() -> messageDispatcher.expectLocally(null, new HashSet<>()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldReturnTrueAndForgetMessageIdWhenExpected() throws Exception {
        //given
        //needed due to MessageDispatcher.storeInCTS complaining about null value in result
        given(jsonSerialisation.serialise(any())).willReturn("{ }");
        given(messageId.toString()).willReturn("AUTHENTICATE:badger");
        messageDispatcher.expectLocally(messageId, new HashSet<>());

        //when
        boolean result = messageDispatcher.forget(messageId);

        //then
        assertThat(cache.getIfPresent(messageId)).isNull();
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForgetWhenNotExpected() throws CoreTokenException {
        //given
        given(messageId.toString()).willReturn("notExpectedForget");

        //when
        boolean result = messageDispatcher.forget(messageId);

        //then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForgetWhenAlreadyHandled() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");
        given(messageId.toString()).willReturn("AUTHENTICATE:badger");
        messageDispatcher.expectLocally(messageId, new HashSet<>());
        messageDispatcher.handle(messageId, json(object()), mockMessageHandler);

        //when
        boolean result = messageDispatcher.forget(messageId);

        //then
        assertThat(result).isFalse();
    }

    @Test
    void expectLocallyShouldUpdateCache() throws Exception {
        //given
        cache.invalidateAll();
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        messageDispatcher.expectLocally(messageId, new HashSet<>());

        //then
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void expectInClusterShouldNotUpdateCache() throws Exception {
        //given
        cache.invalidateAll();
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        messageDispatcher.expectInCluster(messageId, new HashSet<>());

        //then
        assertThat(cache.asMap()).isEmpty();
    }
}
