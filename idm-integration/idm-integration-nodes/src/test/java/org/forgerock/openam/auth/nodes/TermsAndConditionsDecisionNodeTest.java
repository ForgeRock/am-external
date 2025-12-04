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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.TermsAndConditionsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TermsAndConditionsDecisionNodeTest {

    @Mock
    private TermsAndConditionsDecisionNode.Config config;

    @Mock
    private TermsAndConditionsConfig termsConfig;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private TermsAndConditionsDecisionNode node;
    private TreeContext context;

    public static Stream<Arguments> termsAndConditionsDecisionData() {
        return Stream.of(
                Arguments.of(json(object(field("termsAccepted", object(field("termsVersion", "1.0"))))), "1.0", TRUE_OUTCOME_ID),
                Arguments.of(json(object(field("termsAccepted", object(field("termsVersion", "1.0"))))), "2.0", FALSE_OUTCOME_ID),
                Arguments.of(json(object(field("termsAccepted", null))), "2.0", FALSE_OUTCOME_ID)
        );
    }

    @BeforeEach
    void before() throws Exception {
        when(idmIntegrationService.getActiveTerms(any(), any())).thenReturn(termsConfig);

        node = new TermsAndConditionsDecisionNode(config, realm, idmIntegrationService);
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    @ParameterizedTest
    @MethodSource("termsAndConditionsDecisionData")
    public void shouldReturnExpectedOutcome(JsonValue termsAccepted, String activeVersion, String expected)
            throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getAcceptedTerms(any(), any(), any(), any(), any())).thenReturn(termsAccepted);
        when(termsConfig.getVersion()).thenReturn(activeVersion);

        assertThat(node.process(context).outcome).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("termsAndConditionsDecisionData")
    public void shouldReturnExpectedOutcomeWithUsernameAsIdentity(JsonValue termsAccepted, String activeVersion,
            String expected) throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveUsernameSharedState(),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        when(idmIntegrationService.getAcceptedTerms(any(), any(), any(), any(), any())).thenReturn(termsAccepted);
        when(termsConfig.getVersion()).thenReturn(activeVersion);

        assertThat(node.process(context).outcome).isEqualTo(expected);

    }

    @Test
    void shouldThrowNodeProcessExceptionIfNoIdentity() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, json(object()),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        assertThatThrownBy(() -> node.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("userName not present in state");
    }

    @Test
    void shouldThrowNodeProcessExceptionIfGetAcceptedTermsFails() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getAcceptedTerms(any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException());

        assertThatThrownBy(() -> node.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    @Test
    void shouldReturnTrueOutcomeIfTermsConfigCannotBeFound() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getActiveTerms(any(), any())).thenThrow(new NotFoundException());
        when(idmIntegrationService.getAcceptedTerms(any(), any(), any(), any(), any())).thenReturn(json(object()));

        assertThat(node.process(context).outcome).isEqualTo(TRUE_OUTCOME_ID);
    }

    @Test
    void shouldThrowNodeProcessExceptionIfGetTermsConfigCallFails() throws Exception {
        when(idmIntegrationService.getActiveTerms(any(), any())).thenThrow(new BadRequestException());

        assertThatThrownBy(() -> node.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    private JsonValue retrieveSharedState() {
        return json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
    }

    private JsonValue retrieveUsernameSharedState() {
        return json(object(
                field(USERNAME, "test-username")
        ));
    }
}
