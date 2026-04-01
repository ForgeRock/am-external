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
 * Copyright 2019-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.upgrade.exception.NodeUpgradeException.UpgradeFailureReason.UNKNOWN_VERSION;
import static org.forgerock.openam.auth.node.api.upgrade.exception.NodeUpgradeException.UpgradeFailureReason.UPGRADE_FAILED;
import static org.forgerock.openam.auth.nodes.rest.AuthTreeNodeCollectionProvider.OUTCOMES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.NodeUpgrader;
import org.forgerock.api.enums.Stability;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.ConfigProviderEnabled;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.upgrade.exception.NodeUpgradeException;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderScriptGenerator;
import org.forgerock.openam.auth.trees.engine.AuthNodeFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.sms.OutcomeJsonConverter;
import org.forgerock.openam.core.rest.sms.SchemaGeneratorFactory;
import org.forgerock.openam.core.rest.sms.SmsResourceProviderContext;
import org.forgerock.openam.core.rest.sms.SmsSchemaGenerator;
import org.forgerock.openam.core.sms.DefaultConfigService;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.services.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.identity.authentication.config.AMAuthenticationManagerFactory;
import com.sun.identity.authentication.config.AuthenticationTypesCache;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;

@ExtendWith(MockitoExtension.class)
class AuthTreeNodeCollectionProviderTest {
    @Mock
    private ServiceSchema serviceSchema;
    @Mock
    private List<ServiceSchema> subSchemaPath;
    @Mock
    private SmsJsonConverter jsonConverter;
    @Mock
    private Context mockContext;
    @Mock
    private RealmContext mockRealmContext;
    @Mock
    private LocaleContext localeContext;
    @Mock
    private ServiceConfig mockServiceConfig;
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
    private Realm realm;
    @Mock
    private DefaultConfigService defaultConfigService;
    @Mock
    private SmsSchemaGenerator smsSchemaGenerator;
    @Mock
    private ConfigProviderScriptGenerator scriptGenerator;
    @Mock
    private NodeUpgrader nodeUpgrader;
    @Mock
    private NodeRegistry nodeRegistry;

    @BeforeEach
    void setup() throws Exception {
        given(serviceSchema.getName()).willReturn("MessageNode");
        given(serviceSchema.getServiceName()).willReturn("MessageNode");
        given(schemaGeneratorFactory.create(any(), any())).willReturn(smsSchemaGenerator);
        given(smsSchemaGenerator.createSchema(any())).willReturn(json(object(field("properties", object()))));
        given(jsonConverterFactory.create(any(), anyBoolean()))
                .willReturn(jsonConverter);

        given(serviceSchema.getName()).willReturn("Node");
        given(serviceSchema.getVersion()).willReturn("1.0");
    }

    private AuthTreeNodeCollectionProvider getAuthTreeNodeCollectionProvider(boolean isDeprecated) {
        var dependencies = new SmsResourceProviderContext(AMResourceBundleCache.getInstance(), Locale.UK,
                jsonConverterFactory, dataStoreLookup, amAuthenticationManagerFactory, authenticationTypesCache,
                schemaGeneratorFactory, null, null, defaultConfigService);
        return new AuthTreeNodeCollectionProvider(dependencies, serviceSchema, SchemaType.ORGANIZATION, serviceSchema,
                subSchemaPath, "", false, nodeFactory, outcomeJsonConverter, scriptGenerator,
                isDeprecated ? Stability.DEPRECATED : Stability.STABLE, nodeUpgrader, nodeRegistry);
    }

