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
 * Copyright 2019-2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.LOCAL_AUTHENTICATION;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.SelectIdPCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

public class SelectIdPNodeTest {

    @Mock
    SocialIdentityProviders providerConfigStore;

    @Mock
    SelectIdPNode.Config config;

    @Mock
    Realm realm;

    @Mock
    IdmIntegrationService idmIntegrationService;

    private JsonValue providers;
    private SelectIdPNode node;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);

        when(config.includeLocalAuthentication()).thenReturn(false);
        when(config.offerOnlyExisting()).thenReturn(false);
        when(config.filteredProviders()).thenReturn(Collections.emptySet());

        providers = new JsonValue(OBJECT_MAPPER.readValue(
                SelectIdPNodeTest.class.getResource("/SelectIdpNode/providers.json"), List.class));
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
        node = new SelectIdPNode(config, realm, providerConfigStore, idmIntegrationService);
    }

    @Test
    public void shouldReturnCallbackOnFirstProcess() throws Exception {
        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().size()).isGreaterThan(1);
    }

    @Test
    public void shouldRecordSelectedProvider() throws Exception {
        SelectIdPCallback callback = new SelectIdPCallback(providers);
        callback.setProvider("google");
        TreeContext context = getContext(singletonList(callback), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isEmpty();
        assertThat(action.sharedState.get(SELECTED_IDP).asString()).isEqualTo("google");
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldRejectUnknownSelectedProvider() throws Exception {
        SelectIdPCallback callback = new SelectIdPCallback(providers);
        callback.setProvider("unknown");
        TreeContext context = getContext(singletonList(callback), json(object()));
        node.process(context);
    }

    @Test
    public void shouldIncludeLocalAuthIfEnabled() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(true);

        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().stream()
                .anyMatch(provider -> provider.get("provider").asString().equals("localAuthentication"))).isTrue();
    }

    @Test
    public void shouldExcludeLocalAuthIfNotEnabled() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(false);

        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().stream()
                .anyMatch(provider -> provider.get("provider").asString().equals("localAuthentication"))).isFalse();
    }

    @Test
    public void shouldAuthAdvanceIfOnlyOneProviderAvailable() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(true);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(emptyMap());

        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isEmpty();
        assertThat(action.sharedState.get(SELECTED_IDP).asString()).isEqualTo(LOCAL_AUTHENTICATION);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowIfNoProvidersAvailable() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(false);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(emptyMap());

        TreeContext context = getContext(emptyList(), json(object()));
        node.process(context);
    }

    @Test
    public void shouldLimitChoicesToThoseFoundInExistingObject() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(true);
        when(config.offerOnlyExisting()).thenReturn(true);
        when(idmIntegrationService.isEnabled()).thenReturn(true);
        when(idmIntegrationService.getObjectIdentityProviders(any(), any(), any(), any(), any(), any()))
                .thenReturn(asList(LOCAL_AUTHENTICATION, "google"));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenReturn(Optional.of(json("identity")));

        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().asList().size()).isEqualTo(2);
        assertThat(callback.getProviders().stream()
                .anyMatch(provider -> provider.get("provider").asString().equals("localAuthentication"))).isTrue();
        assertThat(callback.getProviders().stream()
                .anyMatch(provider -> provider.get("provider").asString().equals("google"))).isTrue();
    }

    @Test
    public void shouldLimitToThoseDefined() throws Exception {
        when(config.filteredProviders()).thenReturn(ImmutableSet.of("google", "facebook"));
        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().size()).isEqualTo(2);
        assertThat(callback.getProviders().get(0).get("provider").asString()).isEqualTo("facebook");
        assertThat(callback.getProviders().get(1).get("provider").asString()).isEqualTo("google");
    }

    @Test
    public void shouldLimitToThoseDefinedWithLocalAuthenticator() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(true);
        when(config.filteredProviders()).thenReturn(ImmutableSet.of("google", "facebook"));
        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().size()).isEqualTo(3);
        assertThat(callback.getProviders().get(0).get("provider").asString()).isEqualTo("facebook");
        assertThat(callback.getProviders().get(1).get("provider").asString()).isEqualTo("google");
        assertThat(callback.getProviders().get(2).get("provider").asString()).isEqualTo("localAuthentication");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private OAuthClientConfig jsonToIdPConfig(JsonValue provider) {
        return new OAuthClientConfig() {
            @Override
            public String provider() {
                return provider.get("provider").asString();
            }

            @Override
            public String authenticationIdKey() {
                return provider.get("authenticationIdKey").asString();
            }

            @Override
            public Map<String, String> uiConfig() {
                return provider.get("uiConfig").asMap(String.class);
            }

            @Override
            public ScriptConfiguration transform() {
                return null;
            }
        };
    }
}
