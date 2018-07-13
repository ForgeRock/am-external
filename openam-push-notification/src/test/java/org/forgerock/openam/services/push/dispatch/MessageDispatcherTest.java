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
 * Copyright 2016-2017 ForgeRock AS.
 */
package org.forgerock.openam.services.push.dispatch;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;

import java.util.HashSet;

import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

@Test
public class MessageDispatcherTest {

    Cache cache = CacheBuilder.newBuilder().build();
    Debug mockDebug = mock(Debug.class);
    JSONSerialisation jsonSerialisation = mock(JSONSerialisation.class);
    CTSPersistentStore persistentStore = mock(CTSPersistentStore.class);
    ClusterMessageHandler mockMessageHandler = mock(ClusterMessageHandler.class);

    MessageDispatcher messageDispatcher;

    @BeforeTest
    public void setUp() {
        messageDispatcher = new MessageDispatcher(cache, mockDebug, persistentStore, jsonSerialisation, 120);
    }

    @Test
    public void shouldReturnEmptyPromiseForExpectedMessageId() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");

        //when
        MessagePromise result = messageDispatcher.expect("expectMessage", new HashSet<Predicate>());

        //then
        assertThat(result.getPromise().isDone()).isFalse();
    }

    @Test
    public void shouldCompletePromiseForHandledMessageIdWhenExpected() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");
        MessagePromise result = messageDispatcher.expect("completeexpect", new HashSet<Predicate>());

        //when
        messageDispatcher.handle("completeexpect", json(object()), mockMessageHandler);

        //then
        assertThat(result.getPromise().isDone()).isTrue();
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldFallBackToCTSWhenMessageNotExpectedAndFailToFind() throws Exception {
        //given
        given(persistentStore.read("messageId")).willReturn(null);

        //when
        messageDispatcher.handle("messageId", json(object()), mockMessageHandler);

        //then
    }

    @Test
    public void shouldFallBackToCTSWhenMessageNotExpectedAndSucceed() throws Exception {
        //given
        Token token = mock(Token.class);
        ClusterMessageHandler mockHandler = mock(ClusterMessageHandler.class);
        given(token.getBlob()).willReturn(json(object()).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        //when
        messageDispatcher.handle("messageId", json(object()), mockHandler);

        //then
        verify(mockHandler, times(1)).update(any(Token.class), any(JsonValue.class));
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldErrorForHandleMessageIdWhenNotExpected() throws Exception {
        //given

        //when
        messageDispatcher.handle("notExpected", json(object()), null);

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorWhenPrimedForNullMessageId() throws Exception {
        //given

        //when
        messageDispatcher.expect(null, null);

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorWhenPrimedForEmptyMessageId() throws Exception {
        //given

        //when
        messageDispatcher.expect("", new HashSet<Predicate>());

        //then
    }

    @Test
    public void shouldReturnTrueAndForgetMessageIdWhenExpected() throws Exception {
        //given
        messageDispatcher.expect("toForget", new HashSet<Predicate>());

        //when
        boolean result = messageDispatcher.forget("toForget");

        //then
        assertThat(cache.getIfPresent("toForget")).isNull();
        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseForgetWhenNotExpected() throws CoreTokenException {
        //given

        //when
        boolean result = messageDispatcher.forget("notExpectedForget");

        //then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnFalseForgetWhenAlreadyHandled() throws Exception {
        //given
        given(jsonSerialisation.serialise(any())).willReturn("{ }");
        messageDispatcher.expect("alreadyHandled", new HashSet<>());
        messageDispatcher.handle("alreadyHandled", json(object()), mockMessageHandler);

        //when
        boolean result = messageDispatcher.forget("alreadyHandled");

        //then
        assertThat(result).isFalse();
    }
}