    @Test
    void getJsonShouldIncludeOutcomes() throws Exception {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        given(serviceSchema.getServiceType()).willReturn(SchemaType.ORGANIZATION);
        given(localeContext.getLocale()).willReturn(Locale.UK);
        given(mockContext.asContext(LocaleContext.class)).willReturn(localeContext);
        given(mockContext.asContext(RealmContext.class)).willReturn(mockRealmContext);
        given(mockRealmContext.getRealm()).willReturn(realm);
        given(nodeFactory.getOutcomeProvider(any(), any(), any())).willReturn(outcomeProvider);
        final OutcomeProvider.Outcome outcome = new OutcomeProvider.Outcome("outcome", "Out");
        given(outcomeProvider.getOutcomes(any(), any())).willReturn(Collections.singletonList(outcome));
        given(outcomeJsonConverter.toJson(any()))
                .willReturn(object(field("id", outcome.id), field("display", outcome.displayName)));

        // when
        final JsonValue outcomes = provider.getJsonValue("", "", mockServiceConfig, mockContext).get(OUTCOMES);

        // then
        assertThat(outcomes.isNotNull()).isTrue();
        assertThat(outcomes.get(0).get("id").asString()).isEqualTo(outcome.id);
    }

    @Test
    void shouldCreateEncodedScriptWhenCallingScriptAction() throws Exception {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        given(serviceSchema.getServiceType()).willReturn(SchemaType.ORGANIZATION);
        given(smsSchemaGenerator.createSchema(any())).willReturn(json(object(
                field("properties", object(
                        field("message", object(field("propertyOrder", 100))),
                        field("language", object(field("propertyOrder", 200))),
                        field("platform", object(field("propertyOrder", 300))
                        )
                )))));
        given(jsonConverter.toJson(isNull(), eq(false))).willReturn(json(object(
                field("message", object()),
                field("language", "en-GB")
        )));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("message", Map.of());
        properties.put("language", "en-GB");
        properties.put("platform", null);
        String expectedScript = """
                config = {
                  "message": { },
                  "language": "en-GB",
                  "platform": null
                };
                """;
        given(scriptGenerator.generateScript(properties, ScriptingLanguage.JAVASCRIPT)).willReturn(expectedScript);
        given(nodeRegistry.getConfigType(any(), any())).willAnswer(inv -> Object.class);
        var request = Requests.newActionRequest("test", "script");
        request.setContent(json(object(field("language", "JAVASCRIPT"))));

        // when
        var content = provider.configProviderScript(mockContext, request).getOrThrow().getJsonContent();

        // then
        assertThat(content).isNotNull();
        assertThat(content.keys()).containsExactly("script");
        String script = Base64.decodeAsUTF8String(content.get("script").asString());
        assertThat(script).containsIgnoringWhitespaces(expectedScript);
    }

    @Test
    void shouldUseJavaScriptWhenNoLanguageProvided() throws Exception {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        given(serviceSchema.getServiceType()).willReturn(SchemaType.ORGANIZATION);
        given(smsSchemaGenerator.createSchema(any())).willReturn(json(object(
                field("properties", object(field("message", object(field("propertyOrder", 100))))))));
        given(jsonConverter.toJson(isNull(), eq(false))).willReturn(json(object(field("message", object()))));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("message", Map.of());
        String expectedScript = """
                config = {
                  "message": { }
                };
                """;
        given(scriptGenerator.generateScript(properties, ScriptingLanguage.JAVASCRIPT)).willReturn(expectedScript);
        given(nodeRegistry.getConfigType(any(), any())).willAnswer(inv -> Object.class);
        var request = Requests.newActionRequest("test", "script");

        // when
        var content = provider.configProviderScript(mockContext, request).getOrThrow().getJsonContent();

        // then
        assertThat(content).isNotNull();
        assertThat(content.keys()).containsExactly("script");
        String script = Base64.decodeAsUTF8String(content.get("script").asString());
        assertThat(script).containsIgnoringWhitespaces(expectedScript);
    }

