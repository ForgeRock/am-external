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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.hosted.service.AuthenticationContext.AuthenticationComparisonType;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

/**
 * Mapper to go from string representation to an {@link AuthenticationComparisonType} instance.
 */
public class AuthComparisonTypeMapper extends ValueMapper<List<String>, AuthenticationComparisonType> {

    @Override
    public AuthenticationComparisonType map(List<String> values, EnricherContext context) {
        return values.stream()
                .map(String::toUpperCase)
                .map(AuthenticationComparisonType::valueOf)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<String> inverse(AuthenticationComparisonType mappedValue, EnricherContext context) {
        return mappedValue == null ? emptyList() : singletonList(mappedValue.name().toLowerCase());
    }
}
