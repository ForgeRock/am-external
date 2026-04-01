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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceAttributeValidator;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

@ExtendWith(MockitoExtension.class)
class TreeSchemaValidatorTest {

    @Mock
    private SmsJsonConverterFactory treeConverterFactory;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private SmsJsonConverter treeConverter;
    @Mock
    private ServiceSchema treeSchema;
    @Mock
    private ServiceSchemaManager serviceSchemaManager;
    @Mock
    private ServiceSchema orgSchema;

    private TreeSchemaValidator treeSchemaValidator;

    @BeforeEach
    void setUp() throws SMSException, SSOException {
        given(coreWrapper.getServiceSchemaManager(anyString(), any())).willReturn(serviceSchemaManager);
        given(serviceSchemaManager.getOrganizationSchema()).willReturn(orgSchema);
        given(orgSchema.getSubSchema("tree")).willReturn(treeSchema);
        given(treeConverterFactory.create(any(ServiceSchema.class), anyBoolean())).willReturn(treeConverter);
        treeSchemaValidator = new TreeSchemaValidator(treeConverterFactory, coreWrapper);
    }

    @Test
    void isBlockingShouldReturnTrue() {
        assertThat(treeSchemaValidator.isBlocking()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("converterExceptionProvider")
    void validateShouldReturnValidationErrorsWhenConverterThrowsBadRequestException(Exception ex) throws Exception {
        // Given
        JsonValue requestBody = json(object(field("invalid", "data")));
        given(treeConverter.fromJson(any(JsonValue.class))).willThrow(ex);

        // When
        var result = treeSchemaValidator.validate(requestBody, null);

        // Then
        assertThat(result).containsExactly(new ValidationFailure("Some Error Message", ERROR));
    }

    public static Stream<Arguments> converterExceptionProvider() {
        return Stream.of(
                Arguments.of(new JsonException("Some Error Message")),
                Arguments.of(new BadRequestException("Some Error Message")),
                Arguments.of(new InternalServerErrorException("Some Error Message"))
        );
    }

    @Test
    void validateShouldReturnEmptyListWhenNoValidatorsToRun() throws Exception {
        // Given
        JsonValue requestBody = json(object(field("valid", "data")));
        given(treeConverter.fromJson(any(JsonValue.class)))
                .willReturn(ConfigurationAttributesFactory.create(Map.of("valid", Set.of("data"))));
        given(treeSchema.getAttributesToValidatorsMap()).willReturn(Map.of());

        // When
        var result = treeSchemaValidator.validate(requestBody, null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void validateShouldReturnErrorWhenRequiredValueMissing() throws Exception {
        // Given
        JsonValue requestBody = json(object(field("someAttribute", "someValue")));
        given(treeConverter.fromJson(any(JsonValue.class)))
                .willReturn(ConfigurationAttributesFactory.create(Map.of("someAttribute", Set.of("someValue"))));
        given(treeSchema.getAttributesToValidatorsMap())
                .willReturn(Map.of("requiredAttribute", Set.of(new RequiredValueValidator())));

        // When
        var result = treeSchemaValidator.validate(requestBody, null);

        // Then
        assertThat(result).containsExactly(
                new ValidationFailure("Required attribute 'requiredAttribute' not found", ERROR));
    }

    @Test
    void validateShouldReturnErrorWhenAttributeValidationFails() throws Exception {
        // Given
        JsonValue requestBody = json(object(field("someAttribute", "invalidValue")));
        given(treeConverter.fromJson(any(JsonValue.class)))
                .willReturn(ConfigurationAttributesFactory.create(Map.of("someAttribute", Set.of("invalidValue"))));
        var failingValidator = new ServiceAttributeValidator() {
            @Override
            public boolean validate(Set<String> values) {
                return false; // Always fail
            }
        };
        given(treeSchema.getAttributesToValidatorsMap())
                .willReturn(Map.of("someAttribute", Set.of(failingValidator)));

        // When
        var result = treeSchemaValidator.validate(requestBody, null);

        // Then
        assertThat(result).containsExactly(
                new ValidationFailure("Attribute 'someAttribute' failed validation", ERROR));
    }
}