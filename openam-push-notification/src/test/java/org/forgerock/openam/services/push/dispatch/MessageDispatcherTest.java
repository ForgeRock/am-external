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
 * Copyright 2016-2026 ForgeRock AS.
 */
package org.forgerock.openam.services.push.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;

import org.assertj.core.api.ThrowingConsumer;
import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.am.cts.api.tokens.TokenFactory;
import org.forgerock.am.cts.api.tokens.TokenType;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.am.cts.utils.JSONSerialisation;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@Test
public class MessageDispatcherTest {

    private static final String KNOWN_PREDICATE_CLASS =
            "org.forgerock.openam.services.push.dispatch.predicates.SignedJwtVerificationPredicate";
    private static final String PUSH_PREDICATE_ALLOWLIST_PROPERTY = "am.push.predicate.allowlist.enabled";

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
        messageDispatcher = new MessageDispatcher(cache, persistentStore, jsonSerialisation, 120, tokenFactory,
                Collections.emptySet(), "/");
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY);
        cache.invalidateAll();
        Mockito.clearInvocations(jsonSerialisation);
    }

    @Test
    void everyBuiltInPredicateClassNameResolvesToAPredicate() throws Exception {
        for (String name : MessageDispatcher.BUILT_IN_PREDICATE_CLASSES) {
            Class.forName(name).asSubclass(Predicate.class);
        }
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
        given(token.getType()).willReturn(TokenType.PUSH);
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
        assertThat(cache.getIfPresent(messageId)).isNull();
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

    @Test
    public void handleViaCtsShouldThrowNotFoundWhenTokenTypeIsNotPush() throws Exception {
        //given
        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.SESSION);
        given(persistentStore.read("messageId")).willReturn(token);

        //when - then
        assertThatThrownBy(() -> messageDispatcher.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void handleViaCtsShouldRejectUnknownPredicateClassWithBadRequestWhenAllowlistIsEnabled() throws Exception {
        //given
        System.setProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY, "true");

        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.PUSH);
        given(token.getBlob()).willReturn(
                json(object(field("java.lang.String", "test-value"))).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        //when - then
        assertThatThrownBy(() -> messageDispatcher.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Push request rejected");

        verify(jsonSerialisation, times(0)).deserialise(any(), any());
    }

    @Test
    public void handleViaCtsShouldRejectUnknownPredicateClassByDefaultWhenPropertyIsUnset() throws Exception {
        //given - the allowlist now enforces by default when the property is not set
        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.PUSH);
        given(token.getBlob()).willReturn(
                json(object(field("java.lang.String", "test-value"))).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        //when - then
        assertThatThrownBy(() -> messageDispatcher.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Push request rejected");

        verify(jsonSerialisation, times(0)).deserialise(any(), any());
    }

    @Test
    public void handleViaCtsShouldAllowKnownPredicateClassWhenAllowlistIsEnabled() throws Exception {
        //given
        System.setProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY, "true");

        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.PUSH);
        given(token.getBlob()).willReturn(
                json(object(field(KNOWN_PREDICATE_CLASS, "{}"))).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);
        Predicate predicate = mock(Predicate.class);
        given(predicate.perform(any())).willReturn(true);
        given(jsonSerialisation.deserialise(any(), any())).willReturn(predicate);

        //when
        messageDispatcher.handle(messageId, json(object()), mockMessageHandler);

        //then
        verify(mockMessageHandler, times(1)).update(any(Token.class), any(JsonValue.class));
    }

    @Test
    public void handleViaCtsShouldAllowAdditionalConfiguredClassWhenAllowlistIsEnabled() throws Exception {
        //given
        System.setProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY, "true");
        String customClass = "com.example.CustomPredicate";
        MessageDispatcher allowlistEnabled = new MessageDispatcher(
                cache, persistentStore, jsonSerialisation, 120, tokenFactory,
                Collections.singleton(customClass), "/");

        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.PUSH);
        given(token.getBlob()).willReturn(
                json(object(field(customClass, "{}"))).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        assertThatThrownBy(() -> allowlistEnabled.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(NotFoundException.class)
                .isNotInstanceOf(BadRequestException.class);

        verify(jsonSerialisation, times(0)).deserialise(any(), any());
    }

    @Test
    public void handleViaCtsShouldPreservePreExistingBehaviourWhenAllowlistIsDisabled() throws Exception {
        //given
        System.setProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY, "false");
        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.PUSH);
        given(token.getBlob()).willReturn(
                json(object(field("com.example.UnknownClass", "{}"))).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        //when - then
        assertThatThrownBy(() -> messageDispatcher.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void handleViaCtsShouldThrowClassCastExceptionWhenAllowlistedClassIsNotPredicateSubtype() throws Exception {
        // java.lang.String is guaranteed to be on the classpath but is not a Predicate subtype.
        // This test documents that asSubclass(Predicate.class) currently throws ClassCastException
        // as an unhandled RuntimeException when the class name passes the allowlist but the class
        // is not a valid Predicate implementation. A follow-up fix should convert this to a
        // BadRequestException (or NotFoundException) to avoid a 500 response in production.
        System.setProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY, "true");
        String nonPredicateClass = "java.lang.String";
        MessageDispatcher allowlistEnabled = new MessageDispatcher(
                cache, persistentStore, jsonSerialisation, 120, tokenFactory,
                Collections.singleton(nonPredicateClass), "/");

        given(messageId.toString()).willReturn("messageId");
        Token token = mock(Token.class);
        given(token.getType()).willReturn(TokenType.PUSH);
        given(token.getBlob()).willReturn(
                json(object(field(nonPredicateClass, "{}"))).toString().getBytes());
        given(persistentStore.read("messageId")).willReturn(token);

        //when - then
        assertThatThrownBy(() -> allowlistEnabled.handle(messageId, json(object()), mockMessageHandler))
                .isInstanceOf(ClassCastException.class);
    }

    private class FakePredicate implements Predicate {
        @Override
        public boolean perform(JsonValue content) {
            return false;
        }

        @Override
        public String jsonify() {
            return "";
        }
    }

    @Test
    public void handleViaCtsShouldWarnOncePerUnknownPredicateClassWhenAllowlistIsDisabled() throws Exception {
        System.setProperty(PUSH_PREDICATE_ALLOWLIST_PROPERTY, "false");

        MessageDispatcher freshDispatcher = new MessageDispatcher(cache, persistentStore, jsonSerialisation, 120,
                tokenFactory, Collections.emptySet(), "/");

        String unknownPredicateClass = FakePredicate.class.getName();
        ClusterMessageHandler localMessageHandler = mock(ClusterMessageHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(MessageDispatcher.class);
        logger.addAppender(appender);
        try {
            ThrowingConsumer<ILoggingEvent> warningLogMatcher = event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).isEqualTo(
                        "am.push.predicate.allowlist: Predicate class " + unknownPredicateClass + " in realm /"
                                + " is not on the push predicate allowlist.");
            };

            given(messageId.toString()).willReturn("messageId");
            Token token = mock(Token.class);
            given(token.getType()).willReturn(TokenType.PUSH);
            given(token.getBlob()).willReturn(
                    json(object(field(unknownPredicateClass, "{}"))).toString().getBytes());
            given(persistentStore.read("messageId")).willReturn(token);
            Predicate predicate = mock(Predicate.class);
            given(predicate.perform(any())).willReturn(true);
            given(jsonSerialisation.deserialise(any(), any())).willReturn(predicate);

            //when - the allowlist is disabled so an unknown predicate warns but does not block handling
            freshDispatcher.handle(messageId, json(object()), localMessageHandler);

            //then
            assertThat(appender.list).anySatisfy(warningLogMatcher);
            verify(localMessageHandler, times(1)).update(any(Token.class), any(JsonValue.class));
            assertThat(appender.list).noneSatisfy(event ->
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR));

            //when - a repeat delivery for the same predicate class should not warn again
            appender.list.clear();
            freshDispatcher.handle(messageId, json(object()), localMessageHandler);

            //then
            assertThat(appender.list).noneSatisfy(warningLogMatcher);
        } finally {
            logger.detachAppender(appender);
        }
    }
}
