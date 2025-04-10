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
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.EmailSuspendNode.RESUME_URI;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SuspendedTextOutputCallback;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EmailSuspendNodeTest {

    private static final String SUSPEND_MESSAGE = "Suspend message.";
    private static final URI RESUME_URI_VALUE = URI.create("http://openam.example.com");
    @Captor
    ArgumentCaptor<String> recipientCaptor;
    @Captor
    ArgumentCaptor<JsonValue> objectCaptor;
    @Mock
    private EmailSuspendNode.Config config;
    @Mock
    private Realm realm;
    @Mock
    private IdmIntegrationService idmIntegrationService;
    @Mock
    private ExecutorService executorService;
    @Mock
    private LocaleSelector localeSelector;
    @InjectMocks
    private EmailSuspendNode emailSuspendNode;

    @Test
    void shouldSuspendIfNoEmailAddressFoundInContext() throws Exception {
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(config.suspendDuration()).thenReturn(Optional.empty());
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        // no managed object to look up
        when(config.objectLookup()).thenReturn(false);
        when(config.identityAttribute()).thenReturn(null);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getSharedAttributesFromContext(any())).thenCallRealMethod();

        Action action = emailSuspendNode.process(new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.of("universalId")));

        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.suspendDuration).isEmpty();
        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldSuspendIfNoEmailAddressInManagedObject() throws Exception {
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))));
        JsonValue userObject = userObject();
        userObject.remove(DEFAULT_IDM_MAIL_ATTRIBUTE);

        when(config.objectLookup()).thenReturn(true);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject);

        Action action = emailSuspendNode.process(getContext(emptyList(), sharedState));

        assertThat(action.outcome).isEqualTo(null);
        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldSuspendIfObjectNotFound() throws Exception {
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
        ));

        when(config.objectLookup()).thenReturn(true);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);

        Action action = emailSuspendNode.process(getContext(emptyList(), sharedState));

        assertThat(action.outcome).isEqualTo(null);
        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldSuspendIfIdentityNotFound() throws Exception {
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        JsonValue sharedState = json(object());

        when(config.objectLookup()).thenReturn(true);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);

        Action action = emailSuspendNode.process(getContext(emptyList(), sharedState));

        assertThat(action.outcome).isEqualTo(null);
        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldSendMailFromContextToTemplateEndpointIfNoObject() throws Exception {
        when(config.emailTemplateName()).thenReturn(DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE);
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "sharedState@gmail.com")
                ))
        ));
        TreeContext context = getContext(emptyList(), sharedState);

        when(config.identityAttribute()).thenReturn(null);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);

        doNothing().when(idmIntegrationService)
                .sendTemplate(any(), any(), any(), any(), recipientCaptor.capture(), objectCaptor.capture());
        when(idmIntegrationService.getSharedAttributesFromContext(any())).thenCallRealMethod();

        // should still return suspend message if no best locale
        when(localeSelector.getBestLocale(any(), any())).thenReturn(null);

        Action action = emailSuspendNode.process(context);

        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        assertThat(context.sharedState.get(OBJECT_ATTRIBUTES).isDefined(RESUME_URI)).isFalse();
        assertThat(recipientCaptor.getValue()).isEqualTo("sharedState@gmail.com");
        assertThat(objectCaptor.getValue().isDefined(RESUME_URI)).isTrue();
    }

    @Test
    void shouldSendMailFromObjectToTemplateEndpoint() throws Exception {
        when(config.emailTemplateName()).thenReturn(DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE);
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(config.objectLookup()).thenReturn(true);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService)
                .sendTemplate(any(), any(), any(), any(), recipientCaptor.capture(), objectCaptor.capture());
        // ensure that locale is used if correctly returned
        when(localeSelector.getBestLocale(any(), any())).thenReturn(new Locale("en"));

        Action action = emailSuspendNode.process(getContext(emptyList(), sharedState));

        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        assertThat(recipientCaptor.getValue()).isEqualTo("test@gmail.com");
        JsonValue templateObject = userObject();
        templateObject.put(RESUME_URI, RESUME_URI_VALUE);
        assertThat(objectCaptor.getValue().keys().containsAll(templateObject.keys())).isTrue();
    }

    @Test
    void shouldSendMailFromUsernameIdentityToTemplateEndpoint() throws Exception {
        when(config.emailTemplateName()).thenReturn(DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE);
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        JsonValue sharedState = json(object(
                field(USERNAME, "test")
        ));

        when(config.objectLookup()).thenReturn(true);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService)
                .sendTemplate(any(), any(), any(), any(), recipientCaptor.capture(), objectCaptor.capture());
        // ensure that locale is used if correctly returned
        when(localeSelector.getBestLocale(any(), any())).thenReturn(new Locale("en"));

        Action action = emailSuspendNode.process(getContext(emptyList(), sharedState));

        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo(SUSPEND_MESSAGE);
        assertThat(recipientCaptor.getValue()).isEqualTo("test@gmail.com");
        JsonValue templateObject = userObject();
        templateObject.put(RESUME_URI, RESUME_URI_VALUE);
        assertThat(objectCaptor.getValue().keys().containsAll(templateObject.keys())).isTrue();
    }

    @Test
    void shouldUseDefaultMessageIfLocalizationNotPresent() throws Exception {
        when(config.emailTemplateName()).thenReturn(DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE);
        when(config.emailSuspendMessage()).thenReturn(singletonMap(new Locale("en"), SUSPEND_MESSAGE));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                ))
        ));
        TreeContext context = getContext(emptyList(), sharedState);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(config.emailSuspendMessage()).thenReturn(emptyMap());
        when(idmIntegrationService.getSharedAttributesFromContext(any())).thenCallRealMethod();
        doNothing().when(idmIntegrationService).sendTemplate(any(), any(), any(), any(), any(), any());

        Action action = emailSuspendNode.process(context);
        assertThat(((SuspendedTextOutputCallback) action.suspensionHandler.handle(RESUME_URI_VALUE)).getMessage())
                .isEqualTo("An email has been sent to your inbox.");
        assertThat(context.sharedState.get(OBJECT_ATTRIBUTES).isDefined(RESUME_URI)).isFalse();
    }

    @Test
    void shouldNotSuspendIfResumingNode() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                ))
        ));

        TreeContext context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, json(object()),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), true, Optional.empty());

        Action action = emailSuspendNode.process(context);

        verify(idmIntegrationService, times(0)).getObject(any(), any(), any(), any(String.class), any(), any(), any());
        verify(idmIntegrationService, times(0)).sendTemplate(any(), any(), any(), any(), any(), any());
        assertThat(action.suspensionHandler).isNull();
        assertThat(action.outcome).isEqualTo("outcome");
    }

    @Test
    void shouldSuspendWithSuspendDurationIfPresentInConfig() throws NodeProcessException {
        when(config.suspendDuration()).thenReturn(Optional.of(Duration.ofMinutes(1)));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                ))
        ));

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.emailAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getSharedAttributesFromContext(any())).thenCallRealMethod();

        TreeContext context = getContext(emptyList(), sharedState);

        Action action = emailSuspendNode.process(context);

        assertThat(action.suspendDuration).contains(Duration.ofMinutes(1));
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
