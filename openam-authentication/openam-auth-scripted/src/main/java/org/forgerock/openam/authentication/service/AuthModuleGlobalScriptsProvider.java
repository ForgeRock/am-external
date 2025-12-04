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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.service;

import static org.forgerock.openam.authentication.service.AuthModuleGlobalScript.AUTH_MODULE_CLIENT_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleGlobalScript.AUTH_MODULE_SERVER_SIDE;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScriptsProvider;
import org.forgerock.openam.scripting.domain.Script;

import com.google.auto.service.AutoService;

/**
 * Responsible for providing Auth module global scripts.
 */
@AutoService(GlobalScriptsProvider.class)
public class AuthModuleGlobalScriptsProvider implements GlobalScriptsProvider {

    @Override
    public List<Script> get() throws ScriptException {
        List<Script> scripts = new ArrayList<>();

        scripts.add(Script.defaultScriptBuilder()
                .setId(AUTH_MODULE_SERVER_SIDE.getId())
                .setName("Scripted Module - Server Side")
                .setDescription("Default global script for server side Scripted Authentication Module")
                .setContext(AUTH_MODULE_SERVER_SIDE.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/authentication-server-side.js"))
                .build());

        scripts.add(Script.defaultScriptBuilder()
                .setId(AUTH_MODULE_CLIENT_SIDE.getId())
                .setName("Scripted Module - Client Side")
                .setDescription("Default global script for client side Scripted Authentication Module")
                .setContext(AUTH_MODULE_CLIENT_SIDE.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/authentication-client-side.js"))
                .build());

        return scripts;
    }
}
