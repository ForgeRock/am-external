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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.sun.identity.sm.DescriptiveServiceAttributeValidator;

/**
 * Validates Radius handler configuration attribute values.
 */
public class RadiusHandlerConfigValidator implements DescriptiveServiceAttributeValidator {

    private static final String TREE_KEY = "tree";
    private static final String DUPLICATE_MESSAGE_KEY = "radius-handlerconfig-duplicates";
    private static final String I18N_FILE = "radiusServer";

    private List<String> validationFailures = List.of();

    @Override
    public boolean validate(Set<String> values) {
        validationFailures = List.of();
        if (values == null || values.isEmpty()) {
            return true;
        }

        long treeCount = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(RadiusHandlerConfigValidator::extractKey)
                .filter(RadiusHandlerConfigValidator.TREE_KEY::equals)
                .count();

        if (treeCount > 1) {
            validationFailures = List.of(DUPLICATE_MESSAGE_KEY);
            return false;
        }
        return true;
    }

    private static String extractKey(String entry) {
        int idx = entry.indexOf('=');
        return idx == -1 ? entry : entry.substring(0, idx);
    }

    @Override
    public List<String> validationMessages() {
        return validationFailures;
    }

    @Override
    public Optional<String> i18nFile() {
        return Optional.of(I18N_FILE);
    }
}
