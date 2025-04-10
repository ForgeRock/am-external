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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DEVICE_PROFILE_MATCH_DECISION_NODE_SCRIPT;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScriptsProvider;

import com.google.auto.service.AutoService;

/**
 * Responsible for providing Authentication Nodes global scripts.
 */
@AutoService(GlobalScriptsProvider.class)
public class AuthNodesGlobalScriptsProvider implements GlobalScriptsProvider {

    @Override
    public List<Script> get() throws ScriptException {
        List<Script> scripts = new ArrayList<>();

        scripts.add(Script.defaultScriptBuilder()
                .setId(DECISION_NODE_SCRIPT.getId())
                .setName("Authentication Tree Decision Node Script")
                .setDescription("Default global script for a scripted decision node")
                .setContext(DECISION_NODE_SCRIPT.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/authentication-tree-decision-node.js"))
                .build());

        scripts.add(Script.defaultScriptBuilder()
                .setId(DEVICE_PROFILE_MATCH_DECISION_NODE_SCRIPT.getId())
                .setName("Device Profile Match Template - Decision Node Script")
                .setDescription("Default global script template for Device Profile Match decision node script "
                        + "for Authentication Tree")
                .setContext(DEVICE_PROFILE_MATCH_DECISION_NODE_SCRIPT.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/deviceProfileMatch-decision-node.js"))
                .build());

        return scripts;
    }
}
