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
 * Copyright 2017-2018 ForgeRock AS.
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
