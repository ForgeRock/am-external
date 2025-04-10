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

package org.forgerock.openam.authentication.service;

import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScript;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_CLIENT_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_SERVER_SIDE;

/**
 * Default global script configurations for device identification scripts.
 */
public enum DeviceIdGlobalScript implements GlobalScript {

    DEVICE_ID_MATCH_SERVER_SIDE("Device Id (Match) - Server Side", "703dab1a-1921-4981-98dd-b8e5349d8548",
            AUTHENTICATION_SERVER_SIDE),
    DEVICE_ID_MATCH_CLIENT_SIDE("Device Id (Match) - Client Side", "157298c0-7d31-4059-a95b-eeb08473b7e5",
            AUTHENTICATION_CLIENT_SIDE);


    private final String displayName;
    private final String id;
    private final ScriptContext context;

    DeviceIdGlobalScript(String displayName, String id, ScriptContext context) {
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
