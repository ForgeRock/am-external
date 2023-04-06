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
 * Copyright 2021-2023 ForgeRock AS.
 */

package org.forgerock.openam.saml2.service;

import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_SP_ADAPTER_SCRIPT;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScriptsProvider;
import org.forgerock.openam.scripting.domain.Script;

/**
 * Responsible for providing SAML2 global scripts.
 */
public class Saml2GlobalScriptsProvider implements GlobalScriptsProvider {

    @Override
    public List<Script> get() throws ScriptException {
        List<Script> scripts = new ArrayList<>();

        scripts.add(Script.defaultScriptBuilder()
                .setId(SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT.getId())
                .setName("SAML2 IDP Attribute Mapper Script")
                .setDescription("Default global script for SAML2 IDP Attribute Mapper")
                .setContext(SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/saml2-idp-attribute-mapper.js"))
                .build());

        scripts.add(Script.defaultScriptBuilder()
                .setId(SAML2_IDP_ADAPTER_SCRIPT.getId())
                .setName("SAML2 IDP Adapter Script")
                .setDescription("Default global script for SAML2 IDP Adapter")
                .setContext(SAML2_IDP_ADAPTER_SCRIPT.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/saml2-idp-adapter.js"))
                .build());

        scripts.add(Script.defaultScriptBuilder()
                .setId(SAML2_SP_ADAPTER_SCRIPT.getId())
                .setName("SAML2 SP Adapter Script")
                .setDescription("Default global script for SAML2 SP Adapter")
                .setContext(SAML2_SP_ADAPTER_SCRIPT.getContext())
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setScript(loadScript("scripts/saml2-sp-adapter.js"))
                .build());

        return scripts;
    }
}
