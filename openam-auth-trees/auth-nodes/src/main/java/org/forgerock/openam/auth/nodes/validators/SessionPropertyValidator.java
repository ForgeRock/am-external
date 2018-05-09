/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.openam.configuration.MapValueParser;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validator for session property map.
 */
public class SessionPropertyValidator implements ServiceAttributeValidator {
    private final MapValueParser mapValueParser;
    private final Set<String> systemSessionProperties;

    /**
     * Constructs a new SessionPropertyValidator.
     * @param mapValueParser Map parser for string represented values.
     * @param systemSessionProperties List of system session properties.
     */
    @Inject
    public SessionPropertyValidator(MapValueParser mapValueParser, @Named("SystemSessionProperties")
            List<String> systemSessionProperties) {
        this.mapValueParser = mapValueParser;
        this.systemSessionProperties = new HashSet<>(systemSessionProperties);
    }

    @Override
    public boolean validate(Set<String> values) {
        Map<String, String> properties = mapValueParser.parse(values);
        boolean allKeysParsed = values.size() == properties.size();
        boolean containsSystemProperties = !Sets.intersection(systemSessionProperties, properties.keySet()).isEmpty();
        boolean allValuesNonEmpty = properties.values().stream().noneMatch(Strings::isNullOrEmpty);
        boolean hasAtLeastOneValue = values.size() > 0;
        return allKeysParsed && !containsSystemProperties && allValuesNonEmpty && hasAtLeastOneValue;
    }
}
