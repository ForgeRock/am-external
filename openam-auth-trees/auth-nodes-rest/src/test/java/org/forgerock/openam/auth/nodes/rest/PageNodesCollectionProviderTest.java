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
 * Copyright 2024-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.NodeUpgrader;
import org.forgerock.api.enums.Stability;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderScriptGenerator;
import org.forgerock.openam.auth.trees.engine.AuthNodeFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.core.rest.sms.OutcomeJsonConverter;
import org.forgerock.openam.core.rest.sms.SchemaGeneratorFactory;
import org.forgerock.openam.core.rest.sms.SmsResourceProviderContext;
import org.forgerock.openam.core.sms.DefaultConfigService;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.ServiceSchemaManagerFactory;
import org.forgerock.services.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManagerFactory;
import com.sun.identity.authentication.config.AuthenticationTypesCache;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class PageNodesCollectionProviderTest {

    @RegisterExtension
    public static RealmTestHelper.JUnitRealmRule realmRule
            = new RealmTestHelper.JUnitRealmRule(PageNodesCollectionProviderTest.class);

    private static final String PAGE_NODE_ID = UUID.randomUUID().toString();

    @Mock
    private ServiceSchema serviceSchema;
    @Mock
    private SmsJsonConverter jsonConverter;
    @Mock
    private LocaleContext localeContext;
    @Mock
    private DataStoreLookup dataStoreLookup;
    @Mock
    private SmsJsonConverterFactory jsonConverterFactory;
    @Mock
    private AuthNodeFactory nodeFactory;
    @Mock
    private OutcomeJsonConverter outcomeJsonConverter;
    @Mock
    private OutcomeProvider outcomeProvider;
    @Mock
    private AuthenticationTypesCache authenticationTypesCache;
    @Mock
    private AMAuthenticationManagerFactory amAuthenticationManagerFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SchemaGeneratorFactory schemaGeneratorFactory;
    @Mock
    Context context;
    @Mock
    private ServiceConfigManagerFactory serviceConfigManagerFactory;
    @Mock
    private ServiceSchemaManagerFactory serviceSchemaManagerFactory;
    @Mock
    private ServiceConfigManager serviceConfigManager;
    @Mock
    private ServiceSchemaManager serviceSchemaManager;
    @Mock
    private PageNodeCollectionProviderUtils pageNodeCollectionProviderUtils;

    @RealmTestHelper.RealmHelper
    static Realm realm;

    @Mock
    private ServiceConfig parentConfig;
    @Mock
    private ServiceConfig pageNodeConfig;
    @Mock
    private DefaultConfigService defaultConfigService;
    @Mock
    private ConfigProviderScriptGenerator scriptGenerator;
    @Mock
    private NodeUpgrader nodeUpgrader;
    @Mock
    private NodeRegistry nodeRegistry;

    @BeforeEach
    void setUp() throws SMSException, SSOException, NodeProcessException {
        when(serviceSchema.getName()).thenReturn("PageNode");
        when(serviceSchema.getServiceName()).thenReturn("PageNode");
        when(serviceSchema.getVersion()).thenReturn("1.0");
        when(serviceSchema.getServiceType()).thenReturn(SchemaType.ORGANIZATION);
        when(serviceSchema.getResourceName()).thenReturn("USE-PARENT");

        final RealmContext realmContext = mock(RealmContext.class);
        when(realmContext.getRealm()).thenReturn(realm);
        when(context.containsContext(RealmContext.class)).thenReturn(true);
        when(context.asContext(RealmContext.class)).thenReturn(realmContext);
        when(localeContext.getLocale()).thenReturn(Locale.ENGLISH);
        when(context.asContext(LocaleContext.class)).thenReturn(localeContext);
        when(context.asContext(UriRouterContext.class)).thenReturn(UriRouterContext.uriRouterContext(context).build());

        SSOToken token = mock(SSOToken.class);
        final SSOTokenContext tokenContext = mock(SSOTokenContext.class);
        when(tokenContext.getCallerSSOToken()).thenReturn(token);
        when(context.asContext(SSOTokenContext.class)).thenReturn(tokenContext);

        when(dataStoreLookup.lookupRealmId("PageNode", realm)).thenReturn(DataStoreId.CONFIG);

        when(pageNodeConfig.exists()).thenReturn(true);
        when(pageNodeConfig.getSchemaID()).thenReturn("PageNode");

        when(parentConfig.exists()).thenReturn(true);
        when(parentConfig.getSubConfigNames()).thenReturn(Set.of(PAGE_NODE_ID));
        when(parentConfig.getSubConfig(PAGE_NODE_ID)).thenReturn(pageNodeConfig);

        when(serviceConfigManagerFactory.create("PageNode", "1.0", token))
                .thenReturn(serviceConfigManager);
        when(serviceConfigManager.getOrganizationConfig(realm.asPath(), null, DataStoreId.CONFIG))
                .thenReturn(parentConfig);

        when(serviceSchemaManager.getResourceName()).thenReturn("PageNode");
        when(serviceSchemaManagerFactory.build("PageNode", "1.0")).thenReturn(serviceSchemaManager);

        when(jsonConverter.toJson(eq(realm.asPath()), eq(null), eq(false), isA(JsonValue.class)))
                .then(invocation -> {
                    ((JsonValue) invocation.getArgument(3)).put("nodes", List.of(
                            "44f683a8-4163-3352-bafe-57c2e008bc8c:ValidatedPasswordNode:1:Platform Password"));
                    return null;
                });
        when(jsonConverterFactory.create(serviceSchema, false)).thenReturn(jsonConverter);

        when(nodeFactory.getOutcomeProvider(realm, "PageNode", 1)).thenReturn(outcomeProvider);
    }

    @Test
    void shouldDeleteContainedNodes() throws Exception {
        // given
        given(pageNodeCollectionProviderUtils.getJsonValue(any()))
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field("foo", "bar"))));

        ArgumentCaptor<ResourceResponse> captor = ArgumentCaptor.forClass(ResourceResponse.class);
        when(pageNodeCollectionProviderUtils.deleteNodes(any(), captor.capture()))
                .thenReturn(Responses.newResourceResponse("", "", JsonValue.json(JsonValue.object())));

        final PageNodesCollectionProvider subject = getCollectionProvider(false);

        // when
        subject.deleteInstance(context, PAGE_NODE_ID);

        // then
        verify(pageNodeCollectionProviderUtils).deleteNodes(any(), any());
        final ResourceResponse response = captor.getValue();
        assertThat(response).isNotNull();
        assertThat(response.getContent().get("foo").asString()).isEqualTo("bar");
    }

    private PageNodesCollectionProvider getCollectionProvider(boolean isDeprecated) {
        var dependencies = new SmsResourceProviderContext(AMResourceBundleCache.getInstance(),
                Locale.getDefault(), jsonConverterFactory, dataStoreLookup, amAuthenticationManagerFactory,
                authenticationTypesCache, schemaGeneratorFactory, serviceConfigManagerFactory,
                serviceSchemaManagerFactory, defaultConfigService);

        return new PageNodesCollectionProvider(dependencies, serviceSchema, serviceSchema,
                nodeFactory, outcomeJsonConverter, scriptGenerator, pageNodeCollectionProviderUtils,
                isDeprecated ? Stability.DEPRECATED : Stability.STABLE, nodeUpgrader, nodeRegistry);
    }

    @Test
    void shouldCreateDeprecatedResourcesIfDeprecated() {
        // Given
        var provider = getCollectionProvider(true);

        // When
        var resource = provider.getResource();

        // Then
        assertThat(resource).isNotNull();
        Arrays.stream(resource.getActions())
                .forEach(action -> assertThat(action.getStability()).isEqualTo(Stability.DEPRECATED));
        assertThat(resource.getItems().getCreate().getStability()).isEqualTo(Stability.DEPRECATED);
    }

    @Test
    void shouldCreateStableResourcesIfNotDeprecated() {
        // Given
        var provider = getCollectionProvider(false);

        // When
        var resource = provider.getResource();

        // Then
        assertThat(resource).isNotNull();
        Arrays.stream(resource.getActions())
                .forEach(action -> assertThat(action.getStability()).isEqualTo(Stability.STABLE));
        assertThat(resource.getItems().getCreate().getStability()).isEqualTo(Stability.STABLE);
    }
}
