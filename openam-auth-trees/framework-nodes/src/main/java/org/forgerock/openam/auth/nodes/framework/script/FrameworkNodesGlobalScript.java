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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesScriptContext.CONFIG_PROVIDER_NODE;

import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScript;

/**
 * Global script configurations for framework nodes scripts.
 */
public enum FrameworkNodesGlobalScript implements GlobalScript {

    /**
     * The default Config Provider Node Script.
     */
    CONFIG_PROVIDER_NODE_SCRIPT("Config Provider", "5e854779-6ec1-4c39-aeba-0477e0986646", CONFIG_PROVIDER_NODE);

    private final String displayName;
    private final String id;
    private final ScriptContext context;

    FrameworkNodesGlobalScript(String displayName, String id, ScriptContext context) {
        this.displayName = displayName;
        this.id = id;
        this.context = context;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }
}
