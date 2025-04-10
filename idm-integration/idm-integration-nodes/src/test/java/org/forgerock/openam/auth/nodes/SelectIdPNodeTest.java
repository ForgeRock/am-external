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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.LOCAL_AUTHENTICATION;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
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
    @InjectMocks
    private SelectIdPNode node;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(config.includeLocalAuthentication()).thenReturn(false);
        lenient().when(config.filteredProviders()).thenReturn(Collections.emptySet());

        providers = new JsonValue(OBJECT_MAPPER.readValue(
                SelectIdPNodeTest.class.getResource("/SelectIdpNode/providers.json"), List.class));
    }

    @Test
    void shouldReturnCallbackOnFirstProcess() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        SelectIdPCallback callback = (SelectIdPCallback) action.callbacks.get(0);
        assertThat(callback.getProviders().size()).isGreaterThan(1);
    }

    @Test
    void shouldRecordSelectedProvider() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
        SelectIdPCallback callback = new SelectIdPCallback(providers);
        callback.setProvider("google");
        TreeContext context = getContext(singletonList(callback), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isEmpty();
        assertThat(action.sharedState.get(SELECTED_IDP).asString()).isEqualTo("google");
    }

    @Test
    void shouldRejectUnknownSelectedProvider() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
        SelectIdPCallback callback = new SelectIdPCallback(providers);
        callback.setProvider("unknown");
        TreeContext context = getContext(singletonList(callback), json(object()));
        assertThatThrownBy(() -> node.process(context)).isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldIncludeLocalAuthIfEnabled() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
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
    void shouldExcludeLocalAuthIfNotEnabled() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
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
    void shouldAuthAdvanceIfOnlyOneProviderAvailable() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(true);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(emptyMap());

        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        assertThat(action.callbacks).isEmpty();
        assertThat(action.sharedState.get(SELECTED_IDP).asString()).isEqualTo(LOCAL_AUTHENTICATION);
    }

    @Test
    void shouldThrowIfNoProvidersAvailable() throws Exception {
        when(config.includeLocalAuthentication()).thenReturn(false);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(emptyMap());

        TreeContext context = getContext(emptyList(), json(object()));
        assertThatThrownBy(() -> node.process(context)).isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldLimitChoicesToThoseFoundInExistingObject() throws Exception {
        // given
        when(config.offerOnlyExisting()).thenReturn(false);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
        when(config.includeLocalAuthentication()).thenReturn(true);
        when(config.offerOnlyExisting()).thenReturn(true);
        when(idmIntegrationService.isEnabled()).thenReturn(true);
        when(idmIntegrationService.getObjectIdentityProviders(any(), any(), any(), any(), any(), any()))
                .thenReturn(asList(LOCAL_AUTHENTICATION, "google"));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenReturn(Optional.of(json("identity")));

        // when
        TreeContext context = getContext(emptyList(), json(object()));
        Action action = node.process(context);

        // then
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
    void shouldLimitToThoseDefined() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
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
    void shouldLimitToThoseDefinedWithLocalAuthenticator() throws Exception {
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(providers.stream()
                .map(this::jsonToIdPConfig)
                .collect(Collectors.toMap(OAuthClientConfig::provider, provider -> provider)));
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

    @Test
    void shouldIncludeAllOutcomesWhenGetAllOutcomes() {
        // given
        SelectIdPNode.SelectIdPNodeOutcomeProvider outcomeProvider = new SelectIdPNode.SelectIdPNodeOutcomeProvider();

        // when
        var outcomes = outcomeProvider.getAllOutcomes(new PreferredLocales());
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly("socialAuthentication", "localAuthentication");
    }

    @Test
    void shouldIncludeLocalAuthenticationOutcomeWhenIncludeLocationAuthenticationIsMissing() {
        // given
        SelectIdPNode.SelectIdPNodeOutcomeProvider outcomeProvider = new SelectIdPNode.SelectIdPNodeOutcomeProvider();
        JsonValue attributes = json(object());

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly("socialAuthentication", "localAuthentication");
    }

    @Test
    void shouldIncludeLocalAuthenticationOutcomeWhenIncludeLocationAuthenticationIsTrue() {
        // given
        SelectIdPNode.SelectIdPNodeOutcomeProvider outcomeProvider = new SelectIdPNode.SelectIdPNodeOutcomeProvider();
        JsonValue attributes = json(object(field("includeLocalAuthentication", true)));

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly("socialAuthentication", "localAuthentication");
    }

    @Test
    void shouldNotIncludeLocalAuthenticationOutcomeWhenIncludeLocationAuthenticationIsFalse() {
        // given
        SelectIdPNode.SelectIdPNodeOutcomeProvider outcomeProvider = new SelectIdPNode.SelectIdPNodeOutcomeProvider();
        JsonValue attributes = json(object(field("includeLocalAuthentication", false)));

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly("socialAuthentication");
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
            public Script transform() {
                return null;
            }
        };
    }
}
