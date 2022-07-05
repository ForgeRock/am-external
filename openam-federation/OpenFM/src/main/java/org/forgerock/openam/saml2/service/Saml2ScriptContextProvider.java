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
 * Copyright 2021-2022 ForgeRock AS.
 */

package org.forgerock.openam.saml2.service;

import static java.text.MessageFormat.format;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.persistence.config.defaults.ScriptContextDetailsProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * Responsible for providing the SAML2 script contexts.
 */
public class Saml2ScriptContextProvider implements ScriptContextDetailsProvider {

    private static final Logger logger = LoggerFactory.getLogger(Saml2ScriptContextProvider.class);

    @Override
    public List<ScriptContextDetails> get() {
        List<ScriptContextDetails> scriptContexts = new ArrayList<>();

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(SAML2_IDP_ATTRIBUTE_MAPPER)
                .withI18NKey("script-type-12")
                .withDefaultScript(SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT.getId())
                .overrideDefaultWhiteList(
                        "com.iplanet.am.sdk.AMHashMap", "com.sun.identity.saml2.assertion.impl.AttributeImpl",
                        "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Character$Subset",
                        "java.lang.Character$UnicodeBlock", "java.util.Collections$EmptyMap", "java.lang.Double",
                        "java.lang.Float", "com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptHelper",
                        "java.lang.Integer", "java.lang.Long", "java.lang.Math", "java.lang.Number", "java.lang.Object",
                        "java.lang.Short", "java.lang.StrictMath", "java.lang.String", "java.lang.Void",
                        "java.util.AbstractMap$SimpleImmutableEntry", "java.util.ArrayList", "java.util.ArrayList$Itr",
                        "java.util.Collections$1", "java.util.Collections$EmptyList",
                        "java.util.Collections$SingletonList", "java.util.HashMap", "java.util.HashMap$Entry",
                        "java.util.HashMap$KeyIterator", "java.util.HashMap$KeySet", "java.util.HashMap$Node",
                        "java.util.HashSet", "java.util.LinkedHashMap", "java.util.LinkedHashMap$Entry",
                        "java.util.LinkedHashMap$LinkedEntryIterator", "java.util.LinkedHashMap$LinkedEntrySet",
                        "java.util.LinkedHashSet", "java.util.LinkedList", "java.util.TreeMap", "java.util.TreeSet",
                        "java.net.URI", "com.iplanet.sso.providers.dpro.SessionSsoToken",
                        "com.sun.identity.common.CaseInsensitiveHashMap", "com.sun.identity.shared.debug.Debug",
                        "groovy.json.JsonSlurper", "groovy.json.internal.LazyMap",
                        "org.codehaus.groovy.runtime.GStringImpl", "org.codehaus.groovy.runtime.ScriptBytecodeAdapter",
                        "org.forgerock.http.Client", "org.forgerock.http.client.*",
                        "org.forgerock.openam.scripting.api.http.GroovyHttpClient",
                        "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
                        "org.forgerock.util.promise.PromiseImpl", "org.forgerock.json.JsonValue",
                        "com.sun.identity.saml2.common.SAML2Exception",
                        "java.util.Collections$UnmodifiableRandomAccessList",
                        "java.util.Collections$UnmodifiableCollection$1").build());

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(SAML2_IDP_ADAPTER)
                .withI18NKey("script-type-13")
                .withDefaultScript(SAML2_IDP_ADAPTER_SCRIPT.getId())
                .overrideDefaultWhiteList(
                        "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
                        "java.lang.Character$Subset", "java.lang.Character$UnicodeBlock",
                        "java.util.Collections$EmptyMap", "java.lang.Double", "java.lang.Float", "java.lang.Integer",
                        "java.lang.Long", "java.lang.Math", "java.lang.Number", "java.lang.Object", "java.lang.Short",
                        "java.lang.StrictMath", "java.lang.String", "java.lang.Void",
                        "java.util.AbstractMap$SimpleImmutableEntry", "java.util.ArrayList", "java.util.ArrayList$Itr",
                        "java.util.Collections$1", "java.util.Collections$EmptyList",
                        "java.util.Collections$SingletonList", "java.util.HashMap", "java.util.HashMap$Entry",
                        "java.util.HashMap$KeyIterator", "java.util.HashMap$KeySet", "java.util.HashMap$Node",
                        "java.util.HashSet", "java.util.LinkedHashMap", "java.util.LinkedHashMap$Entry",
                        "java.util.LinkedHashMap$LinkedEntryIterator", "java.util.LinkedHashMap$LinkedEntrySet",
                        "java.util.LinkedHashSet", "java.util.LinkedList", "java.util.TreeMap", "java.util.TreeSet",
                        "java.net.URI", "javax.security.auth.Subject", "javax.servlet.http.HttpServletRequestWrapper",
                        "javax.servlet.http.HttpServletResponseWrapper", "com.iplanet.am.sdk.AMHashMap",
                        "com.iplanet.sso.providers.dpro.SessionSsoToken",
                        "com.sun.identity.common.CaseInsensitiveHashMap",
                        "com.sun.identity.saml2.common.SAML2Exception",
                        "com.sun.identity.saml2.plugins.scripted.IdpAdapterScriptHelper",
                        "com.sun.identity.saml2.plugins.scripted.ScriptEntitlementInfo",
                        "com.sun.identity.saml2.protocol.impl.AuthnRequestImpl",
                        "com.sun.identity.saml2.protocol.impl.ResponseImpl", "com.sun.identity.shared.debug.Debug",
                        "groovy.json.JsonSlurper", "groovy.json.internal.LazyMap",
                        "org.codehaus.groovy.runtime.GStringImpl", "org.codehaus.groovy.runtime.ScriptBytecodeAdapter",
                        "org.forgerock.http.Client", "org.forgerock.http.client.*",
                        "org.forgerock.openam.scripting.api.http.GroovyHttpClient",
                        "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
                        "org.forgerock.util.promise.PromiseImpl", "org.forgerock.json.JsonValue",
                        "java.util.Collections$UnmodifiableRandomAccessList",
                        "java.util.Collections$UnmodifiableCollection$1").build());

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
                &&  entityAttributes.get(scriptContext.getAttribute()).contains(script.getId());
    }
}
