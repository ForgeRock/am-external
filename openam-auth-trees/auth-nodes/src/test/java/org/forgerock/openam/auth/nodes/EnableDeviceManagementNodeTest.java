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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.EnableDeviceManagementNode.DeviceEnforcementStrategy.ANY;
import static org.forgerock.openam.auth.nodes.EnableDeviceManagementNode.DeviceEnforcementStrategy.NONE;
import static org.forgerock.openam.auth.nodes.EnableDeviceManagementNode.DeviceEnforcementStrategy.SAME;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sun.identity.idm.AMIdentity;

/**
 * Test for Enable Device Management Node.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EnableDeviceManagementNodeTest {

    private final String user = "rod";
    private final String realm = "/realm";

    private static final String PUSH_AUTH_TYPE = "AuthenticatorPush";
    private static final String WEB_AUTHN_AUTH_TYPE = "WebAuthnAuthentication";
    private static final String ELEVATED_PRIVILEGES = "ElevatedPrivileges";

    @Mock
    EnableDeviceManagementNode.Config config;

    @Mock
    AMIdentity amIdentity;

    @Mock
    NodeUserIdentityProvider identityProvider;
    EnableDeviceManagementNode node;

    @Test
    void testNonExistentUserWithFailureOutcome() throws Exception {
        // Given
        nodeConfigWithDefaultValues();

        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.empty());
        JsonValue sharedState = json(object(field(USERNAME, user), field(REALM, realm)));
        JsonValue transientState = json(object());

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE_OUTCOME_ID);
    }

    @Test
    void testEnforcementStrategyWithSameShouldReturnSuccessOutcome() throws Exception {
        // Given
        nodeConfigWithDefaultValues();

        JsonValue sharedState = json(object(
                field(USERNAME, user),
                field(REALM, realm),
                field(NODE_TYPE, PUSH_AUTH_TYPE)
                ));
        JsonValue transientState = json(object());

        //When
        TreeContext context = getContext(sharedState, transientState, emptyList());
        Action result = node.process(context);

        String sharedStateNodeType = context.sharedState.get(NODE_TYPE).asString();
        String sessionNodeType = result.sessionProperties.get(NODE_TYPE);

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(sharedStateNodeType).contains(PUSH_AUTH_TYPE);
        assertThat(sharedStateNodeType).doesNotContain(ELEVATED_PRIVILEGES);
        assertThat(sessionNodeType).isNull();
    }

    @Test
    void testEnforcementStrategyWithSameAndNoNodeTypeShouldReturnSuccessOutcome() throws Exception {
        // Given
        nodeConfigWithDefaultValues();

        JsonValue sharedState = json(object(
                field(USERNAME, user),
                field(REALM, realm)
        ));
        JsonValue transientState = json(object());

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
    }

    @Test
    void testEnforcementStrategyWithAnyShouldReturnSuccessOutcome() throws Exception {
        // Given
        nodeConfigWithValues(ANY);

        JsonValue sharedState = json(object(
                field(USERNAME, user),
                field(REALM, realm),
                field(NODE_TYPE, WEB_AUTHN_AUTH_TYPE)
        ));
        JsonValue transientState = json(object());

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        String sharedStateNodeType = result.sharedState.get(NODE_TYPE).asString();
        String sessionNodeType = result.sessionProperties.get(NODE_TYPE);

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(sharedStateNodeType).contains(WEB_AUTHN_AUTH_TYPE);
        assertThat(sharedStateNodeType).contains(ELEVATED_PRIVILEGES);
        assertThat(sessionNodeType).contains(ELEVATED_PRIVILEGES);
    }

    @Test
    void testEnforcementStrategyWithAnyWithoutNodeTypeShouldReturnFailureOutcome() throws Exception {
        // Given
        nodeConfigWithValues(ANY);

        JsonValue sharedState = json(object(
                field(USERNAME, user),
                field(REALM, realm)
        ));
        JsonValue transientState = json(object());

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE_OUTCOME_ID);
    }

    @Test
    void testEnforcementStrategyWithNoneShouldReturnSuccessOutcome() throws Exception {
        // Given
        nodeConfigWithValues(NONE);

        JsonValue sharedState = json(object(
                field(USERNAME, user),
                field(REALM, realm)
        ));
        JsonValue transientState = json(object());

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        String sharedStateNodeType = result.sharedState.get(NODE_TYPE).asString();
        String sessionNodeType = result.sessionProperties.get(NODE_TYPE);

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(sharedStateNodeType).contains(ELEVATED_PRIVILEGES);
        assertThat(sessionNodeType).contains(ELEVATED_PRIVILEGES);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(),
                callbacks, Optional.empty());
    }

    private void nodeConfigWithDefaultValues() {
        nodeConfigWithValues(SAME);
    }

    private void nodeConfigWithValues(EnableDeviceManagementNode.DeviceEnforcementStrategy enforcementStrategy) {
        given(config.deviceEnforcementStrategy()).willReturn(enforcementStrategy);
        given(identityProvider.getUniversalId(any())).willReturn(Optional.of("rod"));
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));

        node = spy(new EnableDeviceManagementNode(config, identityProvider));
    }
}
