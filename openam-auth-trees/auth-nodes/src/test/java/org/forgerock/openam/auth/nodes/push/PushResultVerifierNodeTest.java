/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map.Entry;

import org.assertj.core.api.Assertions;
import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;

@Test
@RunWith(CuppaRunner.class)
public class PushResultVerifierNodeTest {

    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private MessageIdFactory messageIdFactory;
    @Mock
    private MessageId messageId;
    @Mock
    private ClusterMessageHandler messageHandler;
    private TreeContext treeContext;
    private PushResultVerifierNode node;

    {
        describe("Push Result Verifier Node", () -> {
            beforeEach(() -> {
                MockitoAnnotations.initMocks(this);
                node = new PushResultVerifierNode(pushNotificationService, messageIdFactory);
            });
            when("the push message ID is missing from sharedstate", () -> {
                beforeEach(() -> {
                    treeContext = new TreeContext(json(object()), new Builder().build(), Collections.emptyList());
                });
                it("should throw an exception", () -> {
                    try {
                        node.process(treeContext);
                        Assertions.fail("Expected exception missing");
                    } catch (NodeProcessException npe) {
                        assertThat(npe).hasMessageContaining("message ID");
                    }
                });
            });
            when("the push message ID is in the sharedstate", () -> {
                beforeEach(() -> {
                    given(messageIdFactory.create("badger", "weasel")).willReturn(messageId);
                    given(messageId.toString()).willReturn("otter");
                    given(messageId.getMessageType()).willReturn(DefaultMessageTypes.AUTHENTICATE);
                    given(pushNotificationService.getMessageHandlers("weasel")).willReturn(
                            ImmutableMap.of(DefaultMessageTypes.AUTHENTICATE, messageHandler));
                    treeContext = getContext();
                });
                when("the push message's message type is unknown", () -> {
                    beforeEach(() -> {
                        given(messageId.getMessageType()).willReturn(DefaultMessageTypes.REGISTER);
                    });
                    it("should return false outcome", () -> {
                        Action result = node.process(treeContext);

                        assertThat(result.outcome).isEqualTo("FALSE");
                    });
                });
                when("the push message's message type is known", () -> {
                    beforeEach(() -> {
                        given(messageId.getMessageType()).willReturn(DefaultMessageTypes.AUTHENTICATE);
                    });
                    it("should check the message state with the correct handler", () -> {
                        node.process(treeContext);

                        verify(messageHandler).check(messageId);
                    });
                    when("the message state can't be retrieved", () -> {
                        beforeEach(() -> {
                            given(messageHandler.check(messageId)).willReturn(null);
                        });
                        it("should return EXPIRED outcome", () -> {
                            Action result = node.process(treeContext);

                            assertThat(result.outcome).isEqualTo("EXPIRED");
                        });
                    });
                    ImmutableMap<MessageState, String> testCases = ImmutableMap.<MessageState, String>builder()
                            .put(MessageState.DENIED, "FALSE")
                            .put(MessageState.SUCCESS, "TRUE")
                            .put(MessageState.UNKNOWN, "WAITING")
                            .build();
                    for (Entry<MessageState, String> testCase : testCases.entrySet()) {
                        when("the message state is " + testCase.getKey(), () -> {
                            beforeEach(() -> {
                                given(messageHandler.check(messageId)).willReturn(testCase.getKey());
                            });
                            it("should return " + testCase.getValue() + " outcome", () -> {
                                Action result = node.process(treeContext);

                                assertThat(result.outcome).isEqualTo(testCase.getValue());
                            });
                        });
                    }
                });
            });
        });
    }

    private TreeContext getContext() {
        return new TreeContext(json(object(field(MESSAGE_ID_KEY, "badger"), field(REALM, "weasel"))),
                new Builder().build(), Collections.emptyList());
    }
}