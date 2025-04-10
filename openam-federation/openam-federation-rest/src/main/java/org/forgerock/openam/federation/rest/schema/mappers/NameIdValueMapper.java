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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.NameIdValueMap;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

/**
 * Converts nameID value map entries to POJOs.
 */
public final class NameIdValueMapper extends ValueMapper<List<String>, List<NameIdValueMap>> {

    private static final String BINARY = ";binary";

    @Override
    public List<NameIdValueMap> map(List<String> value, EnricherContext context) {
        return value.stream()
                .map(v -> v.split("="))
                .filter(v -> v.length == 2)
                .map(components -> {
                    boolean binary = components[1].endsWith(BINARY);
                    if (binary) {
                        components[1] = components[1].replace(BINARY, "");
                    }
                    return new NameIdValueMap(components[0], components[1], binary);
                })
                .collect(toList());
    }

    @Override
    public List<String> inverse(List<NameIdValueMap> mappedValue, EnricherContext context) {
        return mappedValue.stream()
                .map(value -> value.getKey() + "=" + value.getValue() + (value.getBinary() ? BINARY : ""))
                .collect(toList());
    }
}
