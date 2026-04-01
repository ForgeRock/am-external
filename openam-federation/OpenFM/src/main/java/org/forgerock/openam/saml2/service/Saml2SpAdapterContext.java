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

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;

import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpAdapterScriptBindings;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;

@Singleton
public class Saml2SpAdapterContext extends NextGenScriptContext<SamlSpAdapterScriptBindings> {

    /** The name of the SAML2 SP Adapter script context. */
    private static final String SAML2_SP_ADAPTER_NEXTGEN = "SAML2_SP_ADAPTER_NEXTGEN";
    /** The ID for the SAML2 SP Adapter default script. */
    private static final String SAML2_SP_ADAPTER_NAME_DEFAULT_SCRIPT_ID = "91bd046e-0221-41ed-8890-6858c1efd998";

    /** The default script for this context. */
    private final Script defaultScript;

    @Inject
    public Saml2SpAdapterContext() throws ScriptException {
        super(SamlSpAdapterScriptBindings.class);
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
    public SamlSpAdapterScriptBindings getExampleBindings() {
        return SamlSpAdapterScriptBindings.builder()
                .withRequest(null)
                .withResponse(null)
                .withAuthnRequest(new AuthnRequestImpl())
                .withSsoToken(null)
                .withIdpEntityId("http://idp.example.com")
                .withProfile("urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser")
                .withFederation(true)
                .withUserId("exampleUser")
                .withFailureCode(0)
                .withHostedEntityId("http://sp.example.com")
                .withBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")
                .build();
    }

    @Override
    public String name() {
        return SAML2_SP_ADAPTER_NEXTGEN;
    }

    @Override
    public String getI18NKey() {
        return "next-gen-script-type-10";
    }

    private Script createDefaultScript() throws ScriptException {
        return Script.defaultScriptBuilder()
                .setId(SAML2_SP_ADAPTER_NAME_DEFAULT_SCRIPT_ID)
                .setName("SAML2 SP Adapter Script (Next Gen)")
                .setDescription("Default global script for SAML2 SP Adapter (Next Gen)")
                .setContext(this)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setScript(loadScript("scripts/next-gen-saml2-sp-adapter.js"))
                .build();
    }
}
