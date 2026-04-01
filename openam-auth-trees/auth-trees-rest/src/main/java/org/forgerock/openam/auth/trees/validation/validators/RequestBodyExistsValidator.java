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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.validation.validators;

import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;

/**
 * Validator to check that the request body exists and is not empty.
 */
public class RequestBodyExistsValidator implements Validator {
    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        if (requestBody == null || requestBody.isNull() || requestBody.size() == 0) {
            return List.of(new ValidationFailure("Request body is empty", ValidationFailure.Severity.ERROR));
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isBlocking() {
        return true;
    }
}
