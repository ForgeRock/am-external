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

import static com.sun.identity.shared.Constants.AM_SCRIPTING_NEXT_GEN_SAML2;
import static java.text.MessageFormat.format;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_SP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_SP_ADAPTER;

import java.io.PrintWriter;
import java.net.URI;
import java.security.cert.CertificateFactory;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.bind.JAXBElement;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.domain.EvaluatorVersionAllowList;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.persistence.config.defaults.ScriptContextDetailsProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.PromiseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.google.common.collect.ImmutableList;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.scripted.IdpAdapterScriptHelper;
import com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Responsible for providing the SAML2 script contexts.
 */
public class Saml2ScriptContextProvider implements ScriptContextDetailsProvider {

    private static final Logger logger = LoggerFactory.getLogger(Saml2ScriptContextProvider.class);

    private List<String> commonWhiteListV1() {
        return List.of(
                "java.lang.Boolean",
                "java.lang.Byte",
                "java.lang.Character",
                "java.lang.Character$Subset",
                "java.lang.Character$UnicodeBlock",
                "java.lang.Double",
                "java.lang.Float",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Math",
                "java.lang.Number",
                "java.lang.Object",
                "java.lang.Short",
                "java.lang.StrictMath",
                "java.lang.String",
                "java.lang.Void",
                "java.util.AbstractMap$SimpleImmutableEntry",
                "java.util.ArrayList",
                "java.util.ArrayList$Itr",
                "java.util.Collections$1",
                "java.util.Collections$EmptyList",
                "java.util.Collections$EmptyMap",
                "java.util.Collections$SingletonList",
                "java.util.Collections$UnmodifiableRandomAccessList",
                "java.util.Collections$UnmodifiableCollection$1",
                "java.util.HashMap",
                "java.util.HashMap$Entry",
                "java.util.HashMap$KeyIterator",
                "java.util.HashMap$KeySet",
                "java.util.HashMap$Node",
                "java.util.HashSet",
                "java.util.LinkedHashMap",
                "java.util.LinkedHashMap$Entry",
                "java.util.LinkedHashMap$LinkedEntryIterator",
                "java.util.LinkedHashMap$LinkedEntrySet",
                "java.util.LinkedHashSet",
                "java.util.LinkedList",
                "java.util.TreeMap",
                "java.util.TreeSet",
                "java.net.URI",
                "com.iplanet.am.sdk.AMHashMap",
                "com.iplanet.sso.providers.dpro.SessionSsoToken",
                "com.sun.identity.common.CaseInsensitiveHashMap",
                "com.sun.identity.shared.debug.Debug",
                "com.sun.identity.saml2.common.SAML2Exception",
                "groovy.json.JsonSlurper",
                "groovy.json.internal.LazyMap",
                "org.codehaus.groovy.runtime.GStringImpl",
                "org.codehaus.groovy.runtime.ScriptBytecodeAdapter",
                "org.forgerock.http.Client",
                "org.forgerock.http.client.*",
                "org.forgerock.openam.scripting.api.http.GroovyHttpClient",
                "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
                "org.forgerock.util.promise.PromiseImpl",
                "org.forgerock.json.JsonValue",
                "org.mozilla.javascript.JavaScriptException"
        );
    }

    private List<String> whiteListV1() {
        List<String> whitelistV1 = List.of(
                "com.sun.identity.saml2.assertion.*",
                "com.sun.identity.saml2.assertion.impl.*",
                "com.sun.identity.saml2.plugins.scripted.ScriptEntitlementInfo",
                "com.sun.identity.saml2.protocol.*",
                "com.sun.identity.saml2.protocol.impl.*",
                "java.io.PrintWriter",
                "javax.security.auth.Subject",
                "javax.servlet.http.HttpServletRequestWrapper",
                "javax.servlet.http.HttpServletResponseWrapper",
                "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver",
                "sun.security.ec.ECPrivateKeyImpl",
                "org.forgerock.opendj.ldap.Rdn",
                "org.forgerock.opendj.ldap.Dn"
        );
        return Stream.concat(commonWhiteListV1().stream(), whitelistV1.stream())
                .collect(Collectors.toUnmodifiableList());
    }

    private List<String> whiteListV1IdPAttributeMapper() {
        List<String> whitelistV1IdPAttributeMapper = List.of(
                "com.sun.identity.saml2.assertion.impl.AttributeImpl",
                "com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptHelper",
                "javax.servlet.http.Cookie",
                "javax.xml.parsers.DocumentBuilder",
                "javax.xml.parsers.DocumentBuilderFactory",
                "org.forgerock.openam.shared.security.crypto.CertificateService",
                "org.w3c.dom.Document",
                "org.w3c.dom.Element",
                "org.xml.sax.InputSource"
        );

        return Stream.concat(commonWhiteListV1().stream(), whitelistV1IdPAttributeMapper.stream())
                .collect(Collectors.toUnmodifiableList());
    }

