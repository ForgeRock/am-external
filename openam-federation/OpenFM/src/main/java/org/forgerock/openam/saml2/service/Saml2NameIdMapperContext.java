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

package org.forgerock.openam.saml2.service;

import java.io.PrintWriter;
import java.net.URI;
import java.security.cert.CertificateFactory;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScriptsProvider;
import org.xml.sax.InputSource;

import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlNameIdMapperBindings;

/**
 * The script context for SAML2 NameID Mapper.
 */
@Singleton
@AutoService({ScriptContext.class, GlobalScriptsProvider.class})
public class Saml2NameIdMapperContext extends NextGenScriptContext<SamlNameIdMapperBindings> {

    /**
     * The name of the SAML2 NameID Mapper script context.
     */
    public static final String SAML_2_NAMEID_MAPPER_NAME = "SAML2_NAMEID_MAPPER";
    /**
     * The ID for the SAML2 NameID Mapper default script.
     */
    public static final String SAML2_NAMEID_MAPPER_DEFAULT_SCRIPT_ID = "4a171d3a-056b-4ab7-a19f-d7e93ddf7ae5";

    private final Script defaultScript;

    /**
     * Create a new Next Gen script context for SAML2 NameID Mapper.
     */
    public Saml2NameIdMapperContext() throws ScriptException {
        super(SamlNameIdMapperBindings.class);
        this.defaultScript = createDefaultScript();
    }

    @Override
    protected Script getDefaultScript() {
        return defaultScript;
    }

    @Override
    protected List<String> getContextWhiteList() {
        return List.of(
                Byte.class.getName(),
                Character.class.getName(),
                Character.Subset.class.getName(),
                Character.UnicodeBlock.class.getName(),
                Float.class.getName(),
                Long.class.getName(),
                Math.class.getName(),
                Number.class.getName(),
                Short.class.getName(),
                StrictMath.class.getName(),
                Void.class.getName(),
                AbstractMap.SimpleImmutableEntry.class.getName(),
                ArrayList.class.getName(),
                innerClass(ArrayList.class, "Itr"),
                innerClass(Collections.class, "Collections$1"),
                innerClass(Collections.class, "EmptyList"),
                innerClass(Collections.class, "EmptyMap"),
                innerClass(Collections.class, "SingletonList"),
                innerClass(Collections.class, "UnmodifiableRandomAccessList"),
                innerClass(Collections.class, "UnmodifiableCollection$1"),
                HashMap.class.getName(),
                innerClass(HashMap.class, "Entry"),
                innerClass(HashMap.class, "KeyIterator"),
                innerClass(HashMap.class, "KeySet"),
                innerClass(HashMap.class, "Node"),
                HashSet.class.getName(),
                LinkedHashMap.class.getName(),
                innerClass(LinkedHashMap.class, "Entry"),
                innerClass(LinkedHashMap.class, "LinkedEntryIterator"),
                innerClass(LinkedHashMap.class, "LinkedEntrySet"),
                LinkedHashSet.class.getName(),
                LinkedList.class.getName(),
                TreeMap.class.getName(),
                TreeSet.class.getName(),
                URI.class.getName(),
                CaseInsensitiveHashMap.class.getName(),
                JsonValue.class.getName(),
                "org.mozilla.javascript.JavaScriptException",
                javax.servlet.http.Cookie.class.getName(),
                InputSource.class.getName(),
                CertificateFactory.class.getName(),
                "com.iplanet.am.sdk.AMHashMap",
                "com.iplanet.sso.providers.dpro.SessionSsoToken",
                "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
                "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver",
                PrintWriter.class.getName(),
                Subject.class.getName(),
                HttpServletRequestWrapper.class.getName(),
                HttpServletResponseWrapper.class.getName(),
                "sun.security.ec.ECPrivateKeyImpl"
        );
    }

    @Override
    public SamlNameIdMapperBindings getExampleBindings() {
        return SamlNameIdMapperBindings.builder()
                .withRemoteEntityId("")
                .withNameIDFormat("")
                .withSession(null)
                .withNameIdScriptHelper(null)
                .withHostedEntityId("")
                .build();
    }

    @Override
    public String name() {
        return SAML_2_NAMEID_MAPPER_NAME;
    }

    @Override
    public String getI18NKey() {
        return "script-type-19";
    }

    private Script createDefaultScript() throws ScriptException {
        return  Script.defaultScriptBuilder()
                .setId(SAML2_NAMEID_MAPPER_DEFAULT_SCRIPT_ID)
                .setName("SAML2 NameID Mapper Script")
                .setDescription("Default global script for SAML2 NameID Mapper")
                .setContext(this)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setScript(loadScript("scripts/saml2-nameid-mapper.js"))
                .build();
    }
}
