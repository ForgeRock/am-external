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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.forgerock.openam.federation.rest.schema.hosted.service.KeyValue;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

/**
 * Maps strings to {@link KeyValue}s.
 */
public final class KeyValueMapper extends ValueMapper<List<String>, List<KeyValue>> {

    @Override
    public List<KeyValue> map(List<String> values, EnricherContext context) {
        return values.stream()
                .map(value -> value.split("="))
                .filter(components -> components.length == 2)
                .map(components -> new KeyValue(components[0], components[1]))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> inverse(List<KeyValue> mappedValues, EnricherContext context) {
        return mappedValues.stream()
                .map(keyValue -> keyValue.getKey() + "=" + keyValue.getValue())
                .collect(Collectors.toList());
    }
}