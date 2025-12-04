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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.openam.federation.rest.schema.shared.AttributeMap;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps an {@link AttributeMap} from a json representation.
 */
public class AttributeMapMapper extends ValueMapper<List<String>, List<AttributeMap>> {

    private static final Logger logger = LoggerFactory.getLogger(AttributeMap.class);

    private static final Pattern ATTRIBUTE_MAP_PATTERN = Pattern.compile("^((?<nameFormatUri>\\S*?)\\|)?"
            + "(?<samlAttribute>[\\S ]*?)=(?<localAttribute>[\\S ]*?)(;(?<binary>binary))?$");

    @Override
    public List<AttributeMap> map(List<String> values, EnricherContext context) {
        return values.stream()
                .map(this::jaxbToPojo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    @Override
    public List<String> inverse(List<AttributeMap> mappedValue, EnricherContext context) {
        return mappedValue.stream()
                .map(this::pojoToJaxb)
                .collect(toList());
    }

    private Optional<AttributeMap> jaxbToPojo(String serializedAttributeMap) {
        Matcher matcher = ATTRIBUTE_MAP_PATTERN.matcher(serializedAttributeMap);

        if (!matcher.find()) {
            logger.warn("Unable to deserialize attribute map string " + serializedAttributeMap);
            return Optional.empty();
        }

        String nameFormatUri = matcher.group("nameFormatUri");
        String samlAttribute = matcher.group("samlAttribute");
        String localAttribute = matcher.group("localAttribute");
        boolean binary = "binary".equals(matcher.group("binary"));
        return Optional.of(new AttributeMap(nameFormatUri, samlAttribute, localAttribute, binary));
    }

    private String pojoToJaxb(AttributeMap attributeMap) {
        return (StringUtils.isBlank(attributeMap.getNameFormatUri()) ? "" : (attributeMap.getNameFormatUri() + "|"))
                + (attributeMap.getSamlAttribute() + "=" + attributeMap.getLocalAttribute())
                + (Boolean.TRUE.equals(attributeMap.getBinary()) ? ";binary" : "");
    }
}
