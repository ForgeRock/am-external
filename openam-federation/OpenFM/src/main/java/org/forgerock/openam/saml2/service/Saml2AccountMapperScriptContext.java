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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.service;

import java.util.List;

import jakarta.inject.Singleton;

import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;

import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpAccountMapperBindings;

/**
 * The script context for SAML2 SP Account Mapper.
 */
@Singleton
public class Saml2AccountMapperScriptContext extends NextGenScriptContext<SamlSpAccountMapperBindings> {

    /**
     * The name of the SAML2 SP Account Mapper script context.
     */
    public static final String SAML_2_SP_ACCOUNT_MAPPER_NAME = "SAML2_SP_ACCOUNT_MAPPER";

    /**
     * The ID for the SAML2 SP Account Mapper default script.
     */
    public static final String SAML2_ACCOUNT_MAPPER_DEFAULT_SCRIPT_ID = "05ab4a85-11ec-4a32-94cf-05bbb763c8f3";

    private final Script defaultScript;

    /**
     * Create a new Next Gen script context for SAML2 SP Account Mapper.
     */
    public Saml2AccountMapperScriptContext() throws ScriptException {
        super(SamlSpAccountMapperBindings.class);
        this.defaultScript = createDefaultScript();
    }

    @Override
    protected Script getDefaultScript() {
        return defaultScript;
    }

    @Override
    protected List<String> getContextWhiteList() {
        return List.of();
    }

    @Override
    public SamlSpAccountMapperBindings getExampleBindings() {
        return SamlSpAccountMapperBindings.builder()
                .withAssertion(null)
                .withHostedEntityId("")
                .withSpAccountMapperScriptHelper(null)
                .build();
    }

    @Override
    public String name() {
        return SAML_2_SP_ACCOUNT_MAPPER_NAME;
    }

    @Override
    public String getI18NKey() {
        return "script-type-20";
    }

    private Script createDefaultScript() throws ScriptException {
        return  Script.defaultScriptBuilder()
                .setId(SAML2_ACCOUNT_MAPPER_DEFAULT_SCRIPT_ID)
                .setName("SAML2 SP Account Mapper Script")
                .setDescription("Default global script for SAML2 SP Account Mapper")
                .setContext(this)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setScript(loadScript("scripts/saml2-sp-account-mapper.js"))
                .build();
    }
}
