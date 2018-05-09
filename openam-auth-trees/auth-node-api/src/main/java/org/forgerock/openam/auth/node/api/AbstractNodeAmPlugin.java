/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import static java.util.Collections.singletonMap;

import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.plugins.AmPlugin;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.PluginTools;
import org.forgerock.openam.plugins.VersionComparison;

/** A convenient base class for {@link AmPlugin}s that provide authentication nodes.
 *
 * @supported.all.api
 **/
public abstract class AbstractNodeAmPlugin implements AmPlugin {

    /** An instance of the {@link PluginTools} for use when setting up the plugin. */
    protected PluginTools pluginTools;

    /**
     * Guice setter for {@code pluginTools}.
     * @param pluginTools The tools.
     */
    @Inject
    public void setPluginTools(PluginTools pluginTools) {
        this.pluginTools = pluginTools;
    }

    @Override
    public void onInstall() throws PluginException {
        for (String version : getNodesByVersion().keySet()) {
            for (Class<? extends Node> nodeClass : getNodesByVersion().get(version)) {
                pluginTools.installAuthNode(nodeClass);
            }
        }
    }

    @Override
    public void onStartup() throws PluginException {
        for (String version : getNodesByVersion().keySet()) {
            for (Class<? extends Node> nodeClass : getNodesByVersion().get(version)) {
                pluginTools.startAuthNode(nodeClass);
            }
        }
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        Map<String, Iterable<? extends Class<? extends Node>>> nodes = getNodesByVersion();
        for (String nodeVersion : nodes.keySet()) {
            if (isNewerVersion(nodeVersion, fromVersion)) {
                for (Class<? extends Node> nodeClass : nodes.get(nodeVersion)) {
                    pluginTools.installAuthNode(nodeClass);
                }
            } else {
                for (Class<? extends Node> nodeClass : nodes.get(nodeVersion)) {
                    pluginTools.addNodeToRegistry(nodeClass);
                }
            }
        }
    }

    private boolean isNewerVersion(String pluginVersion, String fromVersion) {
        return VersionComparison.compareVersionStrings(fromVersion, pluginVersion) > 0;
    }

    /**
     * Specify the Map of list of node classes that the plugin is providing. These will then be installed and registered
     * at the appropriate times in plugin lifecycle.
     *
     * @return The list of node classes.
     */
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return singletonMap(getPluginVersion(), getNodes());
    }

    /**
     * Specify the list of node classes that the plugin is providing. These will then be installed and registered at
     * the appropriate times in plugin lifecycle.
     *
     * @deprecated in favour of {@link #getNodesByVersion()}
     *
     * @return The list of node classes.
     * @throws UnsupportedOperationException if called as this method is now deprecated.
     */
    @Deprecated
    protected Iterable<? extends Class<? extends Node>> getNodes() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
