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
package org.forgerock.openam.auth.nodes.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.forgerock.am.trees.api.NodeVisitor;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;

/**
 * A {@link NodeVisitor} implementation responsible for collecting and merging all {@link InputState}
 * requirements from an authentication tree or sub-tree.
 *
 * <p>This collector traverses the tree nodes and aggregates the required inputs. If multiple nodes
 * define an input with the same name, the collector merges them using a "promotion" logic:
 * if any instance of the input is marked as {@code required}, the merged result will also be
 * marked as {@code required}.</p>
 */
public class InputCollector implements NodeVisitor<List<InputState>> {

    @Override
    public List<InputState> visit(UUID nodeId, Optional<Class<? extends Node>> nodeType,
                                  Optional<String> displayName, Supplier<Node> nodeSupplier,
                                  Stream<Supplier<List<InputState>>> nextVisitings) throws NodeProcessException {
        final Node node = nodeSupplier.get();
        if (node == null) {
            return Collections.emptyList();
        }
        final InputState[] initialInputs = node.getInputs();
        final Map<String, InputState> mergedMap = new HashMap<>(initialInputs.length + 10);
        for (InputState input : initialInputs) {
            if (input != null) {
                mergedMap.put(input.name, input);
            }
        }
        Iterable<Supplier<List<InputState>>> iterable = nextVisitings::iterator;
        for (Supplier<List<InputState>> childSupplier : iterable) {
            final List<InputState> childInputs = childSupplier.get();
            if (childInputs == null) {
                continue;
            }
            for (InputState childInput : childInputs) {
                if (childInput == null) {
                    continue;
                }
                final InputState existing = mergedMap.get(childInput.name);
                if (existing == null) {
                    mergedMap.put(childInput.name, childInput);
                } else if (!existing.required && childInput.required) {
                    mergedMap.put(childInput.name, childInput);
                }
            }
        }
        return new ArrayList<>(mergedMap.values());
    }
}
