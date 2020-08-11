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
 * Copyright 2016-2019 ForgeRock AS.
 */
package org.forgerock.openam.services.push.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.forgerock.am.cts.api.tokens.TokenFactory;
import org.forgerock.am.cts.api.tokens.TokenType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.am.cts.utils.JSONSerialisation;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Test
public class MessageDispatcherTest {

    private Cache<MessageId, MessagePromise> cache = CacheBuilder.newBuilder().build();
    private MessageId messageId = mock(MessageId.class);
    private JSONSerialisation jsonSerialisation = mock(JSONSerialisation.class);
    private CTSPersistentStore persistentStore = mock(CTSPersistentStore.class);
    private ClusterMessageHandler mockMessageHandler = mock(ClusterMessageHandler.class);
    private TokenFactory tokenFactory = mock(TokenFactory.class);
    private Token token = mock(Token.class);

    private MessageDispatcher messageDispatcher;

    @BeforeTest
    public void setUp() {
        when(tokenFactory.create(anyString(), any(TokenType.class))).thenReturn(token);
        messageDispatcher = new MessageDispatcher(cache, persistentStore, jsonSerialisation, 120, tokenFactory);
    }

    @Test
    public void shouldReturnEmptyPromiseForExpectedMessageId() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        MessagePromise result = messageDispatcher.expectLocally(messageId, new HashSet<>());

        //then
        assertThat(result.getPromise().isDone()).isFalse();
    }

    @Test
    public void shouldCompletePromiseForHandledMessageIdWhenExpected() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");
        given(messageId.toString()).willReturn("AUTHENTICATE:badger");
        MessagePromise result = messageDispatcher.expectLocally(messageId, new HashSet<>());

        //when
        messageDispatcher.handle(messageId, json(object()), mockMessageHandler);

        //then
        assertThat(result.getPromise().isDone()).isTrue();
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldFallBackToCTSWhenMessageNotExpectedAndFailToFind() throws Exception {
        //given
        given(messageId.toString()).willReturn("messageId");
        given(persistentStore.read("messageId")).willReturn(null);

        //when
        messageDispatcher.handle(messageId, json(object()), mockMessageHandler);

        //then
    }

    @Test
    public void shouldFallBackToCTSWhenMessageNotExpectedAndSucceed() throws Exception {
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

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldErrorForHandleMessageIdWhenNotExpected() throws Exception {
        //given
        given(messageId.toString()).willReturn("notExpected");

        //when
        messageDispatcher.handle(messageId, json(object()), null);

        //then
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldErrorWhenPrimedForNullMessageKey() throws Exception {
        //given

        //when
        messageDispatcher.expectLocally(null, new HashSet<>());

        //then
    }

    @Test
    public void shouldReturnTrueAndForgetMessageIdWhenExpected() throws Exception {
        //given
        given(messageId.toString()).willReturn("AUTHENTICATE:badger");
        messageDispatcher.expectLocally(messageId, new HashSet<>());

        //when
        boolean result = messageDispatcher.forget(messageId);

        //then
        assertThat(cache.getIfPresent(messageId.toString())).isNull();
        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseForgetWhenNotExpected() throws CoreTokenException {
        //given
        given(messageId.toString()).willReturn("notExpectedForget");

        //when
        boolean result = messageDispatcher.forget(messageId);

        //then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnFalseForgetWhenAlreadyHandled() throws Exception {
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
    public void expectLocallyShouldUpdateCache() throws Exception {
        //given
        cache.invalidateAll();
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        messageDispatcher.expectLocally(messageId, new HashSet<>());

        //then
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    public void expectInClusterShouldNotUpdateCache() throws Exception {
        //given
        cache.invalidateAll();
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        messageDispatcher.expectInCluster(messageId, new HashSet<>());

        //then
        assertThat(cache.asMap()).isEmpty();
    }
}
