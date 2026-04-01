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

import static java.util.Locale.ENGLISH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.DescriptiveServiceAttributeValidator;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.ServiceAttributeValidator;
import org.forgerock.openam.sm.ConfigurationAttributes;

/**
 * Abstract validator for attribute schema validation.
 */
public abstract class AttributeSchemaValidator implements Validator {
    /**
     * Validate existing attributes against their validators.
     *
     * @param attributes the attributes to validate
     * @param validators the validators to validate against
     * @return list of validation failures
     */
    protected List<ValidationFailure> validateExistingAttributes(Map<String, Set<String>> attributes,
            Map<String, Set<ServiceAttributeValidator>> validators) {
        List<ValidationFailure> failures = new ArrayList<>();
        for (Map.Entry<String, Set<String>> attribute : attributes.entrySet()) {
            if (!validators.containsKey(attribute.getKey())) {
                continue;
            }
            Set<ServiceAttributeValidator> attributeValidators = validators.get(attribute.getKey());
            attributeValidators.forEach(validator -> {
                if (!validator.validate(attribute.getValue())) {
                    if (validator instanceof DescriptiveServiceAttributeValidator descriptiveValidator) {
                        var reasons = descriptiveValidator.i18nFile()
                                              .map(bundleName -> AMResourceBundleCache.getInstance()
                                                                         .getResBundle(bundleName, ENGLISH))
                                              .map(bundle -> descriptiveValidator.validationMessages().stream()
                                                                     .map(key -> Locale.getString(bundle, key))
                                                                     .collect(Collectors.joining("\n")))
                                              .orElseGet(() -> String.join("\n",
                                                      descriptiveValidator.validationMessages()));
                        failures.add(new ValidationFailure("Attribute '%s' failed validation.\n%s".formatted(
                                attribute.getKey(), reasons), ValidationFailure.Severity.ERROR));
                    } else {
                        failures.add(new ValidationFailure("Attribute '%s' failed validation".formatted(
                                attribute.getKey()), ValidationFailure.Severity.ERROR));
                    }
                }
            });
        }
        return failures;
    }

    /**
     * Check for missing required attributes.
     *
     * @param attributes the attributes to check
     * @param validators the validators to check against
     * @return list of validation failures
     */
    protected List<ValidationFailure> checkForMissingAttributes(Map<String, Set<String>> attributes,
            Map<String, Set<ServiceAttributeValidator>> validators) {
        return checkForMissingAttributes(attributes, validators, null);

    }
    /**
     * Check for missing required attributes.
     *
     * @param attributes the attributes to check
     * @param validators the validators to check against
     * @param attributeDefaults the attribute defaults
     * @return list of validation failures
     */
    protected List<ValidationFailure> checkForMissingAttributes(Map<String, Set<String>> attributes,
            Map<String, Set<ServiceAttributeValidator>> validators, ConfigurationAttributes attributeDefaults) {
        List<ValidationFailure> failures = new ArrayList<>();
        Map<String, Set<ServiceAttributeValidator>> requiredValidators =
                validators.entrySet().stream()
                        .filter(attributeValidators -> attributeValidators.getValue()
                                           .stream()
                                           .anyMatch(attributeValidator -> attributeValidator
                                                                                   instanceof RequiredValueValidator))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (String attributeName : requiredValidators.keySet()) {
            if (!attributes.containsKey(attributeName)) {
                Set<String> defaultValues = attributeDefaults != null ? attributeDefaults.get(attributeName) : null;
                if (defaultValues != null && !defaultValues.isEmpty()) {
                    failures.add(new ValidationFailure("Required attribute '" + attributeName
                            + "' not found, but has default value(s): " + attributeDefaults.get(attributeName)
                            + ". Please check suitability",
                            ValidationFailure.Severity.WARNING));
                } else {
                    failures.add(new ValidationFailure("Required attribute '" + attributeName + "' not found",
                            ValidationFailure.Severity.ERROR));
                }
            }
        }
        return failures;
    }
}
