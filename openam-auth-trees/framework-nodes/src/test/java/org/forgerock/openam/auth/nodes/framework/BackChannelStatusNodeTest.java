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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.JOURNEY_SESSION_PROPERTIES;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.TRANSACTION_ID_STATE_KEY;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.forgerock.am.authn.backchannel.BackChannelTransactionInformationResponseData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionService;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthErrorCode;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackChannelStatusNodeTest {

    @Mock
    private BackChannelStatusNode.Config config;
    @Mock
    private BackChannelTransactionService backChannelTransactionService;
    @Mock
    private BackChannelTransactionInformationResponseData informationResponseData;

    @InjectMocks
    BackChannelStatusNode backChannelStatusNode;

    @Test
    void testGetInputs() {
        // when
        var inputs = backChannelStatusNode.getInputs();

        // then
        assertThat(inputs).allMatch(input -> input.name().equals(TRANSACTION_ID_STATE_KEY));
    }

    @Test
    void getOutputsShouldReturnTheOutputsWhenRecordDataOnSuccess() {
        //given
        given(config.recordData()).willReturn(false);

        // when
        var outputs = backChannelStatusNode.getOutputs();

        // then
        assertThat(outputs).noneMatch(output -> output.name().equals(JOURNEY_SESSION_PROPERTIES));
    }

    @Test
    void getOutputsShouldReturnNoOutputsWhenNotRecordDataOnSuccess() {
        //given
        given(config.recordData()).willReturn(true);

        // when
        var outputs = backChannelStatusNode.getOutputs();

        // then
        assertThat(outputs).anyMatch(output -> output.name().equals(JOURNEY_SESSION_PROPERTIES));
    }

    @Test
    void processShouldReturnTheErrorWhenNoTransactionInState() throws NodeProcessException {
        // given
        JsonValue sharedState = json(object());

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("error");
    }

    @Test
    void processShouldReturnPendingWhenTransactionIsCreated() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willReturn(informationResponseData);
        given(informationResponseData.state()).willReturn("CREATED");

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("pending");
    }

    @Test
    void processShouldReturnInProgressWhenTransactionIsInProgress() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willReturn(informationResponseData);
        given(informationResponseData.state()).willReturn("IN_PROGRESS");

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("inprogress");
    }

    @Test
    void processShouldReturnUnknownWhenTransactionIsMissing() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willThrow(new BackChannelAuthException(BackChannelAuthErrorCode.TRANSACTION_NOT_FOUND));

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("unknown");
    }

    @Test
    void processShouldReturnErrorWhenErrorFetchingTransaction() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willThrow(new BackChannelAuthException(BackChannelAuthErrorCode.EMPTY_TRANSACTION_ID));

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("error");
    }

    @Test
    void processShouldReturnFailureWhenTransactionIsDenied() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willReturn(informationResponseData);
        given(informationResponseData.state()).willReturn("COMPLETED");
        given(informationResponseData.result()).willReturn("DENIED");

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("failure");
    }

    @Test
    void processShouldReturnSuccessWhenTransactionIsApproved() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willReturn(informationResponseData);
        given(informationResponseData.state()).willReturn("COMPLETED");
        given(informationResponseData.result()).willReturn("APPROVED");

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, json(object())));

        // then
        assertThat(action.outcome).isEqualTo("success");
        assertThat(sharedState.isDefined(JOURNEY_SESSION_PROPERTIES)).isFalse();
    }

    @Test
    void processShouldAddUserToStateWhenTransactionIsApproved() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        JsonValue transientState = json(object());
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willReturn(informationResponseData);
        given(informationResponseData.state()).willReturn("COMPLETED");
        given(informationResponseData.result()).willReturn("APPROVED");
        given(config.recordData()).willReturn(false);

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, transientState));

        // then
        assertThat(action.outcome).isEqualTo("success");
        assertThat(transientState.isDefined(JOURNEY_SESSION_PROPERTIES)).isFalse();
    }

    @Test
    void processShouldAddSessionPropertiesToStateWhenTransactionIsApproved() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TRANSACTION_ID_STATE_KEY, "transactionId")));
        JsonValue transientState = json(object());
        given(backChannelTransactionService.checkStatus("transactionId"))
                .willReturn(informationResponseData);
        given(informationResponseData.state()).willReturn("COMPLETED");
        given(informationResponseData.result()).willReturn("APPROVED");
        given(informationResponseData.sessionProperties()).willReturn(Map.of("key", "value"));
        given(config.recordData()).willReturn(true);

        // when
        var action = backChannelStatusNode.process(getContext(sharedState, transientState));

        // then
        assertThat(action.outcome).isEqualTo("success");
        assertThat(transientState.get(JOURNEY_SESSION_PROPERTIES).asMap()).containsEntry("key", "value");
    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return TreeContext.builder()
                .sharedState(sharedState)
                .transientState(transientState)
                .build();
    }
}