    private List<String> commonWhiteListV2() {
        return List.of(
                Boolean.class.getName(), Byte.class.getName(),
                Character.class.getName(),
                Character.Subset.class.getName(),
                Character.UnicodeBlock.class.getName(),
                Double.class.getName(),
                Float.class.getName(),
                Integer.class.getName(),
                Long.class.getName(),
                Math.class.getName(),
                Number.class.getName(),
                Object.class.getName(),
                Short.class.getName(),
                StrictMath.class.getName(),
                String.class.getName(),
                Void.class.getName(),
                AbstractMap.SimpleImmutableEntry.class.getName(),
                ArrayList.class.getName(),
                privateInnerClass(ArrayList.class, "Itr"),
                privateInnerClass(Collections.class, "Collections$1"),
                privateInnerClass(Collections.class, "EmptyList"),
                privateInnerClass(Collections.class, "EmptyMap"),
                privateInnerClass(Collections.class, "SingletonList"),
                privateInnerClass(Collections.class, "UnmodifiableRandomAccessList"),
                privateInnerClass(Collections.class, "UnmodifiableCollection$1"),
                HashMap.class.getName(),
                privateInnerClass(HashMap.class, "Entry"),
                privateInnerClass(HashMap.class, "KeyIterator"),
                privateInnerClass(HashMap.class, "KeySet"),
                privateInnerClass(HashMap.class, "Node"),
                HashSet.class.getName(),
                LinkedHashMap.class.getName(),
                privateInnerClass(LinkedHashMap.class, "Entry"),
                privateInnerClass(LinkedHashMap.class, "LinkedEntryIterator"),
                privateInnerClass(LinkedHashMap.class, "LinkedEntrySet"),
                LinkedHashSet.class.getName(),
                LinkedList.class.getName(),
                TreeMap.class.getName(),
                TreeSet.class.getName(),
                URI.class.getName(),
                CaseInsensitiveHashMap.class.getName(),
                JsonValue.class.getName(),
                "org.mozilla.javascript.JavaScriptException",
                PromiseImpl.class.getName(),
                Cookie.class.getName(),
                InputSource.class.getName(),
                CertificateFactory.class.getName(),
                "com.iplanet.am.sdk.AMHashMap",
                "com.iplanet.sso.providers.dpro.SessionSsoToken",
                "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
                "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver"
        );
    }

    private List<String> whiteListV2() {
        List<String> whitelistV2 = List.of(
                PrintWriter.class.getName(),
                Subject.class.getName(),
                HttpServletRequestWrapper.class.getName(),
                HttpServletResponseWrapper.class.getName(),
                "sun.security.ec.ECPrivateKeyImpl"
        );
        return Stream.concat(commonWhiteListV2().stream(), whitelistV2.stream())
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<ScriptContextDetails> get() {
        List<ScriptContextDetails> scriptContexts = new ArrayList<>();
        boolean nextGenEnabled = SystemPropertiesManager.getAsBoolean(AM_SCRIPTING_NEXT_GEN_SAML2);

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(SAML2_IDP_ATTRIBUTE_MAPPER)
                .withI18NKey("script-type-12")
                .withDefaultScript(SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT.getId())
                .withWhiteList(EvaluatorVersionAllowList.builder()
                        .v1AllowList(whiteListV1IdPAttributeMapper())
                        .v2AllowList(commonWhiteListV2())
                        .build())
                .withNextGenEnabled(nextGenEnabled)
                .build());

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(SAML2_IDP_ADAPTER)
                .withI18NKey("script-type-13")
                .withDefaultScript(SAML2_IDP_ADAPTER_SCRIPT.getId())
                .withWhiteList(EvaluatorVersionAllowList.builder()
                        .v1AllowList(ImmutableList.<String>builder()
                                .addAll(whiteListV1())
                                .add(IdpAdapterScriptHelper.class.getName())
                                .build())
                        .v2AllowList(ImmutableList.<String>builder()
                                .addAll(whiteListV2())
                                .build())
                        .build())
                .withNextGenEnabled(nextGenEnabled)
                .build());

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(SAML2_SP_ADAPTER)
                .withI18NKey("script-type-17")
                .withDefaultScript(SAML2_SP_ADAPTER_SCRIPT.getId())
                .withWhiteList(EvaluatorVersionAllowList.builder()
                        .v1AllowList(ImmutableList.<String>builder()
                                .addAll(whiteListV1())
                                .add(SpAdapterScriptHelper.class.getName())
                                .build())
                        .v2AllowList(ImmutableList.<String>builder()
                                .addAll(whiteListV2())
                                .build())
                        .build())
                .withNextGenEnabled(nextGenEnabled)
                .build());

        return scriptContexts;
    }

    @Override
    public long getUsageCount(Realm realm, Script script) {
        SAML2MetaManager saml2MetaManager = SAML2Utils.getSAML2MetaManager();
        try {
            List<EntityConfigElement> entityConfigElements = saml2MetaManager.getAllHostedEntityConfigs(realm.asPath());
            return entityConfigElements.stream()
                    .map(entity -> entity.getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig())
                    .flatMap(List::stream)
                    .filter(entity -> entityContainsScript(entity, script))
                    .count();
        } catch (SAML2MetaException e) {
            logger.error("Unable to check if the script {} is used.", script.getName(), e);
            throw new SamlPluginScriptUsageCountException(
                    format("Unable to check if the script {0} is used.", script.getName()), e);
        }
    }

    private boolean entityContainsScript(JAXBElement<BaseConfigType> entity, Script script) {
        Map<String, List<String>> entityAttributes = SAML2MetaUtils.getAttributes(entity);
        return Arrays.stream(Saml2ScriptContext.values())
                .anyMatch(scriptContext -> entityAttributeContainsScriptId(script, entityAttributes, scriptContext));
    }

    private boolean entityAttributeContainsScriptId(Script script, Map<String,
            List<String>> entityAttributes, Saml2ScriptContext scriptContext) {
        return CollectionUtils.isNotEmpty(entityAttributes.get(scriptContext.getAttribute()))
                && entityAttributes.get(scriptContext.getAttribute()).contains(script.getId());
    }

    private String privateInnerClass(Class clazz, String innerClassName) {
        return clazz.getName() + "$" + innerClassName;
    }
}
