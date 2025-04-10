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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScriptsProvider;

import com.google.auto.service.AutoService;

/**
 * Global script configurations for framework nodes scripts.
 */
@AutoService(GlobalScriptsProvider.class)
public class FrameworkNodesGlobalScriptsProvider implements GlobalScriptsProvider {
    @Override
    public List<Script> get() throws ScriptException {
        List<Script> scripts = new ArrayList<>();
        scripts.add(Script.defaultScriptBuilder()
                            .setId(CONFIG_PROVIDER_NODE_SCRIPT.getId())
                            .setName("Config Provider Node Script")
                            .setDescription("Script to provide values for a config provider node")
                            .setContext(CONFIG_PROVIDER_NODE_SCRIPT.getContext())
                            .setLanguage(ScriptingLanguage.JAVASCRIPT)
                            .setScript(loadScript("scripts/config-provider-node.js"))
                            .build());

        return scripts;
    }
}
