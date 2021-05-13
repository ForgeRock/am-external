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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.push;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Map.Entry;
import java.util.Optional;

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

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
                    treeContext = new TreeContext(json(object()), new Builder().build(), emptyList(), Optional.empty());
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
                new Builder().build(), emptyList(), Optional.empty());
    }
}
