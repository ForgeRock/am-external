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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
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
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_WELCOME_EMAIL_TEMPLATE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class EmailTemplateNodeTest {

    @Captor
    ArgumentCaptor<String> recipientCaptor;
    @Captor
    ArgumentCaptor<JsonValue> objectCaptor;
    @Mock
    private EmailTemplateNode.Config config;
    @Mock
    private Realm realm;
    @Mock
    private IdmIntegrationService idmIntegrationService;
    @Mock
    private ExecutorService executorService;
    private EmailTemplateNode emailTemplateNode;

    public static Stream<Arguments> sharedStateData() {
        return Stream.of(
                Arguments.of(json(object(
                        field(OBJECT_ATTRIBUTES, object(
                                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                                field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                        ))
                ))),
                Arguments.of(json(object(
                        field(USERNAME, "test"),
                        field(OBJECT_ATTRIBUTES, object(
                                field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                        ))
                )))
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        emailTemplateNode = new EmailTemplateNode(config, realm, idmIntegrationService, executorService);

        when(config.emailTemplateName()).thenReturn(DEFAULT_IDM_WELCOME_EMAIL_TEMPLATE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
    }

    @Test
    void shouldContinueIfNoEmailAddressInManagedObject() throws Exception {
        JsonValue userObject = userObject();
        userObject.remove(DEFAULT_IDM_MAIL_ATTRIBUTE);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject);

        Action action = emailTemplateNode.process(getContext(emptyList(), json(object())));

        assertThat(action.outcome).isEqualTo("EMAIL_NOT_SENT");
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldContinueIfNoMailInContextAndOnlyIdentityAttributeSpecified() throws Exception {

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);

        Action action = emailTemplateNode.process(new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.of("universalId")));

        assertThat(action.outcome).isEqualTo("EMAIL_NOT_SENT");
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldContinueIfNoMailInContextAndOnlyObjectTypeSpecified() throws Exception {
        // no mail in context either
        when(config.identityAttribute()).thenReturn(null);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);

        Action action = emailTemplateNode.process(new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.of("universalId")));

        assertThat(action.outcome).isEqualTo("EMAIL_NOT_SENT");
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldContinueIfObjectNotFound() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(NOT_FOUND));

        Action action = emailTemplateNode.process(getContext(emptyList(), json(object())));

        assertThat(action.outcome).isEqualTo("EMAIL_NOT_SENT");
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldContinueIfIdentityNotFound() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);

        Action action = emailTemplateNode.process(getContext(emptyList(), json(object())));

        assertThat(action.outcome).isEqualTo("EMAIL_NOT_SENT");
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("sharedStateData")
    public void shouldSendMailFromObjectToTemplateEndpoint(JsonValue sharedState) throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService)
                .sendTemplate(any(), any(), any(), any(), recipientCaptor.capture(), objectCaptor.capture());

        Action action = emailTemplateNode.process(getContext(emptyList(), sharedState));

        assertThat(action.outcome).isEqualTo("EMAIL_SENT");
        assertThat(recipientCaptor.getValue()).isEqualTo("test@gmail.com");
        assertThat(objectCaptor.getValue().isEqualTo(userObject())).isTrue();
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private JsonValue userObject() {
        return json(object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com"),
                field("givenName", "foo"),
                field("sn", "bar")
        ));
    }
}
