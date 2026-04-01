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
 * Copyright 2026 Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.service;

import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAttributeMapperNextGenBindings;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;

/**
 * Defines the context for the Next Gen SAML2 IDP Attribute Mapper script type.
 */
@Singleton
public class Saml2IdpAttributeMapperContext extends NextGenScriptContext<SamlIdpAttributeMapperNextGenBindings> {

    /** The name of the SAML2 IDP Attribute Mapper script context. */
    private static final String SAML2_IDP_ATTRIBUTE_MAPPER_NEXT_GEN = "SAML2_IDP_ATTRIBUTE_MAPPER_NEXT_GEN";
    /** The ID for the SAML2 IDP Attribute Mapper default script. */
    private static final String SAML2_IDP_ATTRIBUTE_MAPPER_DEFAULT_SCRIPT_ID = "0c1d7319-5a82-4359-8839-094121404832";

    /** The default script for this context. */
    private final Script defaultScript;

    @Inject
    public Saml2IdpAttributeMapperContext() throws ScriptException {
        super(SamlIdpAttributeMapperNextGenBindings.class);
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
    public SamlIdpAttributeMapperNextGenBindings getExampleBindings() {
        return SamlIdpAttributeMapperNextGenBindings.builder()
                .withSsoToken(null)
                .withRealm("/exampleRealm")
                .withRemoteEntityId("http://sp.example.com")
                .withHostedEntityId("http://idp.example.com")
                .build();
    }

    @Override
    public String name() {
        return SAML2_IDP_ATTRIBUTE_MAPPER_NEXT_GEN;
    }

    @Override
    public String getI18NKey() {
        return "next-gen-script-type-12";
    }

    private Script createDefaultScript() throws ScriptException {
        return Script.defaultScriptBuilder()
                .setId(SAML2_IDP_ATTRIBUTE_MAPPER_DEFAULT_SCRIPT_ID)
                .setName("SAML IDP Attribute Mapper Script (Next Gen)")
                .setDescription("Default global script for SAML2 IDP Attribute Mapper (Next Gen)")
                .setContext(this)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setScript(loadScript("scripts/saml2-idp-attribute-mapper-next-gen.js"))
                .build();
    }
}
