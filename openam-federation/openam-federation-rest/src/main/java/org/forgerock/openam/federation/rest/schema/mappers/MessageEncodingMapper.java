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

import static java.util.Collections.singletonList;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.ResponseArtifactMessageEncoding.Encoding;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

/**
 * Mapper to go from string representation to an {@link Encoding} instance.
 */
public class MessageEncodingMapper extends ValueMapper<List<String>, Encoding> {

    @Override
    public Encoding map(List<String> values, EnricherContext context) {
        return values.stream()
                .map(String::toUpperCase)
                .map(Encoding::valueOf)
                .findFirst()
                .orElse(Encoding.URI);
    }

    @Override
    public List<String> inverse(Encoding mappedValue, EnricherContext context) {
        return mappedValue == null ? singletonList(Encoding.URI.name()) : singletonList(mappedValue.name());
    }
}
