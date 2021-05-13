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

import org.forgerock.openam.federation.rest.schema.hosted.service.AuthContextItem;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

/**
 * Mapper to go from string representation to a service provider's {@link AuthContextItem} instance.
 */
public class SpAuthContextMapper extends ValueMapper<List<String>, List<AuthContextItem>> {

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

        return new AuthContextItem(components[0], Integer.valueOf(components[1]), "default".equals(components[2]));
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
                .append(authContextItem.getLevel())
                .append("|");
        if (authContextItem.getDefaultItem() != null) {
            sb.append(authContextItem.getDefaultItem() ? "default" : "");
        }
        return sb.toString();
    }
}
