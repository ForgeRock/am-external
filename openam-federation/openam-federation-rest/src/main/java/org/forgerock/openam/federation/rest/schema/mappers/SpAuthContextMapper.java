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

import org.forgerock.openam.federation.rest.schema.hosted.service.AuthContextItem;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper to go from string representation to a service provider's {@link AuthContextItem} instance.
 */
public class SpAuthContextMapper extends ValueMapper<List<String>, List<AuthContextItem>> {

    private static final Logger logger = LoggerFactory.getLogger(SpAuthContextMapper.class);

    @Override
    public List<AuthContextItem> map(List<String> values, EnricherContext context) {
        return values.stream()
                .map(this::deserializeAuthnContext)
                .collect(Collectors.toList());
    }

    private AuthContextItem deserializeAuthnContext(String serializedRow) {
        String[] components = serializedRow.split("\\|", -1);
        if (components.length < 2) {
            throw new IllegalArgumentException("Unable to parse authentication context data");
        }
        Integer authnLevel = 0;
        try {
            authnLevel = Integer.valueOf(components[1]);
        } catch (NumberFormatException e) {
            logger.warn("Provided Authentication Context level is invalid, defaulting to 0. Error: {}",
                    e.getMessage());
        }
        return new AuthContextItem(components[0], authnLevel, "default".equals(components[2]));
    }

    @Override
    public List<String> inverse(List<AuthContextItem> mappedValue, EnricherContext context) {
        return mappedValue.stream()
                .map(this::serializeAuthnContext)
                .collect(Collectors.toList());
    }

    private String serializeAuthnContext(AuthContextItem authContextItem) {
        StringBuilder sb = new StringBuilder()
                .append(authContextItem.getContextReference())
                .append("|")
                .append(authContextItem.getLevel() != null ? authContextItem.getLevel() : 0)
                .append("|");
        if (authContextItem.getDefaultItem() != null) {
            sb.append(authContextItem.getDefaultItem() ? "default" : "");
        }
        return sb.toString();
    }
}
