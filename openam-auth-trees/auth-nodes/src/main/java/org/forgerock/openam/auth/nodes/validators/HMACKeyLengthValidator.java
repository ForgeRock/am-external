/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.validators;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates a HTOP Key value.
 */
public class HMACKeyLengthValidator implements ServiceAttributeValidator {

    private static final int MINIMUM_PASSWORD_LENGTH = 6;

    /**
     * Validates a set of HMAC One Time Passwords.
     *
     * @param values the set of HMAC One Time Passwords to validate
     * @return true if all of the HMAC One Time Passwords are valid; false otherwise
     */
    public boolean validate(Set<String> values) {
        for (String value : values) {
            if (!validate(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates a HMAC One Time Password.
     *
     * @param value the HMAC One Time Password to validate
     * @return true if the HMAC One Time Password is valid; false otherwise
     */
    public boolean validate(String value) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return StringUtils.isNumeric(value) && Integer.parseInt(value) >= MINIMUM_PASSWORD_LENGTH;
    }
}
