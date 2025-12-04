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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.validators;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
