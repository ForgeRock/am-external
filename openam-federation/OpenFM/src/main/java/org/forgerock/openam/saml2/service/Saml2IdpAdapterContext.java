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

import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAdapterScriptBindings;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;
import com.sun.identity.saml2.protocol.impl.ResponseImpl;

/**
 * The script context for SAML2 IDP Adapter scripts.
 */
@Singleton
public class Saml2IdpAdapterContext extends NextGenScriptContext<SamlIdpAdapterScriptBindings> {

    /** The name of the SAML2 IDP Adapter script context. */
    private static final String SAML2_IDP_ADAPTER_NEXTGEN = "SAML2_IDP_ADAPTER_NEXTGEN";
    /** The ID for the SAML2 IDP Adapter default script. */
    private static final String SAML2_IDP_ADAPTER_DEFAULT_SCRIPT_ID = "7c399b7e-e596-4447-aa7a-071af55e892c";

    private final Script defaultScript;

    @Inject
    public Saml2IdpAdapterContext() throws ScriptException {
        super(SamlIdpAdapterScriptBindings.class);
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
    public SamlIdpAdapterScriptBindings getExampleBindings() {
        return SamlIdpAdapterScriptBindings.builder()
                .withRequest(null)
                .withResponse(null)
                .withAuthnRequest(new AuthnRequestImpl())
                .withSsoResponse(new ResponseImpl())
                .withSsoToken(null)
                .withHostedEntityId("http://idp.example.com")
                .withReqId("exampleRequestId")
                .withRelayState("exampleRelayState")
                .withFaultCode("exampleFaultCode")
                .withFaultDetail("exampleFaultDetail")
                .build();
    }

    @Override
    public String name() {
        return SAML2_IDP_ADAPTER_NEXTGEN;
    }

    @Override
    public String getI18NKey() {
        return "next-gen-script-type-11";
    }

    private Script createDefaultScript() throws ScriptException {
        return Script.defaultScriptBuilder()
                .setId(SAML2_IDP_ADAPTER_DEFAULT_SCRIPT_ID)
                .setName("SAML2 IDP Adapter Script (Next Gen)")
                .setDescription("Default global script for SAML2 IDP Adapter (Next Gen)")
                .setContext(this)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setScript(loadScript("scripts/next-gen-saml2-idp-adapter.js"))
                .build();
    }
}