    @Test
    void shouldCreateEmptyScriptWhenSchemaHasNoProperties() throws Exception {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        given(serviceSchema.getServiceType()).willReturn(SchemaType.ORGANIZATION);
        given(smsSchemaGenerator.createSchema(any())).willReturn(json(object(field("properties", null))));
        given(jsonConverter.toJson(isNull(), eq(false))).willReturn(json(object()));
        String expectedScript = """
                config = { };
                """;
        given(scriptGenerator.generateScript(Map.of(), ScriptingLanguage.JAVASCRIPT)).willReturn(expectedScript);
        given(nodeRegistry.getConfigType(any(), any())).willAnswer(inv -> Object.class);
        var request = Requests.newActionRequest("test", "script");

        // when
        var content = provider.configProviderScript(mockContext, request).getOrThrow().getJsonContent();

        // then
        assertThat(content).isNotNull();
        assertThat(content.keys()).containsExactly("script");
        String script = Base64.decodeAsUTF8String(content.get("script").asString());
        assertThat(script).containsIgnoringWhitespaces(expectedScript);
    }

    @Test
    void shouldRemoveStaticConfig() throws Exception {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        given(serviceSchema.getServiceType()).willReturn(SchemaType.ORGANIZATION);
        given(smsSchemaGenerator.createSchema(any())).willReturn(json(object(
                field("properties", object(
                        field("message", object(field("propertyOrder", 100))),
                        field("outcomes", object(field("propertyOrder", 200)))
                        )))));
        given(jsonConverter.toJson(isNull(), eq(false))).willReturn(json(object(field("message", object()))));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("message", Map.of());
        String expectedScript = """
                config = {
                  "message": { }
                };
                """;
        given(scriptGenerator.generateScript(properties, ScriptingLanguage.JAVASCRIPT)).willReturn(expectedScript);
        given(nodeRegistry.getConfigType(any(), any())).willAnswer(inv -> TestConfig.class);
        var request = Requests.newActionRequest("test", "script");

        // when
        var content = provider.configProviderScript(mockContext, request).getOrThrow().getJsonContent();

        // then
        assertThat(content).isNotNull();
        assertThat(content.keys()).containsExactly("script");
        String script = Base64.decodeAsUTF8String(content.get("script").asString());
        assertThat(script).containsIgnoringWhitespaces(expectedScript);
    }

    @Test
    void shouldReturnBadRequestWhenIllegalLanguageInput() {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "script");
        request.setContent(json(object(field("language", "INVALID"))));

