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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.am.trees.api.TreeBuilder.NODES;
import static org.forgerock.am.trees.api.TreeBuilder.NODE_TYPE;
import static org.forgerock.am.trees.api.TreeBuilder.VERSION;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

@ExtendWith(MockitoExtension.class)
public class NodeConfigValidatorTest {

    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private SmsJsonConverterFactory jsonConverterFactory;
    @Mock
    private ServiceSchemaManager serviceSchemaManager;
    @Mock
    private ServiceSchema serviceSchema;
    @Mock
    private SmsJsonConverter smsJsonConverter;
    @Mock
    private SSOToken adminToken;
    @Mock
    private ServiceConfigValidator serviceConfigValidator;

    private NodeConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NodeConfigValidator(coreWrapper, jsonConverterFactory);
    }

    private void configureAdminTokenStubbing() {
        when(coreWrapper.getAdminToken()).thenReturn(adminToken);
    }

    @Test
    void shouldValidateValidNode() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "1.0");
        when(smsJsonConverter.fromJson(any())).thenReturn(ConfigurationAttributesFactory.create());
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldHandleBadRequestException() throws Exception {
        configureAdminTokenStubbing();

        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "1.0");
        String errorMessage = "Bad config";
        when(smsJsonConverter.fromJson(any())).thenThrow(new BadRequestException(errorMessage));

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).message()).contains("Node 'node1' configuration is invalid. " + errorMessage);
        assertThat(failures.get(0).severity()).isEqualTo(ERROR);
    }

    @Test
    void shouldHandleInternalServerErrorExceptionWithGenericMessage() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "1.0");
        when(smsJsonConverter.fromJson(any())).thenThrow(new InternalServerErrorException("Detailed internal error"));

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).message()).isEqualTo((
                "Node '%s' configuration is invalid. An error occurred during validation.").formatted(nodeId));
        assertThat(failures.get(0).severity()).isEqualTo(ERROR);
    }

    @Test
    void shouldHandleUnrecognizedNodeType() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "UnknownNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        when(coreWrapper.getServiceSchemaManager(anyString(), anyString(), any()))
                .thenThrow(new SMSException("Unknown"));

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).message()).contains((
                "Node '%s' configuration is invalid. 'nodeType' '%s' with 'version' '1.0' is not recognized."
        ).formatted(nodeId, nodeType));
    }

    @Test
    void shouldUseDefaultVersionWhenMissing() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        // No version specified
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "1.0");
        when(smsJsonConverter.fromJson(any())).thenReturn(ConfigurationAttributesFactory.create());
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // When
        validator.validate(tree, null);

        // Then
        // Verify that "1.0" was used as the default version
        org.mockito.Mockito.verify(coreWrapper).getServiceSchemaManager(eq(nodeType), eq("1.0"), any());
    }

    @Test
    void shouldValidatePageNode() throws Exception {
        // Given
        String pageNodeId = "pageNode1";
        String innerNodeId = "innerNode1";
        String innerNodeType = "InnerTestNode";

        JsonValue innerNode = json(object(
                field("_id", innerNodeId),
                field(NODE_TYPE, innerNodeType),
                field("config", object())));

        JsonValue pageNode = json(object(
                field("_id", pageNodeId),
                field(NODE_TYPE, "PageNode"),
                field("config", object(field(NODES, json(List.of(innerNode)))))));

        JsonValue tree = json(object(field(NODES, json(List.of(pageNode)))));

        mockServiceSchema(innerNodeType, "1.0");
        when(smsJsonConverter.fromJson(any())).thenReturn(ConfigurationAttributesFactory.create());
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldSkipPageChildNodeValidationIfNoConfig() throws Exception {
        // Given
        String pageNodeId = "pageNode1";
        String innerNodeId = "innerNode1";
        String innerNodeType = "InnerTestNode";

        JsonValue innerNode = json(object(
                field("_id", innerNodeId),
                field(NODE_TYPE, innerNodeType)));

        JsonValue pageNode = json(object(
                field("_id", pageNodeId),
                field(NODE_TYPE, "PageNode"),
                field("config", object(field(NODES, json(List.of(innerNode)))))));

        JsonValue tree = json(object(field(NODES, json(List.of(pageNode)))));

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldUseExplicitNumericVersion() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        int version = 2;
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field(VERSION, version),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "2.0"); // Expecting "2.0" because of the integer + ".0" logic
        when(smsJsonConverter.fromJson(any())).thenReturn(ConfigurationAttributesFactory.create());
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
        org.mockito.Mockito.verify(coreWrapper).getServiceSchemaManager(eq(nodeType), eq("2.0"), any());
    }

    @Test
    void shouldUseExplicitStringVersion() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        String version = "3.0";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field(VERSION, version),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, version);
        when(smsJsonConverter.fromJson(any())).thenReturn(ConfigurationAttributesFactory.create());
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
        org.mockito.Mockito.verify(coreWrapper).getServiceSchemaManager(eq(nodeType), eq(version), any());
    }

    @Test
    void shouldCheckIsBlocking() {
        assertThat(validator.isBlocking()).isTrue();
    }

    @Test
    void shouldCallServiceConfigValidatorWhenPresent() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "1.0");
        ConfigurationAttributes attributes = ConfigurationAttributesFactory.create();
        when(smsJsonConverter.fromJson(any())).thenReturn(attributes);
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // Mock the service config validator
        when(serviceSchema.getServiceConfigValidator()).thenReturn(serviceConfigValidator);

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
        org.mockito.Mockito.verify(serviceConfigValidator).validate(any(), any(), eq(attributes));
    }

    @Test
    void shouldHandleServiceConfigValidatorException() throws Exception {
        configureAdminTokenStubbing();
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType),
                field("config", object())));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        mockServiceSchema(nodeType, "1.0");
        ConfigurationAttributes attributes = ConfigurationAttributesFactory.create();
        when(smsJsonConverter.fromJson(any())).thenReturn(attributes);
        when(serviceSchema.getAttributesToValidatorsMap()).thenReturn(Collections.emptyMap());

        // Mock the service config validator to throw exception
        when(serviceSchema.getServiceConfigValidator()).thenReturn(serviceConfigValidator);
        String errorMessage = "Config invalid";
        org.mockito.Mockito.doThrow(new ServiceConfigException(errorMessage)).when(serviceConfigValidator)
                .validate(any(), any(), any());

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).message()).contains("Node '%s' configuration is invalid. "
                .formatted(nodeId) + errorMessage);
    }

    @Test
    void shouldSkipAllValidationIfConfigMissing() {
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType)));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldSkipAllValidationIfNoConfigFound() {
        // Given
        String nodeId = "node1";
        String nodeType = "TestNode";
        JsonValue node = json(object(
                field("_id", nodeId),
                field(NODE_TYPE, nodeType)));
        JsonValue tree = json(object(field(NODES, json(List.of(node)))));

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldValidateAllNodesIfOneHasConfig() throws Exception {
        // Given
        String nodeId1 = "node1";
        String nodeType1 = "TestNode";
        JsonValue node1 = json(object(
                field("_id", nodeId1),
                field(NODE_TYPE, nodeType1),
                field("config", object())));

        String nodeId2 = "node2";
        String nodeType2 = "TestNode";
        JsonValue node2 = json(object(
                field("_id", nodeId2),
                field(NODE_TYPE, nodeType2)));

        JsonValue tree = json(object(field(NODES, json(List.of(node1, node2)))));
        mockServiceSchema(nodeType2, "1.0");
        String errorMessage = "Node 2 must have a banana";
        when(smsJsonConverter.fromJson(any()))
                .thenReturn(ConfigurationAttributesFactory.create()) // node 1 will hit this
                .thenThrow(new BadRequestException(errorMessage)); // node 2 will hit this

        // When
        List<ValidationFailure> failures = validator.validate(tree, null);

        // Then
        assertThat(failures).containsExactly(new ValidationFailure(
                "Node 'node2' configuration is invalid. " + errorMessage, ERROR));
    }

    private void mockServiceSchema(String nodeType, String version) throws Exception {
        when(coreWrapper.getServiceSchemaManager(eq(nodeType), eq(version), any())).thenReturn(serviceSchemaManager);
        when(serviceSchemaManager.getOrganizationSchema()).thenReturn(mock(ServiceSchema.class));
        when(serviceSchemaManager.getOrganizationSchema().getSubSchema("instance")).thenReturn(serviceSchema);
        when(jsonConverterFactory.create(serviceSchema, true)).thenReturn(smsJsonConverter);
    }
}
