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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.forgerock.openam.federation.rest.schema.shared.ApplicationSecurityConfigItem;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

import com.google.common.base.Joiner;

/**
 * Maps SAE secret settings to and from POJOs.
 */
public final class ApplicationSecurityConfigItemMapper
        extends ValueMapper<List<String>, List<ApplicationSecurityConfigItem>> {

    private static final String URL = "url";
    private static final String TYPE = "type";
    private static final String PUB_KEY_ALIAS = "pubkeyalias";
    private static final String ENCRYPTION_ALGORITHM = "encryptionalgorithm";
    private static final String ENCRYPTION_KEY_STRENGTH = "encryptionkeystrength";
    private static final String SECRET = "secret";

    @Override
    public List<ApplicationSecurityConfigItem> map(List<String> value, EnricherContext context) {
        return value.stream()
                .map(this::deserializeItems)
                .collect(toList());
    }

    private ApplicationSecurityConfigItem deserializeItems(String item) {
        Map<String, String> settings = Arrays.stream(item.split("\\|"))
                .map(value -> value.split("=", 2))
                .filter(value -> value.length == 2)
                .collect(Collectors.toMap(value -> value[0], value -> value[1]));
        return new ApplicationSecurityConfigItem(settings.get(URL),
                settings.get(TYPE),
                settings.get(PUB_KEY_ALIAS),
                settings.get(ENCRYPTION_ALGORITHM),
                settings.get(ENCRYPTION_KEY_STRENGTH),
                settings.get(SECRET));
    }

    @Override
    public List<String> inverse(List<ApplicationSecurityConfigItem> mappedValue, EnricherContext context) {
        return mappedValue.stream()
                .map(this::serializeItems)
                .collect(toList());
    }

    private String serializeItems(ApplicationSecurityConfigItem item) {
        List<String> components = new ArrayList<>(6);
        addIfNotNull(components, URL, item.getUrl());
        addIfNotNull(components, TYPE, item.getType());
        addIfNotNull(components, PUB_KEY_ALIAS, item.getPubKeyAlias());
        addIfNotNull(components, ENCRYPTION_ALGORITHM, item.getEncryptionAlgorithm());
        addIfNotNull(components, ENCRYPTION_KEY_STRENGTH, item.getEncryptionKeyStrength());
        addIfNotNull(components, SECRET, item.getSecret());
        return Joiner.on('|').join(components);
    }

    private void addIfNotNull(List<String> list, String key, String value) {
        if (value != null) {
            list.add(key + "=" + value);
        }
    }
}
