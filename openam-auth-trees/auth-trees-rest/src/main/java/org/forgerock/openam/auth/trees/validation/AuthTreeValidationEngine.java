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

import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine to run a series of Validators against a JSON request body.
 * The validators are run in order and if a blocking validator returns an error,
 * the validation process is stopped.
 */
public class AuthTreeValidationEngine {

    private static final Logger logger = LoggerFactory.getLogger(AuthTreeValidationEngine.class);
    private final List<Validator> validators;

    @Inject
    AuthTreeValidationEngine(List<Validator> validators) {
        this.validators = validators;
    }

    /**
     * Validate the request body against the configured validators.
     *
     * @param requestBody The JSON request body to validate.
     * @param realm The realm in which the validation is being performed.
     * @return A list of validation failures.
     */
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        List<ValidationFailure> results = new ArrayList<>();
        for (Validator validator : validators) {
            try {
                List<ValidationFailure> result = validator.validate(requestBody, realm);
                results.addAll(result);
                if (validator.isBlocking() && result.stream().anyMatch(r -> r.severity().equals(ERROR))) {
                    break;
                }
            } catch (Exception e) {
                // prevent 500 response due to unexpected errors in validators
                logger.error("Unexpected error during validation", e);
                results.add(new ValidationFailure("Validation failed due to an unexpected error", ERROR));
                break;
            }
        }
        return results;
    }
}
