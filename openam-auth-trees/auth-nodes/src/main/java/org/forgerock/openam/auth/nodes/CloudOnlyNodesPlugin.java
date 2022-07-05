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
 * Copyright 2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import java.util.List;
import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.StartupType;

import com.google.common.collect.ImmutableMap;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class CloudOnlyNodesPlugin extends AbstractNodeAmPlugin {

    private boolean isEnabled() {
        return SystemProperties.getAsBoolean(Constants.AM_CLOUD_ONLY_ENABLED, Boolean.FALSE);
    }

    @Override
    public String getPluginVersion() {
        if (isEnabled()) {
            return "1.0.0";
        } else {
            return "CLOUD_ONLY";
        }
    }

    @Override
    public void onInstall() throws PluginException {
        if (isEnabled()) {
            super.onInstall();
        }
    }

    @Override
    public void onStartup(StartupType startupType) throws PluginException {
        if (isEnabled()) {
            super.onStartup(startupType);
        }
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (isEnabled()) {
            super.upgrade(fromVersion);
        }
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return new ImmutableMap.Builder<String, Iterable<? extends Class<? extends Node>>>()
                .put("1.0.0", List.of(
                        IdentityStoreDecisionNode.class)
                )
                .build();
    }
}
