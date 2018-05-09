/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guava.common.collect.Sets;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validator for session property names.
 */
public class SessionPropertyNameValidator implements ServiceAttributeValidator {
    private final Set<String> systemSessionProperties;

    /**
     * Constructs a new SessionPropertyNameValidator.
     * @param systemSessionProperties List of system session properties.
     */
    @Inject
    public SessionPropertyNameValidator(@Named("SystemSessionProperties") List<String> systemSessionProperties) {
        this.systemSessionProperties = new HashSet<>(systemSessionProperties);
    }

    @Override
    public boolean validate(Set<String> values) {
        return values != null && !values.isEmpty() && Sets.intersection(systemSessionProperties, values).isEmpty();
    }
}
