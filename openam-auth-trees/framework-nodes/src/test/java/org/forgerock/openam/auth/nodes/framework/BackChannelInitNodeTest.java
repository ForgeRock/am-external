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
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.REDIRECT_URI_STATE_KEY;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.TRANSACTION_ID_STATE_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.forgerock.am.authn.backchannel.BackChannelTransactionInformationResponseData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionInitializeRequestData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionInitializeResponseData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionService;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthErrorCode;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackChannelInitNodeTest {

    @Mock
    private Realm realm;
    @Mock
    private BackChannelInitNode.Config backChannelInitNodeConfig;
    @Mock
    private BackChannelTransactionService backChannelTransactionService;
    @Mock
    private BackChannelTransactionInformationResponseData informationResponseData;
    @Mock
    private BaseURLProviderFactory baseURLProviderFactory;
    @Mock
    private BaseURLProvider baseURLProvider;

    @InjectMocks
    private BackChannelInitNode backChannelInitNode;

    @Test
    void getInputsWithConfigKeysShouldReturnBothInputs() {
        // given
        given(backChannelInitNodeConfig.usernameKey()).willReturn(Optional.of("username"));
        given(backChannelInitNodeConfig.dataKey()).willReturn(Optional.of("data"));

        // when
        var inputs = backChannelInitNode.getInputs();

        // then
        assertThat(inputs).anyMatch(input -> input.name().equals("username"));
        assertThat(inputs).anyMatch(input -> input.name().equals("data"));
    }

    @Test
    void getInputsWithEmptyConfigKeysShouldReturnEmptyInputs() {
        // given
        given(backChannelInitNodeConfig.usernameKey()).willReturn(Optional.empty());
        given(backChannelInitNodeConfig.dataKey()).willReturn(Optional.empty());

        // when
        var inputs = backChannelInitNode.getInputs();

        // then
        assertThat(inputs).isEmpty();
    }

    @Test
    void getOutputsShouldReturnBothOutputs() {
        // when
        var outputs = backChannelInitNode.getOutputs();

        // then
        assertThat(outputs).anyMatch(output -> output.name().equals(TRANSACTION_ID_STATE_KEY));
        assertThat(outputs).anyMatch(output -> output.name().equals(REDIRECT_URI_STATE_KEY));
    }

    @Test
    void processShouldMakeInitRequestAndRecordResponseData() throws Exception {
        // given
        given(backChannelInitNodeConfig.usernameKey()).willReturn(Optional.empty());
        given(backChannelInitNodeConfig.dataKey()).willReturn(Optional.empty());
        given(backChannelInitNodeConfig.redirectUrlType()).willReturn(BackChannelInitNode.RedirectUrlType.POST);
        given(backChannelInitNodeConfig.idType()).willReturn(BackChannelInitNode.SubjectType.NONE);
        given(backChannelInitNodeConfig.tree()).willReturn("tree");
        given(backChannelTransactionService.initialize(any(), eq(realm), any()))
                .willReturn(new BackChannelTransactionInitializeResponseData("transactionId",
                        "redirectUri"));
        given(backChannelTransactionService.checkStatus("transactionId")).willReturn(informationResponseData);
        given(informationResponseData.auditTrackingIds()).willReturn(List.of("abc-123"));
        given(baseURLProviderFactory.get(realm)).willReturn(baseURLProvider);
        JsonValue sharedState = json(object());

        // when
        var action = backChannelInitNode.process(getContext(sharedState));

        // then
        assertThat(sharedState.get(TRANSACTION_ID_STATE_KEY).asString()).isEqualTo("transactionId");
        assertThat(sharedState.get(REDIRECT_URI_STATE_KEY).asString()).isEqualTo("redirectUri");
        assertThat(backChannelInitNode.getAuditEntryDetail().get("trackingIds").asList())
                .containsExactly("abc-123");
        assertThat(action.outcome).isEqualTo("created");
    }

    @Test
    void processShouldMakeInitRequestWithUserAndDataAndRecordResponseData() throws Exception {
        // given
        given(backChannelInitNodeConfig.usernameKey()).willReturn(Optional.of("username"));
        given(backChannelInitNodeConfig.dataKey()).willReturn(Optional.of("data"));
        given(backChannelInitNodeConfig.redirectUrlType()).willReturn(BackChannelInitNode.RedirectUrlType.POST);
        given(backChannelInitNodeConfig.idType()).willReturn(BackChannelInitNode.SubjectType.USER);
        given(backChannelInitNodeConfig.tree()).willReturn("tree");
        ArgumentCaptor<BackChannelTransactionInitializeRequestData> initCaptor =
                ArgumentCaptor.forClass(BackChannelTransactionInitializeRequestData.class);
        given(backChannelTransactionService.initialize(initCaptor.capture(), eq(realm), any()))
                .willReturn(new BackChannelTransactionInitializeResponseData("transactionId",
                        "redirectUri"));
        given(backChannelTransactionService.checkStatus("transactionId")).willReturn(informationResponseData);
        given(informationResponseData.auditTrackingIds()).willReturn(List.of("abc-123"));
        given(informationResponseData.subject())
                .willReturn(new BackChannelTransactionInformationResponseData.Subject("user", "demo"));
        given(baseURLProviderFactory.get(realm)).willReturn(baseURLProvider);
        JsonValue sharedState = json(object(field("username", "demo"), field("data", object(field("key", "value")))));

        // when
        var action = backChannelInitNode.process(getContext(sharedState));

        // then
        assertThat(sharedState.get(TRANSACTION_ID_STATE_KEY).asString()).isEqualTo("transactionId");
        assertThat(sharedState.get(REDIRECT_URI_STATE_KEY).asString()).isEqualTo("redirectUri");
        assertThat(backChannelInitNode.getAuditEntryDetail().get("trackingIds").asList())
                .containsExactly("abc-123");
        assertThat(backChannelInitNode.getAuditEntryDetail().get("subject").asMap().get("name")).isEqualTo("demo");
        assertThat(backChannelInitNode.getAuditEntryDetail().get("subject").asMap().get("type")).isEqualTo("user");
        assertThat(initCaptor.getValue().subject().name()).isEqualTo("demo");
        assertThat(initCaptor.getValue().subject().type()).isEqualTo("user");
        assertThat(initCaptor.getValue().data().get("key")).isEqualTo("value");
        assertThat(action.outcome).isEqualTo("created");
    }

    @Test
    void processShouldGoToUnknownUserIfUserIsUnknown() throws Exception {
        // given
        given(baseURLProviderFactory.get(realm)).willReturn(baseURLProvider);
        given(backChannelInitNodeConfig.usernameKey()).willReturn(Optional.of("username"));
        given(backChannelInitNodeConfig.dataKey()).willReturn(Optional.of("data"));
        given(backChannelInitNodeConfig.redirectUrlType()).willReturn(BackChannelInitNode.RedirectUrlType.POST);
        given(backChannelInitNodeConfig.idType()).willReturn(BackChannelInitNode.SubjectType.USER);
        given(backChannelInitNodeConfig.tree()).willReturn("tree");
        ArgumentCaptor<BackChannelTransactionInitializeRequestData> initCaptor =
                ArgumentCaptor.forClass(BackChannelTransactionInitializeRequestData.class);
        given(backChannelTransactionService.initialize(initCaptor.capture(), eq(realm), any()))
                .willThrow(new BackChannelAuthException(BackChannelAuthErrorCode.INVALID_IDENTITY));
        JsonValue sharedState = json(object(field("username", "demo"), field("data", object(field("key", "value")))));

        // when
        var action = backChannelInitNode.process(getContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("unknown");
    }

    private TreeContext getContext(JsonValue sharedState) {
        return TreeContext.builder().sharedState(sharedState).build();
    }

}
