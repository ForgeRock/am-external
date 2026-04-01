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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.sm.DefaultAnnotatedServiceRegistry.INSTANCE_SUB_SCHEMA_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.NodeUpgrader;
import org.forgerock.api.enums.Stability;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.ConfigProviderEnabled;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.SchemaGeneratorFactory;
import org.forgerock.openam.core.rest.sms.SmsResourceProviderContext;
import org.forgerock.openam.core.rest.sms.SmsSchemaGenerator;
import org.forgerock.openam.core.sms.DefaultConfigService;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.config.AuthenticationTypesCache;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

@ExtendWith(MockitoExtension.class)
class ConfigProviderNodeCollectionProviderTest {

    @Mock
    private ServiceSchema schema;
    @Mock
    private SchemaGeneratorFactory schemaGeneratorFactory;
    @Mock
    private SmsSchemaGenerator schemaGenerator;
    @Mock
    private SmsJsonConverterFactory converterFactory;
    @Mock
    private SmsJsonConverter jsonConverter;
    @Mock
    private AuthenticationTypesCache cache;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private NodeUpgrader nodeUpgrader;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private ServiceSchemaManager serviceSchemaManager;
    @Mock
    private ServiceSchema serviceSchema;
    @Mock
    private DefaultConfigService defaultConfigService;

    @BeforeEach
    void setup() {
        given(schema.getName()).willReturn("ConfigProviderNode");
        given(schema.getAttributeSchemaNames()).willReturn(Set.of("nodeType", "nodeVersion", "staticConfig"));
        given(schemaGeneratorFactory.create(any(), eq(SchemaType.ORGANIZATION))).willReturn(schemaGenerator);
        given(schemaGenerator.createSchema(Optional.empty()))
                .willAnswer(inv -> json(object(
                        field("properties", object(
                                field("nodeVersion", object(field("required", true))),
                                field("nodeType", object(field("required", true)))
                        ))
                )));
    }

    private ConfigProviderNodeCollectionProvider getCollectionProvider(boolean isDeprecated) {
        var dependencies = new SmsResourceProviderContext(null, null, converterFactory, null, null, cache,
                schemaGeneratorFactory, null, null, defaultConfigService);
        return new ConfigProviderNodeCollectionProvider(dependencies, schema, schema, "", null, null,
                schemaGeneratorFactory, null, nodeRegistry,
                isDeprecated ? Stability.DEPRECATED : Stability.STABLE, nodeUpgrader, coreWrapper);
    }

    @Test
    void getAttributesFromContentShouldNotAddVersion() throws Exception {
        // Given
        var collectionProvider = getCollectionProvider(false);
        given(nodeRegistry.getConfigType(any(), any()))
                .willAnswer(inv -> Object.class);
        var content = json(object(
                field("nodeType", "testNode")
        ));

        // When
        collectionProvider.getAttributesFromContent("realm", content, null, jsonConverter);

        // Then
        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(jsonConverter).fromJson(eq("realm"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue().asMap()).doesNotContainKey("nodeVersion");
    }


    @Test
    void shouldCreateDeprecatedResourcesIfDeprecated() {
        // Given
        var collectionProvider = getCollectionProvider(true);

        // When
        var resource = collectionProvider.getResource();

        // Then
        assertThat(resource).isNotNull();
        Arrays.stream(resource.getActions())
                .forEach(action -> assertThat(action.getStability()).isEqualTo(Stability.DEPRECATED));
        assertThat(resource.getItems().getCreate().getStability()).isEqualTo(Stability.DEPRECATED);

    }

    @Test
    void shouldCreateStableResourcesIfNotDeprecated() {
        // Given
        var collectionProvider = getCollectionProvider(false);

        // When
        var resource = collectionProvider.getResource();

        // Then
        assertThat(resource).isNotNull();
        Arrays.stream(resource.getActions())
                .forEach(action -> assertThat(action.getStability()).isEqualTo(Stability.STABLE));
        assertThat(resource.getItems().getCreate().getStability()).isEqualTo(Stability.STABLE);
    }
    @Test
    void getAttributesFromContentShouldRemoveUnknownConfigAttributes() throws Exception {
        // Given
        var collectionProvider = getCollectionProvider(false);
        var content = json(object(
                field("_id", "testId"),
                field("nodeType", "testNode"),
                field("unknownAttribute", "unknownValue")
        ));
        given(nodeRegistry.getConfigType(any(), any()))
                .willAnswer(inv -> Object.class);

        // When
        collectionProvider.getAttributesFromContent("realm", content, null, jsonConverter);

        // Then
        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(jsonConverter).fromJson(eq("realm"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue().asMap()).containsEntry("nodeType", "testNode");
        assertThat(jsonCaptor.getValue().asMap()).doesNotContainKey("unknownAttribute");
        assertThat(jsonCaptor.getValue().asMap()).containsKey("_id");
        assertThat(jsonCaptor.getValue().asMap()).containsKey("staticConfig");
    }

    @Test
    void getAttributesFromContentShouldMoveStaticConfigToStaticConfigObject() throws Exception {
        // Given
        var collectionProvider = getCollectionProvider(false);
        var content = json(object(
                field("_id", "testId"),
                field("nodeType", "testNode"),
                field("attribute1", "value1"),
                field("attribute2", "value2")
        ));
        given(nodeRegistry.getConfigType(any(), any()))
                .willAnswer(inv -> TestConfig.class);
        given(coreWrapper.getServiceSchemaManager(any(), any())).willReturn(serviceSchemaManager);
        given(serviceSchemaManager.getOrganizationSchema()).willReturn(serviceSchema);
        given(serviceSchema.getSubSchema(INSTANCE_SUB_SCHEMA_NAME)).willReturn(serviceSchema);
        var config = ConfigurationAttributesFactory.create();
        given(defaultConfigService.getDefaultsForService(serviceSchema))
                .willReturn(config);
        given(converterFactory.create(serviceSchema, false)).willReturn(jsonConverter);
        given(jsonConverter.toJson(config, false)).willReturn(json(object(field("attribute3", 3))));


        // When
        collectionProvider.getAttributesFromContent("realm", content, null, jsonConverter);

        // Then
        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(jsonConverter).fromJson(eq("realm"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue().asMap()).containsEntry("nodeType", "testNode");
        assertThat(jsonCaptor.getValue().asMap()).doesNotContainKeys("attribute1", "attribute2", "attribute3");
        assertThat(jsonCaptor.getValue().asMap()).containsKey("_id");
        assertThat(jsonCaptor.getValue().asMap()).containsKey("staticConfig");
        var staticConfig = jsonCaptor.getValue().get("staticConfig");
        assertThat(staticConfig.asMap()).containsEntry("attribute2", "value2");
        assertThat(staticConfig.asMap()).containsEntry("attribute3", 3);
        assertThat(staticConfig.asMap()).doesNotContainKey("attribute1");
    }

    interface TestConfig {

        @Attribute(order = 100)
        String attribute1();

        @Attribute(order = 200)
        @ConfigProviderEnabled.StaticConfig
        String attribute2();

        @Attribute(order = 300)
        @ConfigProviderEnabled.StaticConfig
        Integer attribute3();
    }

}
