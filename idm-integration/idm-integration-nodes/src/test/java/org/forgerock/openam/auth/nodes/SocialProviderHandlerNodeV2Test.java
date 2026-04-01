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
 * Copyright 2023-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNodeV2.SocialAuthOutcomeV2.TIMEOUT_OUTCOME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.http.ConnectionClosedException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link SocialProviderHandlerNodeV2}
 */
@ExtendWith(MockitoExtension.class)
public class SocialProviderHandlerNodeV2Test extends SocialProviderHandlerNodeTestBase {

    private static final String EXPECT_PROFILE_INFORMATION = "expectProfileInformation";

    @Mock
    private OAuthClientConfig idpConfig;

    @Mock
    private OAuthClient client;

    @Mock
    private DataStore dataStore;

    @Override
    protected void setConfigField() {
        config = mock(SocialProviderHandlerNodeV2.Config.class);
    }

    @Override
    protected void setNodeField() {
        node = new SocialProviderHandlerNodeV2((SocialProviderHandlerNodeV2.Config) config, authModuleHelper,
                providerConfigStore, identityService, realm,
                scriptEvaluatorFactory, idmIntegrationService, authScriptUtilities, transformationScriptContext,
                nodeScriptContext);
    }

    @Test
    void processWhenExpectingMissingSocialProviderParamsResultsInSocialAuthInterruptedOutcome()
            throws Exception {
        // Given
        TreeContext context = TreeContext.builder()
                .sharedState(json(object(field(SELECTED_IDP, PROVIDER_NAME), field(EXPECT_PROFILE_INFORMATION, true))))
                .build();

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome)
                .isEqualTo(SocialProviderHandlerNodeV2.SocialAuthOutcomeV2.SOCIAL_AUTH_INTERRUPTED.name());
    }

    @Test
    void processWhenNotExpectingMissingSocialProviderParamsResultsInNewFlagInNodeState() throws Exception {
        // Given
        JsonValue nodeState = json(object(field(SELECTED_IDP, PROVIDER_NAME)));
        TreeContext context = TreeContext.builder()
                .sharedState(nodeState)
                .build();

        // When
        node.process(context);

        // Then
        assertThat(nodeState.get(JsonPointer.ptr(EXPECT_PROFILE_INFORMATION)).asBoolean()).isTrue();
    }

    @Test
    void assertInputsAreCorrect() {
        // Given
        var expectedInputs = new InputState[]{
                new InputState(SELECTED_IDP),
                new InputState(config.usernameAttribute(), false),
                new InputState(IDPS, false),
                new InputState(EXPECT_PROFILE_INFORMATION, false)
        };

        // When
        var actualInputs = node.getInputs();

        // Then
        for (int i = 0; i < actualInputs.length; i++) {
            assertThat(actualInputs[i].name).isEqualTo(expectedInputs[i].name);
            assertThat(actualInputs[i].required).isEqualTo(expectedInputs[i].required);
        }
    }

    @Test
    void shouldReturnTimeoutOutcomeOnDetectingConnectionTimeOut() throws NodeProcessException {
        // Given
        JsonValue nodeState = json(object(field(SELECTED_IDP, PROVIDER_NAME)));
        TreeContext context = TreeContext.builder()
                .sharedState(nodeState)
                .build();
        when(config.detectConnectionTimeOut()).thenReturn(true);

        SocialProviderHandlerNodeV2 spyNode = createSpyNode();

        RuntimeException connectionClosed = new RuntimeException(new ConnectionClosedException("Connection closed"));
        doThrow(connectionClosed).when(spyNode).handleCallback(any(), anyString(), any(), any(), any());

        // When
        Action result = spyNode.getAction(context, "selectedIdp", idpConfig, client, dataStore);

        // Then
        assertThat(result.outcome).isEqualTo(TIMEOUT_OUTCOME.name());
    }

    @Test
    void shouldReturnThrowExceptionOnNotDetectingConnectionTimeOut() throws NodeProcessException {
        // Given
        JsonValue nodeState = json(object(field(SELECTED_IDP, PROVIDER_NAME)));
        TreeContext context = TreeContext.builder()
                .sharedState(nodeState)
                .build();
        when(config.detectConnectionTimeOut()).thenReturn(false);

        SocialProviderHandlerNodeV2 spyNode = createSpyNode();

        RuntimeException connectionClosed = new RuntimeException(new ConnectionClosedException("Connection closed"));
        doThrow(connectionClosed).when(spyNode).handleCallback(any(), anyString(), any(), any(), any());

        // When - Then
        assertThrows(RuntimeException.class, () -> {
            spyNode.getAction(context, "selectedIdp", idpConfig, client, dataStore);
        });
    }

    @Test
    void shouldThrowNodeProcessExceptionOnOtherExceptions() throws NodeProcessException {
        // Given
        JsonValue nodeState = json(object(field(SELECTED_IDP, PROVIDER_NAME)));
        TreeContext context = TreeContext.builder()
                .sharedState(nodeState)
                .build();

        SocialProviderHandlerNodeV2 spyNode = createSpyNode();

        Exception otherException = new RuntimeException("Some other error");
        doThrow(otherException).when(spyNode).handleCallback(any(), anyString(), any(), any(), any());

        // When/Then
        assertThatThrownBy(() -> spyNode.getAction(context, "selectedIdp", idpConfig, client, dataStore))
                .isInstanceOf(NodeProcessException.class)
                .hasCause(otherException)
                .hasMessage("Unexpected error during social login callback");
    }

    private SocialProviderHandlerNodeV2 createSpyNode() {
        return spy(new SocialProviderHandlerNodeV2(
                config, authModuleHelper, providerConfigStore,
                identityService, realm, scriptEvaluatorFactory, idmIntegrationService, authScriptUtilities,
                transformationScriptContext, nodeScriptContext));
    }
}
