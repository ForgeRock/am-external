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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.auth.trees.validation.Validator;
import org.forgerock.openam.core.realms.Realm;

import com.google.common.collect.Streams;

/**
 * Validator to validate coordinates of the nodes and staticNodes if they are greater or equal to zero.
 */
public class NodeCoordinatesValidator implements Validator {

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        return Streams.concat(
                validateNodes(requestBody.get("nodes")),
                validateNodes(requestBody.get("staticNodes"))
        ).toList();
    }

    private Stream<ValidationFailure> validateNodes(JsonValue nodes) {
        return nodes.keys()
                .stream()
                .flatMap(nodeId -> {
                    var node = nodes.get(nodeId);
                    return Streams.concat(
                            validateCoordinate(node, nodeId, "x").stream(),
                            validateCoordinate(node, nodeId, "y").stream()
                    );

                });
    }

    private Optional<ValidationFailure> validateCoordinate(JsonValue node, String nodeId, String coordinate) {
        if (node.isDefined(coordinate)) {
            Double value;
            JsonValue valueJson = node.get(coordinate);
            value = (valueJson.isNumber()) ? valueJson.asDouble() : Double.valueOf(valueJson.asString());
            if (value < 0) {
                String message = "'" + coordinate + "' coordinate of node '" + nodeId
                        + "' should be greater than or equal to zero";
                return Optional.of(new ValidationFailure(message, ValidationFailure.Severity.WARNING));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
