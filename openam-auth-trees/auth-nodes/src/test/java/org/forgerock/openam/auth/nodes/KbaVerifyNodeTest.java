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
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_KBAINFO_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the KbaVerifyNode.
 */
public class KbaVerifyNodeTest {

    @Mock
    private KbaVerifyNode.Config config;

    @Mock
    IdmIntegrationService idmIntegrationService;

    @Mock
    LocaleSelector localeSelector;

    private KbaVerifyNode node;
    private KbaConfig kbaConfig;
    private JsonValue userObject;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        kbaConfig = OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaVerifyNode/idmKbaConfig.json"), KbaConfig.class);
        userObject = json(OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaVerifyNode/idmUserObject.json"), Map.class));

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.kbaInfoAttribute()).thenReturn(DEFAULT_KBAINFO_ATTRIBUTE);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        doNothing().when(idmIntegrationService).patchObject(any(), any(), any(), any(), any(), any());
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);

        node = new KbaVerifyNode(config, null, idmIntegrationService, localeSelector, new CryptoService());
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveKbaConfig() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getKbaConfig(any(), any())).thenThrow(newResourceException(BAD_REQUEST));

        node.process(getContext(emptyList(), sharedState));
    }

    @Test
    public void callbacksAbsentShouldReturnCallbacks() throws Exception {
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

    @Test
    public void callbacksPresentProcessesSuccessfully() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
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
    public void invalidCallbacksGoToFalse() throws Exception {
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

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
