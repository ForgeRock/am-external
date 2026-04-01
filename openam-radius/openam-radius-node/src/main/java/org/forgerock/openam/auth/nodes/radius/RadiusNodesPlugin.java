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
package org.forgerock.openam.auth.nodes.radius;

import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.nodes.NodesPlugin.NODES_PLUGIN_VERSION;

import java.util.List;
import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.NodesPlugin;
import org.forgerock.openam.plugins.AmPlugin;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;

/**
 * Plugin class for Radius authentication nodes.
 */
@AutoService(AmPlugin.class)
public class RadiusNodesPlugin extends AbstractNodeAmPlugin {

    /** The version of the Radius Nodes Plugin. */
    public static final String RADIUS_NODES_PLUGIN_VERSION = "1.0.0";

    @Override
    public String getPluginVersion() {
        return RADIUS_NODES_PLUGIN_VERSION;
    }

    @Override
    public Map<Class<? extends AmPlugin>, String> getDependencies() {
        return singletonMap(NodesPlugin.class, "[1.0.0,%s]".formatted(NODES_PLUGIN_VERSION));
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return new ImmutableMap.Builder<String, Iterable<? extends Class<? extends Node>>>()
                .put("1.0.0", List.of(
                        RadiusDecisionNode.class,
                        RadiusChallengeCollectorNode.class
                ))
                .build();
    }
}
