/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.fr.oath.validators;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Set;

/**
 * Validates that the entered code length is at least 6.
 */
public class CodeLengthValidator implements ServiceAttributeValidator {

    public static final int MIN_CODE_LENGTH = 6;

    /**
     * Validates each of the provided members of the Set to confirm that they are
     * greater than the minimum value.
     *
     * @param values the <code>Set</code> of attribute values to validate
     * @return true if everything is valid, false otherwise.
     */
    @Override
    public boolean validate(Set<String> values) {
        try {
            for (String toTest : values) {
                if (Integer.valueOf(toTest) < MIN_CODE_LENGTH) {
                    return false;
                }
            }
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }
}
