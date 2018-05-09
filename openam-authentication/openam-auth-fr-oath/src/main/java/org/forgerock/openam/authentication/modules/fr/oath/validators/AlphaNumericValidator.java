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
import java.util.regex.Pattern;
import org.forgerock.openam.utils.StringUtils;

/**
 * Service Manager validator for alpha numeric characters.
 */
public class AlphaNumericValidator implements ServiceAttributeValidator {

    private static final Pattern NOT_ALPHA_NUM = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Validates each of the provided members of the Set to confirm that it conforms
     * to the expected validation - only a-z/A-Z, 0-9.
     *
     * @param values the <code>Set</code> of attribute values to validate
     * @return true if everything is valid, false otherwise.
     */
    @Override
    public boolean validate(Set<String> values) {
        for (String toTest : values) {
            if (StringUtils.isEmpty(toTest) || NOT_ALPHA_NUM.matcher(toTest).find()) {
                return false;
            }
        }

        return true;
    }
}
