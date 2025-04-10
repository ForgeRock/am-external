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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_KBAINFO_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.KbaConfig;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Test the KbaVerifyNode.
 */
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class KbaVerifyNodeTest {

    @Mock
    IdmIntegrationService idmIntegrationService;
    @Mock
    LocaleSelector localeSelector;
    @Mock
    private KbaVerifyNode.Config config;
    private KbaVerifyNode node;
    private KbaConfig kbaConfig;
    private JsonValue userObject;

    public static Stream<Arguments> preferredLanguage() {
        return Stream.of(
                Arguments.of(Locale.ENGLISH, "Question One?"),
                Arguments.of(Locale.FRENCH, "Question Une?"),
                Arguments.of(Locale.GERMAN, "Unable to find translation."),
                Arguments.of(null, "Question One?")
        );
    }

    public static Stream<Arguments> sharedStateData() {
        return Stream.of(
                Arguments.of(json(object(
                        field(OBJECT_ATTRIBUTES, object(
                                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                        ))
                ))),
                Arguments.of(json(object(
                        field(USERNAME, "test-username")
                )))
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        kbaConfig = OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaVerifyNode/idmKbaConfig.json"), KbaConfig.class);
        userObject = json(OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaVerifyNode/idmUserObject.json"), Map.class));

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.kbaInfoAttribute()).thenReturn(DEFAULT_KBAINFO_ATTRIBUTE);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);

        node = new KbaVerifyNode(config, null, idmIntegrationService, localeSelector, new CryptoService());
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveKbaConfig() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getKbaConfig(any(), any())).thenThrow(newResourceException(BAD_REQUEST));

        assertThatThrownBy(() -> node.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveIdentity() throws Exception {
        JsonValue sharedState = json(object());

        assertThatThrownBy(() -> node.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("Failed to retrieve user object");
    }

    @Test
    void callbacksAbsentShouldReturnCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        // When
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        assertThat(((PasswordCallback) action.callbacks.get(0)).getPrompt()).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("preferredLanguage")
    public void callbacksAbsentNoLocaleShouldReturnEnglish(Locale locale, String question) throws Exception {
        // Given
        kbaConfig.setMinimumAnswersToVerify(2);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(locale);
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        // When
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(2);
        assertThat(((PasswordCallback) action.callbacks.get(0)).getPrompt()).isEqualTo(question);
        assertThat(((PasswordCallback) action.callbacks.get(1)).getPrompt()).isEqualTo("custom?");
    }

    @ParameterizedTest
    @MethodSource("sharedStateData")
    public void callbacksPresentProcessesSuccessfully(JsonValue sharedState) throws Exception {
        // Given
        List<Callback> callbacks = new ArrayList<>();
        PasswordCallback callback = new PasswordCallback("Question One?", false);
        callback.setPassword("bar!".toCharArray());
        callbacks.add(callback);

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.callbacks).hasSize(0);
        assertThat(action.outcome).isNotNull();
    }

    @Test
    void invalidCallbacksGoToFalse() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        List<Callback> callbacks = new ArrayList<>();
        PasswordCallback callback = new PasswordCallback("unknown question", false);
        callback.setPassword("ignored answer".toCharArray());
        callbacks.add(callback);

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.outcome).isEqualTo("false");
    }

    @Test
    void shouldProcessCallbacksWhenCustomKBAInfoAppearsFirstInKBAInfoConfig() throws NodeProcessException,
            IOException {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        // Use a configuration that allows for a larger KBAInfo list
        kbaConfig = OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaVerifyNode/KBAInfoOrder/idmKbaConfig.json"), KbaConfig.class);
        userObject = json(OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaVerifyNode/KBAInfoOrder/idmUserObject-customKBAInfoFirst.json"), Map.class));
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject);

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(createPasswordCallback("Question One?", "test", false));
        callbacks.add(createPasswordCallback("Question Two?", "test", false));
        callbacks.add(createPasswordCallback("custom 1?", "test", false));
        callbacks.add(createPasswordCallback("custom 2?", "test", false));
        callbacks.add(createPasswordCallback("custom 3?", "test", false));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.outcome).isEqualTo("true");
    }

    private PasswordCallback createPasswordCallback(String prompt, String password, boolean echoOn) {
        PasswordCallback callback = new PasswordCallback(prompt, echoOn);
        callback.setPassword(password.toCharArray());
        return callback;
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
