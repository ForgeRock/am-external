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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.validators;

import static com.sun.identity.shared.Constants.AM_COOKIE_DEFAULT_VALUE;
import static com.sun.identity.shared.Constants.AM_COOKIE_NAME;
import static com.sun.identity.shared.Constants.AM_LB_COOKIE_DEFAULT_VALUE;
import static com.sun.identity.shared.Constants.AM_LB_COOKIE_NAME;

import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates that the Set-Cookie headers do not contain reserved cookies.
 */
public class ReservedCookieValidator implements ServiceAttributeValidator {

    private final Pattern cookiePattern;

    @Inject
    ReservedCookieValidator(SystemPropertiesWrapper systemProperties) {
        String ssoCookieName = systemProperties.get(AM_COOKIE_NAME, AM_COOKIE_DEFAULT_VALUE);
        String lbCookieName = systemProperties.get(AM_LB_COOKIE_NAME, AM_LB_COOKIE_DEFAULT_VALUE);
        String combinedPattern = Pattern.quote(ssoCookieName) + "|" + Pattern.quote(lbCookieName);
        cookiePattern = Pattern.compile(combinedPattern, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean validate(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }

        return values.stream()
                       .filter(value -> value.startsWith("[Set-Cookie]"))
                       .noneMatch(value -> cookiePattern.matcher(value).find());
    }
}
