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

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.forgerock.am.trees.api.TreeBuilder.VERSION;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;

/**
 * Validator to validate type and version of the node.
 */
public class NodeTypeValidator implements Validator {

    private final NodeRegistry nodeRegistry;

    @Inject
    NodeTypeValidator(NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        List<ValidationFailure> failures = new ArrayList<>();
        var nodes = requestBody.get("nodes");
        nodes.asMap().keySet().forEach(nodeId -> {
            JsonValue node = nodes.get(nodeId);
            String nodeType = node.get("nodeType").asString();
            if (!nodeRegistry.isNodeService(nodeType)) {
                String message = "'nodeType' '" + nodeType + "' does not exist";
                failures.add(new ValidationFailure(message, ValidationFailure.Severity.ERROR));
            } else {
                List<Integer> versions = nodeRegistry.getVersions(nodeType);
                var originalVersion = node.get(VERSION);
                var parsedVersion = parseVersion(originalVersion);
                if (!versions.contains(parsedVersion)) {
                    String message =
                            "'nodeType' '" + nodeType + "' does not have version '" + originalVersion.getObject() + "'";
                    failures.add(new ValidationFailure(message, ValidationFailure.Severity.ERROR));
                }
            }
        });
        return failures;
    }

    private Integer parseVersion(JsonValue originalVersion) {
        if (originalVersion.isString()) {
            return Integer.valueOf(substringBefore(originalVersion.asString(), "."));
        } else if (originalVersion.isNumber()) {
            return originalVersion.asInteger();
        } else {
            return 1;
        }
    }

    @Override
    public boolean isBlocking() {
        return true;
    }
}
