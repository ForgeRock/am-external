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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;

/**
 * Validator to validate displayName of the nodes if it doesn't contain non-printable characters.
 */
public class NodeDisplayNameValidator implements Validator {

    private static final Pattern PATTERN = Pattern.compile("[\\p{C}]");

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        JsonValue nodes = requestBody.get("nodes");
        List<ValidationFailure> failures = new ArrayList<>();

        nodes.keys().forEach(nodeId -> {
            JsonValue node = nodes.get(nodeId);
            String displayName = node.get("displayName").asString();
            if (displayName != null && !displayName.isEmpty()) {
                Matcher matcher = PATTERN.matcher(displayName);
                if (matcher.find()) {
                    String message = "'displayName' for node '" + nodeId + "' contains invalid characters";
                    failures.add(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
                }
            }
        });
        return  failures;
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}