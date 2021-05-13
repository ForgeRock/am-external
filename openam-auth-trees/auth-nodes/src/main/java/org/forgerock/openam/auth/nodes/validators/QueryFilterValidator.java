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

package org.forgerock.openam.auth.nodes.validators;

import java.util.Set;

import org.forgerock.json.resource.QueryFilters;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validator for query filters.
 */
public class QueryFilterValidator implements ServiceAttributeValidator {
    @Override
    public boolean validate(Set<String> values) {
        for (String value : values) {
            try {
                QueryFilters.parse(value);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
}
