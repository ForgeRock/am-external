/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.Set;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validator for session property map.
 */
public class GreaterThanZeroValidator implements ServiceAttributeValidator {

    @Override
    public boolean validate(Set<String> values) {
        boolean isValid = true;
        for (String value : values) {
            if (Integer.parseInt(value) <= 0) {
                isValid = false;
                break;
            }
        }
        return isValid;
    }
}
