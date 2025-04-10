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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.forgerock.openam.federation.rest.schema.hosted.identity.AuthContextItem;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper to go from string representation to an {@link AuthContextItem} instance.
 */
public class IdpAuthContextMapper extends ValueMapper<List<String>, List<AuthContextItem>> {

    private static final Logger logger = LoggerFactory.getLogger(IdpAuthContextMapper.class);

    @Override
    public List<AuthContextItem> map(List<String> values, EnricherContext context) {
        return values.stream()
                     .sorted(IdpAuthContextMapper::sortByDefault)
                     .map(this::deserializeAuthnContext)
                     .collect(Collectors.toList());
    }

    private AuthContextItem deserializeAuthnContext(String serializedRow) {
        String[] components = serializedRow.split("\\|", -1);
        if (components.length < 4) {
            throw new IllegalArgumentException("Unable to parse authentication context data");
        }

        String[] authnData = components[2].split("=");
        AuthContextItem.Key indexType = null;
        String indexValue = null;
        if (authnData.length == 2) {
            indexType = AuthContextItem.Key.fromValue(authnData[0]);
            indexValue = authnData[1];
        }
        Integer authnLevel = 0;
        try {
            authnLevel = Integer.valueOf(components[1]);
        } catch (NumberFormatException e) {
            logger.warn("Provided Authentication Context level is invalid, defaulting to 0. Error: {}",
                    e.getMessage());
        }
        return new AuthContextItem(components[0], indexType, indexValue, authnLevel);
    }

    @Override
    public List<String> inverse(List<AuthContextItem> mappedValue, EnricherContext context) {
        return IntStream.range(0, mappedValue.size())
                        .mapToObj(index -> serializeAuthnContext(mappedValue.get(index), index))
                        .collect(Collectors.toList());
    }

    private String serializeAuthnContext(AuthContextItem authContextItem, int itemPosition) {
        StringBuilder sb = new StringBuilder()
                .append(authContextItem.getContextReference())
                .append("|")
                .append(authContextItem.getLevel() != null ? authContextItem.getLevel() : 0)
                .append("|");
        if (authContextItem.getKey() != null) {
            sb.append(authContextItem.getKey())
                .append("=")
                .append(authContextItem.getValue());
        }
        sb.append("|");
        if (itemPosition == 0) {
            sb.append("default");
        }
        return sb.toString();
    }

    /**
     * Performs a comparator operation that ensures items with the 'default' tag come first in a collection. When
     * serialising authentication context items we treat the first item in the list as the default (this is reflected
     * in the UI). When deserialising, the entity config xml item will be tagged with a 'default' string, so the list
     * of items must be sorted with the default at first index.
     *
     * @param item1 first item to compare
     * @param item2 second item to compare
     * @return a comparator int value that should mean default items always come first
     */
    private static int sortByDefault(String item1, String item2) {
        if (item1.contains("default")) {
            return -1;
        } else if (item2.contains("default")) {
            return 1;
        } else {
            return 0;
        }
    }
}
