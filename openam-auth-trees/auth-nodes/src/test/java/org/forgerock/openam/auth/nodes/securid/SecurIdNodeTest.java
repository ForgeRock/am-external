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
package org.forgerock.openam.auth.nodes.securid;

import static javax.security.auth.callback.ConfirmationCallback.INFORMATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.APPROVE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PASSWORD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PUSH;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.VOICE_OR_SMS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AVAILABLE_METHODS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CHOSEN_METHOD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.securIdResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurIdNodeTest {
    @Mock
    private SecurIdNode.Config config;
    @Mock
    private SecurIdRestClient.Factory restClientFactory;
    @Mock
    private SecurIdRestClient restClient;
    @Mock
    private SecurIdResponseHandler.Factory responseHandlerFactory;
    @Mock
    private SecurIdResponseHandler responseHandler;
    @Mock
    private AvailableMethodHandler.Factory methodHandlerFactory;
    @Mock
    private SecurIdVerifyService.Factory verifyServiceFactory;
    @Mock
    private SecurIdVerifyService verifyService;
    @Mock
    private LocalizedMessageProviderFactory localizedMessageProviderFactory;
    @Mock
    private Realm realm;

    private SecurIdNode node;

    @BeforeEach
    void setup() {
        given(restClientFactory.create(eq(realm), any(), any(), anyInt(), any())).willReturn(restClient);
        given(responseHandlerFactory.create(any())).willReturn(responseHandler);
        given(verifyServiceFactory.create(any())).willReturn(verifyService);
        given(localizedMessageProviderFactory.create(realm)).willReturn(mock(LocalizedMessageProvider.class));
        node = new SecurIdNode(config, realm, restClientFactory, verifyServiceFactory, responseHandlerFactory,
                methodHandlerFactory, localizedMessageProviderFactory);
    }

    @Test
    void returnsErrorOutcomeWhenRestClientThrowsException() throws Exception {
        var sharedState = json(object(field("username", "user")));
        var context = TreeContext.builder().sharedState(sharedState).build();
        given(restClient.initialize(any(), any(), any())).willThrow(new IOException("Network error"));

        var action = node.process(context);

        assertThat(action.outcome).isEqualTo(OutcomeId.ERROR);
    }

    @Test
    void goesToErrorOutcomeIfNoUsernameInState() {
        var context = TreeContext.builder().build();

        var action = node.process(context);

        assertThat(action.outcome).isEqualTo(OutcomeId.ERROR);
    }

    @Test
    void initializesAuthenticationIfNoCallbacksArePresent() throws Exception {
        var sharedState = json(object(field("username", "user")));
        var context = TreeContext.builder().sharedState(sharedState).build();
        given(config.clientId()).willReturn(Optional.of("clientId"));
        given(config.assurancePolicy()).willReturn(Optional.of("assurancePolicy"));
        var initializeResponse = securIdResponse().build();
        given(restClient.initialize("user", "clientId", "assurancePolicy"))
                .willReturn(initializeResponse);
        Action result = Action.goTo("SUCCESS").build();
        given(responseHandler.handleInitialize(eq(initializeResponse), any(), any(), any())).willReturn(result);

        assertThat(node.process(context)).isEqualTo(result);
    }

    @Test
    void returnsCancelOutcomeWhenCancelIsClicked() {
        var sharedState = json(object(
                field(CONFIRMATION_CB, json(List.of("Next", "cancel")))
        ));
        var confirmationCallback = new ConfirmationCallback(INFORMATION, new String[]{"Next", "cancel"}, 0);
        confirmationCallback.setSelectedIndex(1);
        var context = TreeContext.builder().sharedState(sharedState).callbacks(List.of(confirmationCallback)).build();

        var action = node.process(context);

        assertThat(action.outcome).isEqualTo(OutcomeId.CANCEL);
    }

    @Test
    void sendsChallengeCallbacksCorrespondingToSelectedMethod() throws Exception {
        var sharedState = json(object(
                field(AVAILABLE_METHODS, json(array("TOKEN", "APPROVE", "VOICE")))
        ));
        var choiceCallback = new ChoiceCallback("prompt", new String[]{"choices"}, 0, false);
        choiceCallback.setSelectedIndex(1); // APPROVE selected
        var context = TreeContext.builder().sharedState(sharedState).callbacks(List.of(choiceCallback)).build();
        List<Callback> challengeCallbacks = List.of(new PasswordCallback("prompt", false));
        given(verifyService.beginVerification(eq(APPROVE), any(), any())).willReturn(challengeCallbacks);

        var action = node.process(context);

        assertThat(action.callbacks).containsAll(challengeCallbacks);
    }

    @ParameterizedTest
    @MethodSource("tokenMethods")
    void verifiesTokenMethods(SecurIdMethod method) throws Exception {
        var sharedState = json(object(
                field(CHOSEN_METHOD, json(method.name())),
                field(RESPONSE_MESSAGE_ID, json("messageId")),
                field(AUTHN_ATTEMPT_ID, json("attemptId"))
        ));
        var passwordCallback = new PasswordCallback("prompt", false);
        String userToken = "userToken";
        passwordCallback.setPassword(userToken.toCharArray());
        var context = TreeContext.builder().sharedState(sharedState).callbacks(List.of(passwordCallback)).build();
        var response = securIdResponse().build();
        given(restClient.verify("messageId", "attemptId", tokenSubjectCredentials(method, userToken)))
                .willReturn(response);
        Action result = mock(Action.class);
        given(responseHandler.handleVerify(eq(response), any(), eq(method), any(), any())).willReturn(result);

        assertThat(node.process(context)).isEqualTo(result);
    }

    private static Stream<Arguments> tokenMethods() {
        return Stream.of(SecurIdMethod.values())
                .filter(m -> m.type().equals(PASSWORD) || m.type().equals(VOICE_OR_SMS))
                .map(Arguments::of);
    }

    private static List<SubjectCredentials> tokenSubjectCredentials(SecurIdMethod method, String userToken) {
        var methodName = method.type().equals(VOICE_OR_SMS) ? method.name() + "_VERIFY_CODE" : method.name();
        return List.of(new SubjectCredentials(methodName, null,
                List.of(new SubjectCredentials.CollectedInput(methodName, userToken, "STRING"))));
    }

    @ParameterizedTest
    @MethodSource("pushMethods")
    void verifiesPushMethods(SecurIdMethod method) throws Exception {
        String referenceId = "referenceId";
        var sharedState = json(object(
                field(CHOSEN_METHOD, json(method.name())),
                field(RESPONSE_MESSAGE_ID, json("messageId")),
                field(AUTHN_ATTEMPT_ID, json("attemptId")),
                field(PUSH_REFERENCE_ID, json(referenceId))
        ));
        var context = TreeContext.builder().sharedState(sharedState).callbacks(List.of(mock(Callback.class))).build();
        var response = securIdResponse().build();
        given(restClient.verify("messageId", "attemptId", pushSubjectCredentials(method, referenceId)))
                .willReturn(response);
        Action result = mock(Action.class);
        given(responseHandler.handleVerify(eq(response), any(), eq(method), any(), any())).willReturn(result);

        assertThat(node.process(context)).isEqualTo(result);
    }

    private static Stream<Arguments> pushMethods() {
        return Stream.of(SecurIdMethod.values())
                .filter(m -> m.type().equals(PUSH))
                .map(Arguments::of);
    }

    private static List<SubjectCredentials> pushSubjectCredentials(SecurIdMethod method, String referenceId) {
        return List.of(new SubjectCredentials(method.name(), referenceId, null));
    }
}
