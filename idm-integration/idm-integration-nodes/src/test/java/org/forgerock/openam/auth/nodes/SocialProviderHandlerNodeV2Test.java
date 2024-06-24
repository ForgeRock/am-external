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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.mockito.Mockito.mock;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link SocialProviderHandlerNodeV2}
 */
@RunWith(MockitoJUnitRunner.class)
public class SocialProviderHandlerNodeV2Test extends SocialProviderHandlerNodeTest {

    private static final String EXPECT_PROFILE_INFORMATION = "expectProfileInformation";

    @Override
    protected void setConfigField() {
        config = mock(SocialProviderHandlerNodeV2.Config.class);
    }

    @Override
    protected void setNodeField() {
        node = new SocialProviderHandlerNodeV2((SocialProviderHandlerNodeV2.Config) config, authModuleHelper,
                providerConfigStore, identityService, realm,
                __ -> scriptEvaluator, sessionServiceProvider, idmIntegrationService);
    }

    @Test
    public void processWhenExpectingMissingSocialProviderParamsResultsInSocialAuthInterruptedOutcome()
            throws Exception {
        // Given
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME), field(EXPECT_PROFILE_INFORMATION, true))),
                new ExternalRequestContext.Builder().build(), emptyList());

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome)
                .isEqualTo(SocialProviderHandlerNodeV2.SocialAuthOutcomeV2.SOCIAL_AUTH_INTERRUPTED.name());
    }

    @Test
    public void processWhenNotExpectingMissingSocialProviderParamsResultsInNewFlagInNodeState() throws Exception {
        // Given
        JsonValue nodeState = json(object(field(SELECTED_IDP, PROVIDER_NAME)));
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                nodeState,
                new ExternalRequestContext.Builder().build(), emptyList());

        // When
        node.process(context);

        // Then
        assertThat(nodeState.get(JsonPointer.ptr(EXPECT_PROFILE_INFORMATION)).asBoolean()).isTrue();
    }

    @Test
    public void assertInputsAreCorrect() {
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
}