        // when / then
        assertThatThrownBy(() -> provider.configProviderScript(mockContext, request).getOrThrow())
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Invalid language");
    }

    @Test
    void shouldReturnInternalServerErrorWhenScriptGenerationFails() throws JsonProcessingException, ScriptException {
        // given
        var provider = getAuthTreeNodeCollectionProvider(false);
        given(serviceSchema.getServiceType()).willReturn(SchemaType.ORGANIZATION);
        var request = Requests.newActionRequest("test", "script");
        request.setContent(json(object(field("language", "JAVASCRIPT"))));
        given(scriptGenerator.generateScript(any(), any())).willThrow(JsonProcessingException.class);
        given(nodeRegistry.getConfigType(any(), any())).willAnswer(inv -> Object.class);

        // when / then
        assertThatThrownBy(() -> provider.configProviderScript(mockContext, request).getOrThrow())
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Failed to generate script");
    }

    @Test
    void shouldCreateDeprecatedResourcesIfDeprecated() {
        // Given
        var provider = getAuthTreeNodeCollectionProvider(true);

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
        var provider = getAuthTreeNodeCollectionProvider(false);

        // When
        var resource = provider.getResource();

        // Then
        assertThat(resource).isNotNull();
        Arrays.stream(resource.getActions())
                .forEach(action -> assertThat(action.getStability()).isEqualTo(Stability.STABLE));
        assertThat(resource.getItems().getCreate().getStability()).isEqualTo(Stability.STABLE);
    }

    @Test
    void getUpgradedShouldReturnBadRequestWhenNoTargetVersionProvided() {
        // given
        var collectionProvider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "getUpgraded");

        // when
        var response = collectionProvider.getUpgradedConfig(mockContext, request);

        // then
        assertThatThrownBy(response::getOrThrowIfInterrupted).isInstanceOfSatisfying(BadRequestException.class, ex -> {
            assertThat(ex.getMessage()).contains("targetVersion must be supplied");
        });
    }

    @Test
    void getUpgradedShouldReturnBadRequestWhenInvalidTargetVersionProvided() throws BadRequestException {
        // given
        var collectionProvider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "getUpgraded");
        request.setAdditionalParameter("targetVersion", "invalid-version");

        // when
        var response = collectionProvider.getUpgradedConfig(mockContext, request);

        // then
        assertThatThrownBy(response::getOrThrowIfInterrupted).isInstanceOfSatisfying(BadRequestException.class, ex -> {
            assertThat(ex.getMessage()).contains("targetVersion must be a valid version");
        });
    }

    @Test
    void getUpgradedShouldReturnBadRequestWhenTargetVersionTooLow() throws BadRequestException {
        // given
        var collectionProvider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "getUpgraded");
        request.setAdditionalParameter("targetVersion", "0.0");

        // when
        var response = collectionProvider.getUpgradedConfig(mockContext, request);

        // then
        assertThatThrownBy(response::getOrThrowIfInterrupted).isInstanceOfSatisfying(BadRequestException.class, ex -> {
            assertThat(ex.getMessage()).contains("targetVersion must not be less than current version");
        });
    }

    @Test
    void getUpgradedShouldReturnBadRequestWhenUnknownVersion() throws Exception {
        // given
        var collectionProvider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "getUpgraded");
        request.setAdditionalParameter("targetVersion", "2.0");
        given(nodeUpgrader.upgrade(any(), anyInt(), anyInt(), any()))
                .willThrow(new NodeUpgradeException(UNKNOWN_VERSION));

        // when
        var response = collectionProvider.getUpgradedConfig(mockContext, request);

        // then
        assertThatThrownBy(response::getOrThrowIfInterrupted).isInstanceOfSatisfying(BadRequestException.class, ex -> {
            assertThat(ex.getMessage()).contains("targetVersion not upgradable");
        });
    }

    @Test
    void getUpgradedShouldReturnInternalServerErrorWhenUpgradeFails() throws Exception {
        // given
        var collectionProvider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "getUpgraded");
        request.setAdditionalParameter("targetVersion", "2.0");
        given(nodeUpgrader.upgrade(any(), anyInt(), anyInt(), any()))
                .willThrow(new NodeUpgradeException(UPGRADE_FAILED));

        // when
        var response = collectionProvider.getUpgradedConfig(mockContext, request);

        // then
        assertThatThrownBy(response::getOrThrowIfInterrupted)
                .isInstanceOfSatisfying(InternalServerErrorException.class,
                        ex -> assertThat(ex.getMessage()).contains("Failed to upgrade node config"));
    }

    @Test
    void getUpgradedShouldReturnNewActionResponseWhenUpgradeSuccessful() throws Exception {
        // given
        var collectionProvider = getAuthTreeNodeCollectionProvider(false);
        var request = Requests.newActionRequest("test", "getUpgraded");
        request.setAdditionalParameter("targetVersion", "2.0");
        given(nodeUpgrader.upgrade(any(), anyInt(), anyInt(), any())).willReturn(json(object(field("upgraded", true))));

        // when
        var response = collectionProvider.getUpgradedConfig(mockContext, request);

        // then
        assertThat(response.getOrThrowIfInterrupted().getJsonContent().get("upgraded").asBoolean()).isTrue();
    }

    private interface TestConfig {

        @Attribute(order =  100)
        Map<String, Object> message();

        @Attribute(order = 200)
        @ConfigProviderEnabled.StaticConfig
        List<String> outcomes();
    }
}
