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

import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScript;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_CLIENT_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_SERVER_SIDE;

/**
 * Default global script configurations for authentication module scripts.
 */
public enum AuthModuleGlobalScript implements GlobalScript {

    AUTH_MODULE_SERVER_SIDE("Scripted Module - Server Side", "7e3d7067-d50f-4674-8c76-a3e13a810c33",
            AUTHENTICATION_SERVER_SIDE),
    AUTH_MODULE_CLIENT_SIDE("Scripted Module - Client Side", "c827d2b4-3608-4693-868e-bbcf86bd87c7",
            AUTHENTICATION_CLIENT_SIDE);


    private final String displayName;
    private final String id;
    private final ScriptContext context;

    AuthModuleGlobalScript(String displayName, String id, ScriptContext context) {
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
