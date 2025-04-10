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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.MAX_TREE_DURATION_IN_MINUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.AuthSessionHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.dpro.session.SessionException;

@ExtendWith(MockitoExtension.class)
class UpdateJourneyTimeoutNodeTest {

    @Mock
    private UpdateJourneyTimeoutNode.Config config;
    @Mock
    private AuthSessionHelper authSessionHelper;
    @Mock
    private Session session;
    @Mock
    private Realm realm;
    @Mock
    private AnnotatedServiceRegistry serviceRegistry;

    private UpdateJourneyTimeoutNode updateJourneyTimeoutNode;

    @InjectMocks
    private UpdateJourneyTimeoutNode.ValueValidator valueValidator;

    @BeforeEach
    void setUp() {
      updateJourneyTimeoutNode = new UpdateJourneyTimeoutNode(config, authSessionHelper);
    }


    @Test
    void shouldSetTimeoutIfOperationIsSet() throws NodeProcessException {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.SET);
        given(config.value()).willReturn(Duration.ofMinutes(5));
        JsonValue transientState = json(object());
        var context = getContext(transientState, null);

        // when
        var result = updateJourneyTimeoutNode.process(context);

        // then
        assertThat(transientState.get(MAX_TREE_DURATION_IN_MINUTES).asLong()).isEqualTo(5);
        assertThat(result.maxTreeDuration).contains(Duration.ofMinutes(5));
    }

    @Test
    void shouldModifyTimeoutIfOperationIsModify() throws NodeProcessException {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.MODIFY);
        given(config.value()).willReturn(Duration.ofMinutes(5));
        JsonValue transientState = json(object());
        var context = getContext(transientState, null);

        // when
        var result = updateJourneyTimeoutNode.process(context);

        // then
        assertThat(transientState.get(MAX_TREE_DURATION_IN_MINUTES).asLong()).isEqualTo(5);
        assertThat(result.maxTreeDuration).contains(Duration.ofMinutes(5));

    }

    @Test
    void shouldModifyTimeoutIfOperationIsModifyAndAuthIdIsPresent() throws NodeProcessException, SessionException {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.MODIFY);
        given(config.value()).willReturn(Duration.ofMinutes(5));
        String authId = "authId";
        given(authSessionHelper.getAuthSession(any())).willReturn(session);
        given(session.getMaxSessionTime()).willReturn(1L);
        JsonValue transientState = json(object());
        var context = getContext(transientState, authId);

        // when
        var result = updateJourneyTimeoutNode.process(context);

        // then
        assertThat(transientState.get(MAX_TREE_DURATION_IN_MINUTES).asLong()).isEqualTo(6);
        assertThat(result.maxTreeDuration).contains(Duration.ofMinutes(6));

    }

    @Test
    void shouldModifyTimeoutIfOperationIsModifyAndDurationInTransientState() throws NodeProcessException {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.MODIFY);
        given(config.value()).willReturn(Duration.ofMinutes(5));
        JsonValue transientState = json(object(field(MAX_TREE_DURATION_IN_MINUTES, 1)));
        var context = getContext(transientState, null);

        // when
        var result = updateJourneyTimeoutNode.process(context);

        // then
        assertThat(transientState.get(MAX_TREE_DURATION_IN_MINUTES).asLong()).isEqualTo(6);
        assertThat(result.maxTreeDuration).contains(Duration.ofMinutes(6));

    }

    @Test
    void shouldModifyTimeoutIfOperationIsModifyAndValueIsNegative() throws NodeProcessException, SessionException {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.MODIFY);
        given(config.value()).willReturn(Duration.ofMinutes(-1));
        String authId = "authId";
        given(authSessionHelper.getAuthSession(any())).willReturn(session);
        given(session.getMaxSessionTime()).willReturn(5L);
        JsonValue transientState = json(object());
        var context = getContext(transientState, authId);

        // when
        var result = updateJourneyTimeoutNode.process(context);

        // then
        assertThat(transientState.get(MAX_TREE_DURATION_IN_MINUTES).asLong()).isEqualTo(4);
        assertThat(result.maxTreeDuration).contains(Duration.ofMinutes(4));
    }

    @Test
    void shouldThrowExceptionIfOperationIsSetAndValueIsNegative() {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.SET);
        given(config.value()).willReturn(Duration.ofMinutes(-5));

        // when
        // then
        assertThatThrownBy(() -> updateJourneyTimeoutNode.process(null))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("The timeout value must be positive when SET operation is selected");
    }

    @Test
    void shouldThrowExceptionIfModifiedTimeoutIsNegative() throws Exception {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.MODIFY);
        given(config.value()).willReturn(Duration.ofMinutes(-6));
        String authId = "authId";
        given(authSessionHelper.getAuthSession(any())).willReturn(session);
        JsonValue transientState = json(object());
        var context = getContext(transientState, authId);

        // when
        // then
        assertThatThrownBy(() -> updateJourneyTimeoutNode.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("The resulting timeout value must be positive");
    }

    @Test
    void shouldThrowExceptionFromValidatorIfValueIsNegativeAndSetIsOperation() throws Exception {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.SET);
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.of(config));
        List<String> configPaths = List.of("value");
        Map<String, Set<String>> attributes = Map.of("value", Set.of("-5"));

        // when
        // then
        assertThatThrownBy(() -> valueValidator.validate(realm, configPaths, attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("The timeout value must be positive when SET operation is selected");
    }

    @Test
    void shouldThrowExceptionFromValidatorIfValueIsNull() throws Exception {
        // given
        given(config.operation()).willReturn(UpdateJourneyTimeoutNode.Operation.SET);
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.of(config));
        List<String> configPaths = List.of("value");
        Map<String, Set<String>> attributes = Map.of("value", Set.of());

        // when
        // then
        assertThatThrownBy(() -> valueValidator.validate(realm, configPaths, attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("The value and operation attributes are required");
    }

    private TreeContext getContext(JsonValue transientState, String authId) {
        return new TreeContext(json(object()), transientState,
                new ExternalRequestContext.Builder().authId(authId).build(), emptyList(), Optional.empty());
    }

}
