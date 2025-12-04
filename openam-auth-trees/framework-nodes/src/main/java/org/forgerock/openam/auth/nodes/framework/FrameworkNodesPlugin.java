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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.nodes.NodesPlugin.NODES_PLUGIN_VERSION;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.NodesPlugin;
import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;
import org.forgerock.openam.plugins.AmPlugin;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.VersionComparison;

import com.google.common.collect.ImmutableMap;

/** Plugin for nodes that are tightly coupled to the tree engine. */
public class FrameworkNodesPlugin extends AbstractNodeAmPlugin {

    /**
     * The current version of the framework nodes plugin.
     * N.B. If upgrading this version you must ensure that the amPluginService version in the latest.groovy
     * FBC upgrade rules file is kept in sync.
     */
    public static final String FRAMEWORK_NODES_VERSION = "4.1.0";

    @Inject
    FrameworkNodesPlugin() {

    }

    @Override
    public String getPluginVersion() {
        return FRAMEWORK_NODES_VERSION;
    }

    @Override
    public Map<Class<? extends AmPlugin>, String> getDependencies() {
        return singletonMap(NodesPlugin.class, "[1.0.0,%s]".formatted(NODES_PLUGIN_VERSION));
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return ImmutableMap.of(
                "1.0.0", List.of(
                        InnerTreeEvaluatorNode.class,
                        ModifyAuthLevelNode.class),
                "2.0.0", List.of(
                        PageNode.class,
                        SetPersistentCookieNode.class),
                "3.0.0", List.of(
                        ConfigProviderNode.class
                ));
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        super.upgrade(fromVersion);
        if (VersionComparison.compareVersionStrings(fromVersion, "2.0.1") > 0) {
            pluginTools.upgradeAuthNode(InnerTreeEvaluatorNode.class);
            pluginTools.upgradeAuthNode(PageNode.class);
        }
        if (inRange("2.0.0", fromVersion, FRAMEWORK_NODES_VERSION)) {
            pluginTools.upgradeAuthNode(SetPersistentCookieNode.class);
        }
        if (inRange("3.0.0", fromVersion, FRAMEWORK_NODES_VERSION)) {
            pluginTools.upgradeAuthNode(ConfigProviderNode.class);
        }
    }

}
