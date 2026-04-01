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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.DescriptiveServiceAttributeValidator;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.ServiceAttributeValidator;

@ExtendWith(MockitoExtension.class)
class AttributeSchemaValidatorTest {

    private TestAttributeSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TestAttributeSchemaValidator();
    }

    @Test
    void shouldPassValidationWhenAttributeIsValid() {
        // Given
        String attrName = "testAttr";
        Set<String> attrValues = Set.of("value1");
        Map<String, Set<String>> attributes = Map.of(attrName, attrValues);

        ServiceAttributeValidator attrValidator = mock(ServiceAttributeValidator.class);
        when(attrValidator.validate(anySet())).thenReturn(true);
        Map<String, Set<ServiceAttributeValidator>> validators = Map.of(attrName, Set.of(attrValidator));

        // When
        List<ValidationFailure> failures = validator.validateExistingAttributes(attributes, validators);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldFailValidationWhenAttributeIsInvalid() {
        // Given
        String attrName = "testAttr";
        Set<String> attrValues = Set.of("invalidValue");
        Map<String, Set<String>> attributes = Map.of(attrName, attrValues);

        ServiceAttributeValidator attrValidator = mock(ServiceAttributeValidator.class);
        when(attrValidator.validate(anySet())).thenReturn(false);
        Map<String, Set<ServiceAttributeValidator>> validators = Map.of(attrName, Set.of(attrValidator));

        // When
        List<ValidationFailure> failures = validator.validateExistingAttributes(attributes, validators);

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).message()).contains("Attribute '" + attrName + "' failed validation");
    }

    @Test
    void shouldFailValidationWithDescriptiveMessage() {
        // Given
        String attrName = "testAttr";
        Set<String> attrValues = Set.of("invalidValue");
        Map<String, Set<String>> attributes = Map.of(attrName, attrValues);

        DescriptiveServiceAttributeValidator attrValidator = mock(DescriptiveServiceAttributeValidator.class);
        when(attrValidator.validate(anySet())).thenReturn(false);
        when(attrValidator.i18nFile()).thenReturn(Optional.empty());
        when(attrValidator.validationMessages()).thenReturn(List.of("Custom error message"));
        Map<String, Set<ServiceAttributeValidator>> validators = Map.of(attrName, Set.of(attrValidator));

        // When
        List<ValidationFailure> failures = validator.validateExistingAttributes(attributes, validators);

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).message()).contains("Custom error message");
    }

    @Test
    void shouldSkipValidationForUnknownAttribute() {
        // Given
        String attrName = "unknownAttr";
        Map<String, Set<String>> attributes = Map.of(attrName, Set.of("value"));
        Map<String, Set<ServiceAttributeValidator>> validators = Collections.emptyMap();

        // When
        List<ValidationFailure> failures = validator.validateExistingAttributes(attributes, validators);

        // Then
        assertThat(failures).isEmpty();
    }

    public static Stream<Arguments> missingAttributeTestCases() {
        String requiredAttr = "requiredAttr";
        return Stream.of(
                // Empty defaults
                Arguments.of(requiredAttr,
                        null,
                        ValidationFailure.Severity.ERROR,
                        "Required attribute '" + requiredAttr + "' not found"),
                // Default values but not for requiredAttr
                Arguments.of(requiredAttr,
                        Map.of("otherAttr1", Set.of("defaultValue1"),
                                "otherAttr2", Set.of("defaultValue2", "defaultValue3")),
                        ValidationFailure.Severity.ERROR,
                        "Required attribute '" + requiredAttr + "' not found"),
                // Empty default values
                Arguments.of(requiredAttr,
                        Map.of(requiredAttr, Collections.emptySet(),
                                "otherAttr2", Set.of("defaultValue2", "defaultValue3")),
                        ValidationFailure.Severity.ERROR,
                        "Required attribute '" + requiredAttr + "' not found"),
                // With default values - note: preserves insertion order of default values for message consistency
                Arguments.of(requiredAttr,
                        Map.of(requiredAttr, new LinkedHashSet<>(List.of("defaultValue1", "defaultValue2")),
                                "otherAttr2", Set.of("defaultValue2", "defaultValue3")),
                        ValidationFailure.Severity.WARNING,
                        "Required attribute '" + requiredAttr + "' not found, but has default value(s): "
                                + "[defaultValue1, defaultValue2]. Please check suitability")
        );
    }
    @ParameterizedTest
    @MethodSource("missingAttributeTestCases")
    void shouldReportMissingRequiredAttribute(String requiredAttr, Map<String, Set<String>> defaults,
        ValidationFailure.Severity expectedSeverity, String expectedMessage) {
        // Given
        Map<String, Set<String>> attributes = Collections.emptyMap();
        ConfigurationAttributes defaultAttributes = null;
        if (defaults != null) {
            defaultAttributes = ConfigurationAttributesFactory.create();
            defaults.forEach(defaultAttributes::put);
        }

        ServiceAttributeValidator requiredValidator = mock(RequiredValueValidator.class);
        Map<String, Set<ServiceAttributeValidator>> validators = Map.of(requiredAttr, Set.of(requiredValidator));

        // When
        List<ValidationFailure> failures;
        if (defaults == null) {
            failures = validator.checkForMissingAttributes(attributes, validators);
        } else {
            failures = validator.checkForMissingAttributes(attributes, validators, defaultAttributes);
        }

        // Then
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).severity()).isEqualTo(expectedSeverity);
        assertThat(failures.get(0).message()).isEqualTo(expectedMessage);
    }

    @Test
    void shouldPassCheckForMissingAttributesWhenPresent() {
        // Given
        String requiredAttr = "requiredAttr";
        Map<String, Set<String>> attributes = Map.of(requiredAttr, Set.of("value"));

        ServiceAttributeValidator requiredValidator = mock(RequiredValueValidator.class);
        Map<String, Set<ServiceAttributeValidator>> validators = Map.of(requiredAttr, Set.of(requiredValidator));

        // When
        List<ValidationFailure> failures = validator.checkForMissingAttributes(attributes, validators);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldIgnoreNonRequiredValidatorsWhenCheckingMissing() {
        // Given
        String optionalAttr = "optionalAttr";
        Map<String, Set<String>> attributes = Collections.emptyMap();

        ServiceAttributeValidator optionalValidator = mock(ServiceAttributeValidator.class);
        Map<String, Set<ServiceAttributeValidator>> validators = Map.of(optionalAttr, Set.of(optionalValidator));

        // When
        List<ValidationFailure> failures = validator.checkForMissingAttributes(attributes, validators);

        // Then
        assertThat(failures).isEmpty();
    }

    @Test
    void shouldFailValidationWithDescriptiveI18nMessage() {
        // Given
        String attrName = "testAttr";
        Set<String> attrValues = Set.of("invalidValue");
        Map<String, Set<String>> attributes = Map.of(attrName, attrValues);

        DescriptiveServiceAttributeValidator attrValidator = mock(DescriptiveServiceAttributeValidator.class);
        when(attrValidator.validate(anySet())).thenReturn(false);
        when(attrValidator.i18nFile()).thenReturn(Optional.of("TestBundle"));
        when(attrValidator.validationMessages()).thenReturn(List.of("test.error.key"));

        // Mock ResourceBundle and Locale static methods
        try (MockedStatic<AMResourceBundleCache> mockedBundleCache = mockStatic(AMResourceBundleCache.class);
             MockedStatic<Locale> mockedLocale = mockStatic(Locale.class)) {

            AMResourceBundleCache bundleCache = mock(AMResourceBundleCache.class);
            java.util.ResourceBundle resourceBundle = mock(java.util.ResourceBundle.class);

            when(AMResourceBundleCache.getInstance()).thenReturn(bundleCache);
            when(bundleCache.getResBundle(eq("TestBundle"), eq(java.util.Locale.ENGLISH)))
                    .thenReturn(resourceBundle);
            when(Locale.getString(eq(resourceBundle), eq("test.error.key")))
                    .thenReturn("Localized Error Message");

            Map<String, Set<ServiceAttributeValidator>> validators = Map.of(attrName, Set.of(attrValidator));

            // When
            List<ValidationFailure> failures = validator.validateExistingAttributes(attributes, validators);

            // Then
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).message()).contains("Localized Error Message");
        }
    }

    // Concrete implementation for testing
    static class TestAttributeSchemaValidator extends AttributeSchemaValidator {
        @Override
        public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
            return List.of();
        }

        @Override
        public boolean isBlocking() {
            return false;
        }
    }
}
