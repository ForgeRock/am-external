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
package org.forgerock.openam.auth.trees.validation;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.realms.Realm;

/**
 * Interface for validators that can validate a JSON request body.
 * Validators can be blocking or non-blocking. If a blocking validator
 * returns an error, the validation process is stopped.
 */
public interface Validator {

    /**
     * Validate the given JSON request body.
     *
     * @param requestBody The JSON request body to validate.
     * @param realm The realm in which the validation is being performed.
     * @return A list of validation failures. If there are no failures, an empty list is returned.
     */
    List<ValidationFailure> validate(JsonValue requestBody, Realm realm);

    /**
     * Indicates if this validator is blocking. If a blocking validator returns an error,
     * the validation process is stopped.
     *
     * @return true if the validator is blocking, false otherwise.
     */
    boolean isBlocking();
}
