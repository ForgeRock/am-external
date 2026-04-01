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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static java.util.Objects.requireNonNull;
import static org.forgerock.api.enums.Stability.STABLE;
import static org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder.leaf;

import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.http.routing.Version;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.openam.core.rest.sms.routing.ServiceEndpointDefinition;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder;

import com.google.auto.service.AutoService;

/**
 * A service endpoint definition for all the Node services.
 */
@AutoService(ServiceEndpointDefinition.class)
public class NodeEndpointDefinition implements ServiceEndpointDefinition {

    private final Predicate<String> nodesHandlesFunction;
    private final NodeSubordinateActionsHandler nodeSubordinateActionsHandler;

    @Inject
    NodeEndpointDefinition(NodeRegistry nodeRegistry) {
        this.nodesHandlesFunction = requireNonNull(nodeRegistry)::isNodeService;
        this.nodeSubordinateActionsHandler = new NodeSubordinateActionsHandler(
                new GeneralActionsHandler(null, STABLE), STABLE);
    }

    @Override
    public boolean handles(String serviceName) {
        return nodesHandlesFunction.test(serviceName);
    }

    @Override
    public SmsRouteTreeBuilder overrideTreeBuilder() {
        List<Version> versions;
        versions = List.of(Version.version(1), Version.version(2), Version.version(3));
        return leaf("/authentication/authenticationtrees/nodes", nodesHandlesFunction, versions,
                nodeSubordinateActionsHandler);
    }
}
