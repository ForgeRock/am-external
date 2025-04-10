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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.Set;

import com.sun.identity.sm.ServiceAttributeValidator;
import org.forgerock.json.JsonException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates if a String is in valid JSON value format.
 */
public class JsonValidator implements ServiceAttributeValidator {

    /**
     * Validates the given set of string values contains a valid JSON format.
     *
     * @param values
     *            the <code>Set</code> of attribute values to validate
     * @return true if String contains a valid JSON format or is null/empty; false otherwise
     */
    @Override
    public boolean validate(Set<String> values) {
        if (values != null) {
            for (String value : values) {
                try {
                    if (StringUtils.isNotEmpty(value)) {
                        JsonValueBuilder.toJsonValue(value);
                    }
                } catch (JsonException e) {
                    return false;
                }
            }
        }
        return true;
    }
}
