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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sun.identity.sm.DescriptiveServiceAttributeValidator;

/**
 * Validator for session property map.
 *
 * @deprecated Use {@link org.forgerock.openam.sm.validation.GreaterThanZeroValidator} instead.
 */
@Deprecated(since = "8.1.0")
public class GreaterThanZeroValidator implements DescriptiveServiceAttributeValidator {

    private List<String> validationMessages;

    @Override
    public boolean validate(Set<String> values) {
        boolean isValid = true;
        for (String value : values) {
            if (Integer.parseInt(value) <= 0) {
                isValid = false;
                validationMessages = List.of("value-must-be-greater-than-zero");
                break;
            }
        }
        return isValid;
    }

    @Override
    public List<String> validationMessages() {
        return validationMessages;
    }

    @Override
    public Optional<String> i18nFile() {
        return Optional.of(GreaterThanZeroValidator.class.getSimpleName());
    }
}
