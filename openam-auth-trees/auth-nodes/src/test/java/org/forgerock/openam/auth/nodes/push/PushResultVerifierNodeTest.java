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

package org.forgerock.openam.auth.nodes.push;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_MESSAGE_EXPIRATION;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.TIME_TO_LIVE_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.utils.Time;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;

public class PushResultVerifierNodeTest {

    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private MessageIdFactory messageIdFactory;
    @Mock
    private MessageId messageId;
    @Mock
    private ClusterMessageHandler messageHandler;
    @Mock
    private PushDeviceProfileHelper pushDeviceProfileHelper;
    @Mock
    private PushDeviceSettings pushDeviceSettings;

    private TreeContext treeContext;
    private PushResultVerifierNode node;

    PushResultVerifierNodeTest() {
        MockitoAnnotations.initMocks(this);
        node = new PushResultVerifierNode(pushNotificationService, messageIdFactory, pushDeviceProfileHelper);
    }

    @Nested
    @DisplayName(value = "Push Result Verifier Node")
    class PushResultVerifierNodeNested {

        @BeforeEach
        public void beforeEach() {
        }

        @Nested
        @DisplayName(value = "the push message ID is missing from sharedstate")
        class ThePushMessageIdIsMissingFromSharedstate {

            @BeforeEach
            public void beforeEach() {
                treeContext = new TreeContext(json(object()), new Builder().build(), emptyList(), Optional.empty());
            }

            @Test
            @DisplayName(value = "should throw an exception")
            public void testShouldThrowAnException() throws Exception {
                try {
                    node.process(treeContext);
                    Assertions.fail("Expected exception missing");
                } catch (NodeProcessException npe) {
                    assertThat(npe).hasMessageContaining("message ID");
                }
            }
        }

        @Nested
        @DisplayName(value = "the push message ID is in the sharedstate")
        class ThePushMessageIdIsInTheSharedstate {

            @BeforeEach
            public void beforeEach() throws Exception {
                given(messageIdFactory.create("badger", "weasel")).willReturn(messageId);
                given(messageId.toString()).willReturn("otter");
                given(messageId.getMessageType()).willReturn(DefaultMessageTypes.AUTHENTICATE);
                given(pushNotificationService.getMessageHandlers("weasel")).willReturn(
                        ImmutableMap.of(DefaultMessageTypes.AUTHENTICATE, messageHandler)
                );
                given(pushDeviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                        .willReturn(pushDeviceSettings);
                treeContext = getContext();
            }

            @Nested
            @DisplayName(value = "the push message's message type is unknown")
            class ThePushMessageSMessageTypeIsUnknown {

                @BeforeEach
                public void beforeEach() {
                    given(messageId.getMessageType()).willReturn(DefaultMessageTypes.REGISTER);
                }

                @Test
                @DisplayName(value = "should return false outcome")
                public void testShouldReturnFalseOutcome() throws Exception {
                    Action result = node.process(treeContext);

                    assertThat(result.outcome).isEqualTo("FALSE");
                }
            }

            @Nested
            @DisplayName(value = "the message is NOT expired")
            class TheMessageIsNotExpired {

                @BeforeEach
                public void beforeEach() throws Exception {
                    given(messageHandler.check(messageId)).willReturn(MessageState.UNKNOWN);
                    treeContext = getContextWithPushTimeout(60000);
                }

                @Test
                @DisplayName(value = "should return WAITING outcome")
                public void testShouldReturnWaitingOutcome() throws Exception {
                    Action result = node.process(treeContext);

                    assertThat(result.outcome).isEqualTo("WAITING");
                }
            }

            @Nested
            @DisplayName(value = "the message is expired")
            class TheMessageIsExpired {

                @BeforeEach
                public void beforeEach() throws Exception {
                    given(messageHandler.check(messageId)).willReturn(MessageState.UNKNOWN);
                    treeContext = getContextWithPushTimeout(-1000);
                }

                @Test
                @DisplayName(value = "should return EXPIRED outcome")
                public void testShouldReturnExpiredOutcome() throws Exception {
                    Action result = node.process(treeContext);

                    assertThat(result.outcome).isEqualTo("EXPIRED");
                }
            }

            @Nested
            @DisplayName(value = "the push message's message type is known")
            class ThePushMessageSMessageTypeIsKnown {

                @BeforeEach
                public void beforeEach() {
                    given(messageId.getMessageType()).willReturn(DefaultMessageTypes.AUTHENTICATE);
                }

                @Test
                @DisplayName(value = "should check the message state with the correct handler")
                public void testShouldCheckTheMessageStateWithTheCorrectHandler() throws Exception {
                    node.process(treeContext);

                    verify(messageHandler).check(messageId);
                }

                @Nested
                @DisplayName(value = "the message state can't be retrieved")
                class TheMessageStateCanTBeRetrieved {

                    @BeforeEach
                    public void beforeEach() throws Exception {
                        given(messageHandler.check(messageId)).willReturn(null);
                    }

                    @Test
                    @DisplayName(value = "should return EXPIRED outcome")
                    public void testShouldReturnExpiredOutcome() throws Exception {
                        Action result = node.process(treeContext);

                        assertThat(result.outcome).isEqualTo("EXPIRED");
                    }
                }
            }
        }
    }

    private TreeContext getContext() {
        JsonValue sharedState = json(object(
                field(MESSAGE_ID_KEY, "badger"), field(REALM, "weasel")
        ));
        return new TreeContext(sharedState, new Builder().build(), emptyList(), Optional.empty());
    }

    private TreeContext getContextWithPushTimeout(int timeout) {
        JsonValue sharedState = json(object(
                field(REALM, "weasel"),
                field(MESSAGE_ID_KEY, "badger"),
                field(TIME_TO_LIVE_KEY, 60000),
                field(PUSH_MESSAGE_EXPIRATION, getEndTime(timeout))
        ));
        return new TreeContext(
                sharedState,
                new Builder().build(),
                emptyList(),
                Optional.empty());
    }

    private long getEndTime(int timeout) {
        return Time.getClock().instant().plusMillis(timeout).toEpochMilli();
    }

}
